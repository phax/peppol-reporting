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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import org.jspecify.annotations.NonNull;
import org.junit.Test;

import com.helger.base.string.StringHelper;
import com.helger.datetime.helper.PDTFactory;
import com.helger.peppolid.peppol.doctype.EPredefinedDocumentTypeIdentifier;
import com.helger.peppolid.peppol.process.EPredefinedProcessIdentifier;
import com.helger.unittest.support.TestHelper;

/**
 * Test class for class {@link PeppolReportingItemWithID}.
 *
 * @author Philip Helger
 */
public final class PeppolReportingItemWithIDTest
{
  @NonNull
  private static PeppolReportingItem _item ()
  {
    return PeppolReportingItem.builder ()
                              .exchangeDateTime (PDTFactory.getCurrentOffsetDateTimeMillisOnly ())
                              .directionSending ()
                              .c2ID ("PAT000001")
                              .c3ID ("POP000002")
                              .docTypeID (EPredefinedDocumentTypeIdentifier.INVOICE_EN16931_PEPPOL_V30)
                              .processID (EPredefinedProcessIdentifier.BIS3_BILLING)
                              .transportProtocolPeppolAS4v2 ()
                              .c1CountryCode ("FI")
                              .endUserID ("abc")
                              .build ();
  }

  @Test
  public void testBasic ()
  {
    final PeppolReportingItem aItem = _item ();
    final PeppolReportingItemWithID aItemWithID = new PeppolReportingItemWithID ("id1", aItem);
    assertEquals ("id1", aItemWithID.getItemID ());
    assertSame (aItem, aItemWithID.getReportingItem ());

    TestHelper.testDefaultImplementationWithEqualContentObject (aItemWithID,
                                                                new PeppolReportingItemWithID ("id1", aItem));
    TestHelper.testDefaultImplementationWithDifferentContentObject (aItemWithID,
                                                                    new PeppolReportingItemWithID ("id2", aItem));
  }

  @Test
  public void testInvalid ()
  {
    final PeppolReportingItem aItem = _item ();
    try
    {
      new PeppolReportingItemWithID ("", aItem);
      fail ();
    }
    catch (final IllegalArgumentException ex)
    {
      // expected
    }

    // Maximum length is fine
    new PeppolReportingItemWithID (StringHelper.getRepeated ('a', PeppolReportingItemWithID.MAX_LEN_ITEM_ID), aItem);
    try
    {
      new PeppolReportingItemWithID (StringHelper.getRepeated ('a', PeppolReportingItemWithID.MAX_LEN_ITEM_ID + 1),
                                     aItem);
      fail ();
    }
    catch (final IllegalArgumentException ex)
    {
      // expected
    }
  }
}
