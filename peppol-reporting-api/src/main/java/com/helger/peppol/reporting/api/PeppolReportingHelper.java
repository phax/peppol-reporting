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
package com.helger.peppol.reporting.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.regex.RegExHelper;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.peppol.PeppolIdentifierHelper;

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
   * Check if a document type is eligible for TSR or not. Based on the TSR
   * specification 1.0 chapter 2.2.1 and EUSR specification 1.1 chapter 2.2.1:
   * <ul>
   * <li>End User Statistics Reports MUST NOT be counted for (a Transaction
   * Statistics Report|an End User Statistics Report)</li>
   * <li>Transaction Statistics Reports MUST NOT be counted for (a Transaction
   * Statistics Report|an End User Statistics Report)</li>
   * </ul>
   *
   * @param aDocTypeID
   *        The document type ID to be checked. May be <code>null</code>.
   * @return <code>false</code> if the document type is not suitable for
   *         reporting, <code>true</code> otherwise.
   */
  public static boolean isDocumentTypeEligableForReporting (@Nonnull final IDocumentTypeIdentifier aDocTypeID)
  {
    ValueEnforcer.notNull (aDocTypeID, "DocTypeID");

    return isDocumentTypeEligableForReporting (aDocTypeID.getScheme (), aDocTypeID.getValue ());
  }

  /**
   * Check if a document type is eligible for TSR or not. Based on the TSR
   * specification 1.0 chapter 2.2.1 and EUSR specification 1.1 chapter 2.2.1:
   * <ul>
   * <li>End User Statistics Reports MUST NOT be counted for (a Transaction
   * Statistics Report|an End User Statistics Report)</li>
   * <li>Transaction Statistics Reports MUST NOT be counted for (a Transaction
   * Statistics Report|an End User Statistics Report)</li>
   * </ul>
   *
   * @param sDocTypeIDScheme
   *        The document type ID scheme to be checked. May be <code>null</code>.
   * @param sDocTypeIDValue
   *        The document type ID value to be checked. May be <code>null</code>.
   * @return <code>false</code> if the document type is not suitable for
   *         reporting, <code>true</code> otherwise.
   * @since 3.0.3
   */
  public static boolean isDocumentTypeEligableForReporting (@Nullable final String sDocTypeIDScheme,
                                                            @Nullable final String sDocTypeIDValue)
  {
    if (PeppolIdentifierHelper.DOCUMENT_TYPE_SCHEME_BUSDOX_DOCID_QNS.equals (sDocTypeIDScheme))
    {
      // EUSR 1.1
      if ("urn:fdc:peppol:end-user-statistics-report:1.1::EndUserStatisticsReport##urn:fdc:peppol.eu:edec:trns:end-user-statistics-report:1.1::1.1".equals (sDocTypeIDValue))
        return false;
      // TSR 1.0
      if ("urn:fdc:peppol:transaction-statistics-report:1.0::TransactionStatisticsReport##urn:fdc:peppol.eu:edec:trns:transaction-statistics-reporting:1.0::1.0".equals (sDocTypeIDValue))
        return false;
    }

    // All others are okay
    return true;
  }
}
