/*
 * Copyright (C) 2022-2023 Philip Helger
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
package com.helper.peppol.reporting.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.Test;

import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.mock.CommonsTestHelper;
import com.helger.commons.string.StringHelper;
import com.helger.peppolid.peppol.doctype.EPredefinedDocumentTypeIdentifier;
import com.helger.peppolid.peppol.process.EPredefinedProcessIdentifier;
import com.helper.peppol.reporting.api.PeppolReportingItem.Builder;

/**
 * Test class for class {@link PeppolReportingItem}
 *
 * @author Philip Helger
 */
public final class PeppolReportingItemTest
{
  @Test
  public void testBasicSending ()
  {
    final OffsetDateTime aNow = PDTFactory.getCurrentOffsetDateTime ();
    final String sMySPID = "PAT000001";
    final String sOtherSPID = "POP000002";

    final Builder aBuilder = PeppolReportingItem.builder ()
                                                .exchangeDateTime (aNow)
                                                .directionSending ()
                                                .c2ID (sMySPID)
                                                .c3ID (sOtherSPID)
                                                .docTypeID (EPredefinedDocumentTypeIdentifier.INVOICE_EN16931_PEPPOL_V30)
                                                .processID (EPredefinedProcessIdentifier.BIS3_BILLING)
                                                .transportProtocolPeppolAS4v2 ()
                                                .c1CountryCode ("FI")
                                                .endUserID ("abc");
    final PeppolReportingItem aItem = aBuilder.build ();
    assertNotNull (aItem);
    assertEquals (aNow.atZoneSameInstant (ZoneOffset.UTC).toOffsetDateTime (), aItem.getExchangeDTUTC ());
    assertEquals (EReportingDirection.SENDING, aItem.getDirection ());
    assertTrue (aItem.isSending ());
    assertFalse (aItem.isReceiving ());
    assertEquals (sMySPID, aItem.getC2ID ());
    assertEquals (sOtherSPID, aItem.getC3ID ());
    assertEquals (sOtherSPID, aItem.getOtherServiceProviderID ());
    assertEquals (EPredefinedDocumentTypeIdentifier.INVOICE_EN16931_PEPPOL_V30.getScheme (),
                  aItem.getDocTypeIDScheme ());
    assertEquals (EPredefinedDocumentTypeIdentifier.INVOICE_EN16931_PEPPOL_V30.getValue (), aItem.getDocTypeIDValue ());
    assertEquals (EPredefinedProcessIdentifier.BIS3_BILLING.getScheme (), aItem.getProcessIDScheme ());
    assertEquals (EPredefinedProcessIdentifier.BIS3_BILLING.getValue (), aItem.getProcessIDValue ());
    assertEquals ("peppol-transport-as4-v2_0", aItem.getTransportProtocol ());
    assertEquals ("FI", aItem.getC1CountryCode ());
    assertNull (aItem.getC4CountryCode ());
    assertEquals ("FI", aItem.getEndUserCountryCode ());
    assertEquals ("abc", aItem.getEndUserID ());

    CommonsTestHelper.testDefaultImplementationWithEqualContentObject (aItem, aBuilder.build ());
    CommonsTestHelper.testDefaultImplementationWithDifferentContentObject (aItem,
                                                                           aBuilder.endUserID ("foobar").build ());
  }

  @Test
  public void testBasicReceiving ()
  {
    final OffsetDateTime aNow = PDTFactory.getCurrentOffsetDateTime ();
    final String sMySPID = "PAT000001";
    final String sOtherSPID = "POP000002";

    final PeppolReportingItem aItem = PeppolReportingItem.builder ()
                                                         .exchangeDateTime (aNow)
                                                         .directionReceiving ()
                                                         .c2ID (sOtherSPID)
                                                         .c3ID (sMySPID)
                                                         .docTypeID (EPredefinedDocumentTypeIdentifier.INVOICE_EN16931_PEPPOL_V30)
                                                         .processID (EPredefinedProcessIdentifier.BIS3_BILLING)
                                                         .transportProtocolPeppolAS4v2 ()
                                                         .c1CountryCode ("FI")
                                                         .c4CountryCode ("AT")
                                                         .endUserID ("abc")
                                                         .build ();
    assertNotNull (aItem);
    assertEquals (aNow.atZoneSameInstant (ZoneOffset.UTC).toOffsetDateTime (), aItem.getExchangeDTUTC ());
    assertEquals (EReportingDirection.RECEIVING, aItem.getDirection ());
    assertFalse (aItem.isSending ());
    assertTrue (aItem.isReceiving ());
    assertEquals (sOtherSPID, aItem.getC2ID ());
    assertEquals (sMySPID, aItem.getC3ID ());
    assertEquals (sOtherSPID, aItem.getOtherServiceProviderID ());
    assertEquals (EPredefinedDocumentTypeIdentifier.INVOICE_EN16931_PEPPOL_V30.getScheme (),
                  aItem.getDocTypeIDScheme ());
    assertEquals (EPredefinedDocumentTypeIdentifier.INVOICE_EN16931_PEPPOL_V30.getValue (), aItem.getDocTypeIDValue ());
    assertEquals (EPredefinedProcessIdentifier.BIS3_BILLING.getScheme (), aItem.getProcessIDScheme ());
    assertEquals (EPredefinedProcessIdentifier.BIS3_BILLING.getValue (), aItem.getProcessIDValue ());
    assertEquals ("peppol-transport-as4-v2_0", aItem.getTransportProtocol ());
    assertEquals ("FI", aItem.getC1CountryCode ());
    assertEquals ("AT", aItem.getC4CountryCode ());
    assertEquals ("AT", aItem.getEndUserCountryCode ());
    assertEquals ("abc", aItem.getEndUserID ());
  }

  @Test
  public void testIsComplete ()
  {
    final OffsetDateTime aNow = PDTFactory.getCurrentOffsetDateTime ();
    final String sMySPID = "PAT000001";
    final String sOtherSPID = "POP000002";

    final Builder aBuilder = PeppolReportingItem.builder ();
    assertFalse (aBuilder.isComplete (false));
    aBuilder.exchangeDateTime (aNow);
    assertFalse (aBuilder.isComplete (false));
    aBuilder.directionReceiving ();
    assertFalse (aBuilder.isComplete (false));
    aBuilder.c2ID (sOtherSPID);
    assertFalse (aBuilder.isComplete (false));
    aBuilder.c3ID (sMySPID);
    assertFalse (aBuilder.isComplete (false));
    aBuilder.docTypeIDScheme (EPredefinedDocumentTypeIdentifier.INVOICE_EN16931_PEPPOL_V30.getScheme ());
    assertFalse (aBuilder.isComplete (false));
    aBuilder.docTypeIDValue (EPredefinedDocumentTypeIdentifier.INVOICE_EN16931_PEPPOL_V30.getValue ());
    assertFalse (aBuilder.isComplete (false));
    aBuilder.processIDScheme (EPredefinedProcessIdentifier.BIS3_BILLING.getScheme ());
    assertFalse (aBuilder.isComplete (false));
    aBuilder.processIDValue (EPredefinedProcessIdentifier.BIS3_BILLING.getValue ());
    assertFalse (aBuilder.isComplete (false));
    aBuilder.transportProtocolPeppolAS4v2 ();
    assertFalse (aBuilder.isComplete (false));
    aBuilder.c1CountryCode ("FI");
    assertFalse (aBuilder.isComplete (false));
    aBuilder.c4CountryCode ("AT");
    assertFalse (aBuilder.isComplete (false));
    aBuilder.endUserID ("abc");
    assertTrue (aBuilder.isComplete (false));
    assertNotNull (aBuilder.build ());
  }

  @Test
  public void testMaxLength ()
  {
    final OffsetDateTime aNow = PDTFactory.getCurrentOffsetDateTime ();
    final String sMySPID = "PAT000001";
    final String sOtherSPID = "POP000002";

    final Builder aBuilder = PeppolReportingItem.builder ()
                                                .exchangeDateTime (aNow)
                                                .directionReceiving ()
                                                .c2ID (sOtherSPID)
                                                .c3ID (sMySPID)
                                                .docTypeID (EPredefinedDocumentTypeIdentifier.INVOICE_EN16931_PEPPOL_V30)
                                                .processID (EPredefinedProcessIdentifier.BIS3_BILLING)
                                                .transportProtocolPeppolAS4v2 ()
                                                .c1CountryCode ("FI")
                                                .c4CountryCode ("AT")
                                                .endUserID ("abc");
    assertTrue (aBuilder.isComplete (false));

    // C2
    aBuilder.c2ID (StringHelper.getRepeated ('a', PeppolReportingItem.MAX_LEN_C2_ID + 1));
    assertFalse (aBuilder.isComplete (false));
    aBuilder.c2ID (sOtherSPID);
    assertTrue (aBuilder.isComplete (false));

    // C3
    aBuilder.c3ID (StringHelper.getRepeated ('a', PeppolReportingItem.MAX_LEN_C3_ID + 1));
    assertFalse (aBuilder.isComplete (false));
    aBuilder.c3ID (sOtherSPID);
    assertTrue (aBuilder.isComplete (false));

    // DocTypeID Scheme
    aBuilder.docTypeIDScheme (StringHelper.getRepeated ('a', PeppolReportingItem.MAX_LEN_DOCTYPE_SCHEME + 1));
    assertFalse (aBuilder.isComplete (false));
    aBuilder.docTypeIDScheme (EPredefinedDocumentTypeIdentifier.INVOICE_EN16931_PEPPOL_V30.getScheme ());
    assertTrue (aBuilder.isComplete (false));

    // DocTypeID Value
    aBuilder.docTypeIDValue (StringHelper.getRepeated ('a', PeppolReportingItem.MAX_LEN_DOCTYPE_VALUE + 1));
    assertFalse (aBuilder.isComplete (false));
    aBuilder.docTypeIDValue (EPredefinedDocumentTypeIdentifier.INVOICE_EN16931_PEPPOL_V30.getValue ());
    assertTrue (aBuilder.isComplete (false));

    // ProcessID Scheme
    aBuilder.processIDScheme (StringHelper.getRepeated ('a', PeppolReportingItem.MAX_LEN_PROCESS_SCHEME + 1));
    assertFalse (aBuilder.isComplete (false));
    aBuilder.processIDScheme (EPredefinedProcessIdentifier.BIS3_BILLING.getScheme ());
    assertTrue (aBuilder.isComplete (false));

    // ProcessID Value
    aBuilder.processIDValue (StringHelper.getRepeated ('a', PeppolReportingItem.MAX_LEN_PROCESS_VALUE + 1));
    assertFalse (aBuilder.isComplete (false));
    aBuilder.processIDValue (EPredefinedProcessIdentifier.BIS3_BILLING.getValue ());
    assertTrue (aBuilder.isComplete (false));

    // Transport Protocol
    aBuilder.transportProtocol (StringHelper.getRepeated ('a', PeppolReportingItem.MAX_LEN_TRANSPORT_PROTOCOL + 1));
    assertFalse (aBuilder.isComplete (false));
    aBuilder.transportProtocolPeppolAS4v2 ();
    assertTrue (aBuilder.isComplete (false));

    // C1 Country Code
    aBuilder.c1CountryCode (StringHelper.getRepeated ('a', PeppolReportingItem.MAX_LEN_C1_COUNTRY_CODE + 1));
    assertFalse (aBuilder.isComplete (false));
    // too short
    aBuilder.c1CountryCode ("F");
    assertFalse (aBuilder.isComplete (false));
    // Wrong casing
    aBuilder.c1CountryCode ("fi");
    assertFalse (aBuilder.isComplete (false));
    aBuilder.c1CountryCode ("FI");
    assertTrue (aBuilder.isComplete (false));

    // C4 Country Code
    aBuilder.c4CountryCode (StringHelper.getRepeated ('a', PeppolReportingItem.MAX_LEN_C4_COUNTRY_CODE + 1));
    assertFalse (aBuilder.isComplete (false));
    // too short
    aBuilder.c4CountryCode ("A");
    assertFalse (aBuilder.isComplete (false));
    // Wrong casing
    aBuilder.c4CountryCode ("at");
    assertFalse (aBuilder.isComplete (false));
    aBuilder.c4CountryCode ("AT");
    assertTrue (aBuilder.isComplete (false));

    // End User ID
    aBuilder.endUserID (StringHelper.getRepeated ('a', PeppolReportingItem.MAX_LEN_END_USER_ID + 1));
    assertFalse (aBuilder.isComplete (false));
    aBuilder.endUserID ("abc");
    assertTrue (aBuilder.isComplete (false));

    assertNotNull (aBuilder.build ());
  }
}
