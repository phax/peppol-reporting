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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.helger.peppolid.peppol.doctype.EPredefinedDocumentTypeIdentifier;

/**
 * Test class for class {@link PeppolReportingHelper}
 *
 * @author Philip Helger
 */
public final class PeppolReportingHelperTest
{
  private static final EPredefinedDocumentTypeIdentifier DT_INVOICE = EPredefinedDocumentTypeIdentifier.INVOICE_EN16931_PEPPOL_V30;
  private static final EPredefinedDocumentTypeIdentifier DT_MLS = EPredefinedDocumentTypeIdentifier.PEPPOL_MLS_1_0;
  private static final EPredefinedDocumentTypeIdentifier DT_EUSR = EPredefinedDocumentTypeIdentifier.ENDUSERSTATISTICSREPORT_FDC_PEPPOL_EU_EDEC_TRNS_END_USER_STATISTICS_REPORT_1_1;
  private static final EPredefinedDocumentTypeIdentifier DT_TSR = EPredefinedDocumentTypeIdentifier.TRANSACTIONSTATISTICSREPORT_FDC_PEPPOL_EU_EDEC_TRNS_TRANSACTION_STATISTICS_REPORTING_1_0;

  @Test
  public void testIsValidCountryCode ()
  {
    assertTrue (PeppolReportingHelper.isValidCountryCode ("AT"));
    assertTrue (PeppolReportingHelper.isValidCountryCode ("ZZ"));
    assertTrue (PeppolReportingHelper.isValidCountryCode ("12"));

    assertFalse (PeppolReportingHelper.isValidCountryCode (null));
    assertFalse (PeppolReportingHelper.isValidCountryCode (""));
    assertFalse (PeppolReportingHelper.isValidCountryCode ("A"));
    assertFalse (PeppolReportingHelper.isValidCountryCode ("AUT"));
    assertFalse (PeppolReportingHelper.isValidCountryCode ("at"));
  }

  @Test
  public void testEligableForTSR ()
  {
    // Regular business documents are counted
    assertTrue (PeppolReportingHelper.isDocumentTypeEligableForReporting (EPeppolReportType.TSR_V10, DT_INVOICE));

    // MLS is counted for TSR only
    assertTrue (PeppolReportingHelper.isDocumentTypeEligableForReporting (EPeppolReportType.TSR_V10, DT_MLS));

    // Reports themselves are never counted
    assertFalse (PeppolReportingHelper.isDocumentTypeEligableForReporting (EPeppolReportType.TSR_V10, DT_EUSR));
    assertFalse (PeppolReportingHelper.isDocumentTypeEligableForReporting (EPeppolReportType.TSR_V10, DT_TSR));
  }

  @Test
  public void testEligableForEUSR ()
  {
    // Regular business documents are counted
    assertTrue (PeppolReportingHelper.isDocumentTypeEligableForReporting (EPeppolReportType.EUSR_V11, DT_INVOICE));

    // MLS is not related to an end user, so it is not counted for EUSR
    assertFalse (PeppolReportingHelper.isDocumentTypeEligableForReporting (EPeppolReportType.EUSR_V11, DT_MLS));

    // Reports themselves are never counted
    assertFalse (PeppolReportingHelper.isDocumentTypeEligableForReporting (EPeppolReportType.EUSR_V11, DT_EUSR));
    assertFalse (PeppolReportingHelper.isDocumentTypeEligableForReporting (EPeppolReportType.EUSR_V11, DT_TSR));
  }

  @Test
  public void testEligableForAnyReport ()
  {
    assertTrue (PeppolReportingHelper.isDocumentTypeEligableForReporting (DT_INVOICE));

    // MLS must be stored, because it is needed for TSR
    assertTrue (PeppolReportingHelper.isDocumentTypeEligableForReporting (DT_MLS));

    assertFalse (PeppolReportingHelper.isDocumentTypeEligableForReporting (DT_EUSR));
    assertFalse (PeppolReportingHelper.isDocumentTypeEligableForReporting (DT_TSR));

    // Unknown scheme is always okay
    assertTrue (PeppolReportingHelper.isDocumentTypeEligableForReporting ((String) null, null));
    assertTrue (PeppolReportingHelper.isDocumentTypeEligableForReporting ("other-scheme", DT_TSR.getValue ()));
  }
}
