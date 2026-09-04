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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.cache.regex.RegExHelper;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.peppol.PeppolIdentifierHelper;
import com.helger.peppolid.peppol.doctype.EPredefinedDocumentTypeIdentifier;

/**
 * Miscellaneous helper methods regarding Peppol Reporting
 *
 * @author Philip Helger
 * @since 2.1.2
 */
@Immutable
public final class PeppolReportingHelper
{
  private PeppolReportingHelper ()
  {}

  /**
   * Check if the provided country code is valid according to the Peppol rules.
   * It must be 2 characters long and follow a provided RegEx.
   *
   * @param s
   *        The country code to check. May be <code>null</code>.
   * @return <code>true</code> if it is a valid country code, <code>false</code>
   *         if not.
   * @since 2.2.2
   */
  public static boolean isValidCountryCode (@Nullable final String s)
  {
    return s != null && s.length () == 2 && RegExHelper.stringMatchesPattern ("[0-9A-Z]{2}", s);
  }

  /**
   * Check if a document type is eligible for the provided report type or not.
   * Based on the TSR specification 1.0 chapter 2.2.1 and EUSR specification 1.1
   * chapter 2.2.1:
   * <ul>
   * <li>End User Statistics Reports MUST NOT be counted for (a Transaction
   * Statistics Report|an End User Statistics Report)</li>
   * <li>Transaction Statistics Reports MUST NOT be counted for (a Transaction
   * Statistics Report|an End User Statistics Report)</li>
   * </ul>
   * Additionally Message Level Status (MLS) messages are only counted for a
   * Transaction Statistics Report but not for an End User Statistics Report, as
   * they are not related to an end user.
   *
   * @param eReportType
   *        The report type to check the eligibility for. May not be
   *        <code>null</code>.
   * @param sDocTypeIDScheme
   *        The document type ID scheme to be checked. May be <code>null</code>.
   * @param sDocTypeIDValue
   *        The document type ID value to be checked. May be <code>null</code>.
   * @return <code>false</code> if the document type is not suitable for the
   *         provided report type, <code>true</code> otherwise.
   * @since 4.3.0
   */
  public static boolean isDocumentTypeEligableForReporting (@NonNull final EPeppolReportType eReportType,
                                                            @Nullable final String sDocTypeIDScheme,
                                                            @Nullable final String sDocTypeIDValue)
  {
    ValueEnforcer.notNull (eReportType, "ReportType");

    if (PeppolIdentifierHelper.DOCUMENT_TYPE_SCHEME_BUSDOX_DOCID_QNS.equals (sDocTypeIDScheme))
    {
      // EUSR 1.1 - never counted
      if ("urn:fdc:peppol:end-user-statistics-report:1.1::EndUserStatisticsReport##urn:fdc:peppol.eu:edec:trns:end-user-statistics-report:1.1::1.1".equals (sDocTypeIDValue))
        return false;
      // TSR 1.0 - never counted
      if ("urn:fdc:peppol:transaction-statistics-report:1.0::TransactionStatisticsReport##urn:fdc:peppol.eu:edec:trns:transaction-statistics-reporting:1.0::1.0".equals (sDocTypeIDValue))
        return false;
      // MLS 1.0 - only counted for TSR (see SPOG on MLS)
      if (eReportType.isEUSR () &&
          EPredefinedDocumentTypeIdentifier.PEPPOL_MLS_1_0.getValue ().equals (sDocTypeIDValue))
        return false;
    }

    // All others are okay
    return true;
  }

  /**
   * Check if a document type is eligible for the provided report type or not.
   * See
   * {@link #isDocumentTypeEligableForReporting(EPeppolReportType, String, String)}
   * for the details.
   *
   * @param eReportType
   *        The report type to check the eligibility for. May not be
   *        <code>null</code>.
   * @param aDocTypeID
   *        The document type ID to be checked. May not be <code>null</code>.
   * @return <code>false</code> if the document type is not suitable for the
   *         provided report type, <code>true</code> otherwise.
   * @since 4.3.0
   */
  public static boolean isDocumentTypeEligableForReporting (@NonNull final EPeppolReportType eReportType,
                                                            @NonNull final IDocumentTypeIdentifier aDocTypeID)
  {
    ValueEnforcer.notNull (aDocTypeID, "DocTypeID");

    return isDocumentTypeEligableForReporting (eReportType, aDocTypeID.getScheme (), aDocTypeID.getValue ());
  }

  /**
   * Check if a document type is eligible for at least one of the existing
   * report types or not. This is the check to be used when storing reporting
   * items, as the stored items are the source for all report types. Use
   * {@link #isDocumentTypeEligableForReporting(EPeppolReportType, String, String)}
   * to check the eligibility for a specific report type.
   *
   * @param sDocTypeIDScheme
   *        The document type ID scheme to be checked. May be <code>null</code>.
   * @param sDocTypeIDValue
   *        The document type ID value to be checked. May be <code>null</code>.
   * @return <code>false</code> if the document type is not suitable for any
   *         reporting, <code>true</code> otherwise.
   * @since 3.0.3
   */
  public static boolean isDocumentTypeEligableForReporting (@Nullable final String sDocTypeIDScheme,
                                                            @Nullable final String sDocTypeIDValue)
  {
    for (final EPeppolReportType eReportType : EPeppolReportType.values ())
      if (isDocumentTypeEligableForReporting (eReportType, sDocTypeIDScheme, sDocTypeIDValue))
        return true;
    return false;
  }

  /**
   * Check if a document type is eligible for at least one of the existing
   * report types or not. See
   * {@link #isDocumentTypeEligableForReporting(String, String)} for the
   * details.
   *
   * @param aDocTypeID
   *        The document type ID to be checked. May not be <code>null</code>.
   * @return <code>false</code> if the document type is not suitable for any
   *         reporting, <code>true</code> otherwise.
   */
  public static boolean isDocumentTypeEligableForReporting (@NonNull final IDocumentTypeIdentifier aDocTypeID)
  {
    ValueEnforcer.notNull (aDocTypeID, "DocTypeID");

    return isDocumentTypeEligableForReporting (aDocTypeID.getScheme (), aDocTypeID.getValue ());
  }
}
