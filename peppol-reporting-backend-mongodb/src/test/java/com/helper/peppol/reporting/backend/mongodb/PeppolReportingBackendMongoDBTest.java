package com.helper.peppol.reporting.backend.mongodb;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.state.ESuccess;
import com.helger.config.ConfigFactory;
import com.helger.peppolid.peppol.doctype.EPredefinedDocumentTypeIdentifier;
import com.helger.peppolid.peppol.process.EPredefinedProcessIdentifier;
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
  @Test
  public void testBasic () throws PeppolReportingBackendException
  {
    final ESuccess eSuccess = PeppolReportingBackend.withBackendDo (ConfigFactory.getDefaultConfig (), aBackend -> {

      final PeppolReportingItem aItem = PeppolReportingItem.builder ()
                                                           .exchangeDateTime (PDTFactory.getCurrentOffsetDateTime ())
                                                           .directionSending ()
                                                           .c2ID ("pop000001")
                                                           .c3ID ("pop000002")
                                                           .docTypeID (EPredefinedDocumentTypeIdentifier.INVOICE_EN16931_PEPPOL_V30)
                                                           .processID (EPredefinedProcessIdentifier.BIS3_BILLING)
                                                           .transportProtocolPeppolAS4v2 ()
                                                           .c1CountryCode ("FI")
                                                           .endUserID ("12345")
                                                           .build ();

      aBackend.storeReportingItem (aItem);
    });
    assertTrue (eSuccess.isSuccess ());
  }
}
