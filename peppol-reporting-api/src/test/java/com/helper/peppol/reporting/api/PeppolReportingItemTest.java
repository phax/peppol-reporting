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
import com.helger.peppolid.peppol.doctype.EPredefinedDocumentTypeIdentifier;
import com.helger.peppolid.peppol.process.EPredefinedProcessIdentifier;

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

    final PeppolReportingItem aItem = PeppolReportingItem.builder ()
                                                         .exchangeDateTime (aNow)
                                                         .directionSending ()
                                                         .c2ID (sMySPID)
                                                         .c3ID (sOtherSPID)
                                                         .docTypeID (EPredefinedDocumentTypeIdentifier.INVOICE_EN16931_PEPPOL_V30)
                                                         .processID (EPredefinedProcessIdentifier.BIS3_BILLING)
                                                         .transportProtocolPeppolAS4v2 ()
                                                         .c1CountryCode ("FI")
                                                         .endUserID ("abc")
                                                         .build ();
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
  }
}
