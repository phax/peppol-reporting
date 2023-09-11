package com.helper.peppol.reporting.api;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.ValueEnforcer;
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

    if (PeppolIdentifierHelper.DOCUMENT_TYPE_SCHEME_BUSDOX_DOCID_QNS.equals (aDocTypeID.getScheme ()))
    {
      final String sValue = aDocTypeID.getValue ();

      // EUSR 1.1
      if ("urn:fdc:peppol:end-user-statistics-report:1.1::EndUserStatisticsReport##urn:fdc:peppol.eu:edec:trns:end-user-statistics-report:1.1::1.1".equals (sValue))
        return false;
      // TSR 1.0
      if ("urn:fdc:peppol:transaction-statistics-report:1.0::TransactionStatisticsReport##urn:fdc:peppol.eu:edec:trns:transaction-statistics-reporting:1.0::1.0".equals (sValue))
        return false;
    }

    // All others are okay
    return true;
  }
}
