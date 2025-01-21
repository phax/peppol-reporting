/*
 * Copyright (C) 2022-2025 Philip Helger
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
package com.helger.peppol.reporting.backend.mongodb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.time.OffsetDateTime;

import org.bson.Document;
import org.junit.Test;

import com.helger.commons.datetime.PDTFactory;
import com.helger.peppol.reporting.api.PeppolReportingItem;
import com.helger.peppolid.peppol.doctype.EPredefinedDocumentTypeIdentifier;
import com.helger.peppolid.peppol.process.EPredefinedProcessIdentifier;

/**
 * Test class for class {@link PeppolReportingMongoDBHelper}
 *
 * @author Philip Helger
 */
public final class PeppolReportingMongoDBHelperTest
{
  @Test
  public void testBasic ()
  {
    final OffsetDateTime aNow = PDTFactory.getCurrentOffsetDateTime ();
    final PeppolReportingItem aItem = PeppolReportingItem.builder ()
                                                         .exchangeDateTime (aNow)
                                                         .directionSending ()
                                                         .c2ID ("pop000001")
                                                         .c3ID ("pop000002")
                                                         .docTypeID (EPredefinedDocumentTypeIdentifier.INVOICE_EN16931_PEPPOL_V30)
                                                         .processID (EPredefinedProcessIdentifier.BIS3_BILLING)
                                                         .transportProtocolPeppolAS4v2 ()
                                                         .c1CountryCode ("FI")
                                                         .endUserID ("12345")
                                                         .build ();

    // To BSON
    final Document aDoc = PeppolReportingMongoDBHelper.toBson (aItem);
    assertNotNull (aDoc);

    // And back
    final PeppolReportingItem aItem2 = PeppolReportingMongoDBHelper.toDomain (aDoc);
    assertNotNull (aItem2);

    // Should be identical
    assertEquals (aItem, aItem2);
  }
}
