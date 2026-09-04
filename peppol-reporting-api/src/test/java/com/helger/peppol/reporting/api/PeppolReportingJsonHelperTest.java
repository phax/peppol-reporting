/*
 * Copyright (C) 2023-2026 Philip Helger
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.peppol.reporting.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.OffsetDateTime;

import org.jspecify.annotations.NonNull;
import org.junit.Test;

import com.helger.datetime.helper.PDTFactory;
import com.helger.json.IJsonObject;
import com.helger.peppolid.peppol.doctype.EPredefinedDocumentTypeIdentifier;
import com.helger.peppolid.peppol.process.EPredefinedProcessIdentifier;

/**
 * Test class for class {@link PeppolReportingJsonHelper}
 *
 * @author Philip Helger
 */
public final class PeppolReportingJsonHelperTest
{
  private static final String MY_SPID = "PAT000001";
  private static final String OTHER_SPID = "POP000002";

  @NonNull
  private static PeppolReportingItem _sendingItem ()
  {
    return PeppolReportingItem.builder ()
                              .exchangeDateTime (PDTFactory.getCurrentOffsetDateTimeMillisOnly ())
                              .directionSending ()
                              .c2ID (MY_SPID)
                              .c3ID (OTHER_SPID)
                              .docTypeID (EPredefinedDocumentTypeIdentifier.INVOICE_EN16931_PEPPOL_V30)
                              .processID (EPredefinedProcessIdentifier.BIS3_BILLING)
                              .transportProtocolPeppolAS4v2 ()
                              .c1CountryCode ("FI")
                              .endUserID ("abc")
                              .build ();
  }

  @NonNull
  private static PeppolReportingItem _receivingItem ()
  {
    return PeppolReportingItem.builder ()
                              .exchangeDateTime (PDTFactory.getCurrentOffsetDateTimeMillisOnly ())
                              .directionReceiving ()
                              .c2ID (OTHER_SPID)
                              .c3ID (MY_SPID)
                              .docTypeID (EPredefinedDocumentTypeIdentifier.INVOICE_EN16931_PEPPOL_V30)
                              .processID (EPredefinedProcessIdentifier.BIS3_BILLING)
                              .transportProtocolPeppolAS4v2 ()
                              .c1CountryCode ("FI")
                              .c4CountryCode ("DE")
                              .endUserID ("abc")
                              .build ();
  }

  @Test
  public void testSendingItem ()
  {
    final PeppolReportingItem aItem = _sendingItem ();
    final IJsonObject aJson = PeppolReportingJsonHelper.toJson (aItem);
    assertNotNull (aJson);

    assertEquals (PeppolReportingJsonHelper.JSON_VERSION_1,
                  aJson.getAsInt (PeppolReportingJsonHelper.JSON_VERSION, -1));
    assertEquals (EReportingDirection.SENDING.getID (),
                  aJson.getAsString (PeppolReportingJsonHelper.JSON_DIRECTION));
    assertEquals (MY_SPID, aJson.getAsString (PeppolReportingJsonHelper.JSON_C2ID));
    assertEquals ("FI", aJson.getAsString (PeppolReportingJsonHelper.JSON_C1CC));
    // Sending items never have a C4 country code
    assertFalse (aJson.containsKey (PeppolReportingJsonHelper.JSON_C4CC));

    // Object round trip
    assertEquals (aItem, PeppolReportingJsonHelper.toDomain (aJson));

    // String round trip
    final String sJson = PeppolReportingJsonHelper.toJsonString (aItem);
    assertTrue (sJson.contains ("\"version\":1"));
    assertEquals (aItem, PeppolReportingJsonHelper.toDomain (sJson));
  }

  @Test
  public void testReceivingItem ()
  {
    final PeppolReportingItem aItem = _receivingItem ();
    final IJsonObject aJson = PeppolReportingJsonHelper.toJson (aItem);
    assertNotNull (aJson);

    assertEquals (EReportingDirection.RECEIVING.getID (),
                  aJson.getAsString (PeppolReportingJsonHelper.JSON_DIRECTION));
    assertEquals ("DE", aJson.getAsString (PeppolReportingJsonHelper.JSON_C4CC));

    assertEquals (aItem, PeppolReportingJsonHelper.toDomain (aJson));
    assertEquals (aItem, PeppolReportingJsonHelper.toDomain (PeppolReportingJsonHelper.toJsonString (aItem)));
  }

  @Test
  public void testExchangeDateTimeIsKept ()
  {
    final PeppolReportingItem aItem = _sendingItem ();
    final OffsetDateTime aRead = PeppolReportingJsonHelper.toDomain (PeppolReportingJsonHelper.toJson (aItem))
                                                          .getExchangeDTUTC ();
    assertEquals (aItem.getExchangeDTUTC (), aRead);
  }

  @Test
  public void testToDomainInvalid ()
  {
    assertNull (PeppolReportingJsonHelper.toDomain ((String) null));
    assertNull (PeppolReportingJsonHelper.toDomain ("this is no JSON"));
    // Valid JSON, but no object
    assertNull (PeppolReportingJsonHelper.toDomain ("[1,2,3]"));

    // Unsupported layout version
    final IJsonObject aJson = PeppolReportingJsonHelper.toJson (_sendingItem ());
    aJson.add (PeppolReportingJsonHelper.JSON_VERSION, 2);
    try
    {
      PeppolReportingJsonHelper.toDomain (aJson);
      fail ();
    }
    catch (final IllegalArgumentException ex)
    {
      // expected
    }
  }
}
