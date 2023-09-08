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
package com.helper.peppol.reporting.backend.mongodb;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.ThreadLocalRandom;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.mutable.MutableInt;
import com.helger.commons.state.ESuccess;
import com.helger.config.ConfigFactory;
import com.helger.peppolid.peppol.doctype.EPredefinedDocumentTypeIdentifier;
import com.helger.peppolid.peppol.process.EPredefinedProcessIdentifier;
import com.helper.peppol.reporting.api.EReportingDirection;
import com.helper.peppol.reporting.api.PeppolReportingItem;
import com.helper.peppol.reporting.api.backend.PeppolReportingBackend;
import com.helper.peppol.reporting.api.backend.PeppolReportingBackendException;

/**
 * Test class for class {@link PeppolReportingBackendMongoDB}.
 *
 * @author Philip Helger
 */
public final class PeppolReportingBackendMongoDBTest
{
  private static final Logger LOGGER = LoggerFactory.getLogger (PeppolReportingBackendMongoDBTest.class);

  @Test
  public void testBasic () throws PeppolReportingBackendException
  {
    // The default configuration uses e.g.
    // src/test/resources/application.properties for the configuration
    final ESuccess eSuccess = PeppolReportingBackend.withBackendDo (ConfigFactory.getDefaultConfig (), aBackend -> {
      final ThreadLocalRandom aTLR = ThreadLocalRandom.current ();
      final int nReportItems = aTLR.nextInt (100);
      LOGGER.info ("Creating " + nReportItems + " test reporting items");

      boolean bSending = true;
      for (int i = 0; i < nReportItems; ++i)
      {
        final PeppolReportingItem aItem = PeppolReportingItem.builder ()
                                                             .exchangeDateTime (PDTFactory.getCurrentOffsetDateTime ())
                                                             .direction (bSending ? EReportingDirection.SENDING
                                                                                  : EReportingDirection.RECEIVING)
                                                             .c2ID ("pop000001")
                                                             .c3ID ("pop000002")
                                                             .docTypeID (EPredefinedDocumentTypeIdentifier.INVOICE_EN16931_PEPPOL_V30)
                                                             .processID (EPredefinedProcessIdentifier.BIS3_BILLING)
                                                             .transportProtocolPeppolAS4v2 ()
                                                             .c1CountryCode ("FI")
                                                             .c4CountryCode (bSending ? null : "DE")
                                                             .endUserID ("eu" + Integer.toString (aTLR.nextInt (10)))
                                                             .build ();

        aBackend.storeReportingItem (aItem);

        bSending = !bSending;
      }

      // At least the amount of written entries must be available
      final MutableInt aCounter = new MutableInt (0);
      aBackend.forEachReportingItem (PDTFactory.getCurrentYearMonth (), aLoadedItem -> {
        aCounter.inc ();
      });
      assertTrue (aCounter.intValue () >= nReportItems);
    });
    assertTrue (eSuccess.isSuccess ());
  }
}
