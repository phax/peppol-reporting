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
package com.helger.peppol.reporting.api.envelope;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.jspecify.annotations.NonNull;
import org.junit.Test;

import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.datetime.helper.PDTFactory;
import com.helger.peppol.reporting.api.PeppolReportingItem;
import com.helger.peppol.reporting.api.PeppolReportingItemWithID;
import com.helger.peppolid.peppol.doctype.EPredefinedDocumentTypeIdentifier;
import com.helger.peppolid.peppol.process.EPredefinedProcessIdentifier;
import com.helger.unittest.support.TestHelper;

/**
 * Test class for class {@link PeppolReportingEnvelope}.
 *
 * @author Philip Helger
 */
public final class PeppolReportingEnvelopeTest
{
  private static final String SEAT_ID = "PAT000001";
  private static final String SOURCE_ID = "5f0c6b1e-0a43-4d5c-9b0e-2c3e5c1f7a10";

  @NonNull
  static PeppolReportingItemWithID createItem (@NonNull final String sItemID)
  {
    final PeppolReportingItem aItem = PeppolReportingItem.builder ()
                                                         .exchangeDateTime (PDTFactory.getCurrentOffsetDateTimeMillisOnly ())
                                                         .directionSending ()
                                                         .c2ID (SEAT_ID)
                                                         .c3ID ("POP000002")
                                                         .docTypeID (EPredefinedDocumentTypeIdentifier.INVOICE_EN16931_PEPPOL_V30)
                                                         .processID (EPredefinedProcessIdentifier.BIS3_BILLING)
                                                         .transportProtocolPeppolAS4v2 ()
                                                         .c1CountryCode ("FI")
                                                         .endUserID ("abc")
                                                         .build ();
    return new PeppolReportingItemWithID (sItemID, aItem);
  }

  @NonNull
  private static ICommonsList <PeppolReportingItemWithID> _createItems (final int nCount)
  {
    final ICommonsList <PeppolReportingItemWithID> ret = new CommonsArrayList <> ();
    for (int i = 0; i < nCount; ++i)
      ret.add (createItem ("id" + i));
    return ret;
  }

  private static void _expectIAE (@NonNull final Runnable aRunnable)
  {
    try
    {
      aRunnable.run ();
      fail ();
    }
    catch (final IllegalArgumentException ex)
    {
      // expected
    }
  }

  @Test
  public void testBasic ()
  {
    final ICommonsList <PeppolReportingItemWithID> aItems = _createItems (3);
    final PeppolReportingEnvelope aEnvelope = new PeppolReportingEnvelope (SEAT_ID, SOURCE_ID, aItems);
    assertEquals (SEAT_ID, aEnvelope.getSeatID ());
    assertEquals (SOURCE_ID, aEnvelope.getSourceID ());
    assertEquals (3, aEnvelope.getItemCount ());
    assertEquals (aItems, aEnvelope.getAllItems ());

    // Changing the source list does not change the envelope
    aItems.add (createItem ("other"));
    assertEquals (3, aEnvelope.getItemCount ());

    TestHelper.testDefaultImplementationWithEqualContentObject (aEnvelope,
                                                                new PeppolReportingEnvelope (SEAT_ID,
                                                                                             SOURCE_ID,
                                                                                             aEnvelope.getAllItems ()));
    TestHelper.testDefaultImplementationWithDifferentContentObject (aEnvelope,
                                                                    new PeppolReportingEnvelope (SEAT_ID,
                                                                                                 "other",
                                                                                                 aEnvelope.getAllItems ()));
  }

  @Test
  public void testMaxItems ()
  {
    new PeppolReportingEnvelope (SEAT_ID, SOURCE_ID, _createItems (PeppolReportingEnvelope.MAX_ITEMS));
    _expectIAE ( () -> new PeppolReportingEnvelope (SEAT_ID,
                                                    SOURCE_ID,
                                                    _createItems (PeppolReportingEnvelope.MAX_ITEMS + 1)));
  }

  @Test
  public void testInvalid ()
  {
    final ICommonsList <PeppolReportingItemWithID> aItems = _createItems (1);
    _expectIAE ( () -> new PeppolReportingEnvelope ("", SOURCE_ID, aItems));
    _expectIAE ( () -> new PeppolReportingEnvelope (StringHelper.getRepeated ('a',
                                                                              PeppolReportingEnvelope.MAX_LEN_SEAT_ID +
                                                                                   1),
                                                    SOURCE_ID,
                                                    aItems));
    _expectIAE ( () -> new PeppolReportingEnvelope (SEAT_ID, "", aItems));
    _expectIAE ( () -> new PeppolReportingEnvelope (SEAT_ID,
                                                    StringHelper.getRepeated ('a',
                                                                              PeppolReportingEnvelope.MAX_LEN_SOURCE_ID +
                                                                                   1),
                                                    aItems));
    // No items
    _expectIAE ( () -> new PeppolReportingEnvelope (SEAT_ID, SOURCE_ID, new CommonsArrayList <> ()));
    // Duplicate item ID
    _expectIAE ( () -> new PeppolReportingEnvelope (SEAT_ID,
                                                    SOURCE_ID,
                                                    new CommonsArrayList <> (createItem ("a"), createItem ("a"))));
  }
}
