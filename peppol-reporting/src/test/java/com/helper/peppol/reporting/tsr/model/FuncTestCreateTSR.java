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
package com.helper.peppol.reporting.tsr.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.util.function.IntFunction;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.datetime.XMLOffsetDate;
import com.helger.peppol.reporting.jaxb.tsr.v101.TransactionStatisticsReportType;
import com.helger.peppolid.peppol.doctype.EPredefinedDocumentTypeIdentifier;
import com.helger.peppolid.peppol.process.EPredefinedProcessIdentifier;
import com.helger.schematron.svrl.SVRLHelper;
import com.helger.schematron.svrl.jaxb.SchematronOutputType;
import com.helper.peppol.reporting.api.CPeppolReporting;
import com.helper.peppol.reporting.api.PeppolReportingItem;
import com.helper.peppol.reporting.tsr.TransactionStatisticsReport;
import com.helper.peppol.reporting.tsr.TransactionStatisticsReport101Marshaller;
import com.helper.peppol.reporting.tsr.TransactionStatisticsReportValidator;

public final class FuncTestCreateTSR
{
  private static final Logger LOGGER = LoggerFactory.getLogger (FuncTestCreateTSR.class);
  private static final String MY_SPID = "PDE000001";

  @Test
  public void testCreateEmpty () throws Exception
  {
    final OffsetDateTime aNow = PDTFactory.getCurrentOffsetDateTime ();

    // Create report without any transaction
    final TransactionStatisticsReportType aReport = TransactionStatisticsReport.builder ()
                                                                               .monthOf (aNow)
                                                                               .reportingServiceProviderID (MY_SPID)
                                                                               .reportingItemList (new CommonsArrayList <> ())
                                                                               .build ();

    // Check content
    assertEquals (CPeppolReporting.TSR_CUSTOMIZATION_ID_V10, aReport.getCustomizationIDValue ());
    assertEquals (CPeppolReporting.TSR_PROFILE_ID_V10, aReport.getProfileIDValue ());
    assertNotNull (aReport.getHeader ());

    assertNotNull (aReport.getHeader ().getReportPeriod ());
    assertEquals (XMLOffsetDate.of (aNow.toLocalDate ().withDayOfMonth (1)),
                  aReport.getHeader ().getReportPeriod ().getStartDate ());
    assertEquals (XMLOffsetDate.of (aNow.toLocalDate ().withDayOfMonth (1).plusMonths (1).minusDays (1)),
                  aReport.getHeader ().getReportPeriod ().getEndDate ());

    assertNotNull (aReport.getHeader ().getReporterID ());
    assertEquals (CPeppolReporting.SERVICE_PROVIDER_ID_SCHEME, aReport.getHeader ().getReporterID ().getSchemeID ());
    assertEquals (MY_SPID, aReport.getHeader ().getReporterIDValue ());

    // Full set
    assertNotNull (aReport.getTotal ());
    assertEquals (BigInteger.ZERO, aReport.getTotal ().getIncoming ());
    assertEquals (BigInteger.ZERO, aReport.getTotal ().getOutgoing ());

    // Subset
    assertEquals (0, aReport.getSubtotalCount ());

    // Avoid bloating the logs
    if (false)
      LOGGER.info (new TransactionStatisticsReport101Marshaller ().setFormattedOutput (true).getAsString (aReport));

    // Ensure it is valid XML
    final Document aDoc = new TransactionStatisticsReport101Marshaller ().getAsDocument (aReport);
    assertNotNull (aDoc);

    // Perform Schematron verification
    final SchematronOutputType aSVRL = TransactionStatisticsReportValidator.getSchematronTSR_10 ()
                                                                           .applySchematronValidationToSVRL (aDoc,
                                                                                                             null);
    assertEquals (0, SVRLHelper.getAllFailedAssertionsAndSuccessfulReports (aSVRL).size ());
  }

  @Test
  public void testCreateForOneSendingTransmission () throws Exception
  {
    final OffsetDateTime aNow = PDTFactory.getCurrentOffsetDateTime ();
    final String sOtherSPID = "POP000002";
    final String sEndUserID = "abc";

    final PeppolReportingItem aItem = PeppolReportingItem.builder ()
                                                         .exchangeDateTime (aNow)
                                                         .directionSending ()
                                                         .c2ID (MY_SPID)
                                                         .c3ID (sOtherSPID)
                                                         .docTypeID (EPredefinedDocumentTypeIdentifier.INVOICE_EN16931_PEPPOL_V30)
                                                         .processID (EPredefinedProcessIdentifier.BIS3_BILLING)
                                                         .transportProtocolPeppolAS4v2 ()
                                                         .c1CountryCode ("FI")
                                                         .endUserID (sEndUserID)
                                                         .build ();

    // Create report with exactly one transaction
    final TransactionStatisticsReportType aReport = TransactionStatisticsReport.builder ()
                                                                               .monthOf (aNow)
                                                                               .reportingServiceProviderID (MY_SPID)
                                                                               .reportingItemList (new CommonsArrayList <> (aItem))
                                                                               .build ();

    // Check content
    assertEquals (CPeppolReporting.TSR_CUSTOMIZATION_ID_V10, aReport.getCustomizationIDValue ());
    assertEquals (CPeppolReporting.TSR_PROFILE_ID_V10, aReport.getProfileIDValue ());
    assertNotNull (aReport.getHeader ());

    assertNotNull (aReport.getHeader ().getReportPeriod ());
    assertEquals (XMLOffsetDate.of (aNow.toLocalDate ().withDayOfMonth (1)),
                  aReport.getHeader ().getReportPeriod ().getStartDate ());
    assertEquals (XMLOffsetDate.of (aNow.toLocalDate ().withDayOfMonth (1).plusMonths (1).minusDays (1)),
                  aReport.getHeader ().getReportPeriod ().getEndDate ());

    assertNotNull (aReport.getHeader ().getReporterID ());
    assertEquals (CPeppolReporting.SERVICE_PROVIDER_ID_SCHEME, aReport.getHeader ().getReporterID ().getSchemeID ());
    assertEquals (MY_SPID, aReport.getHeader ().getReporterIDValue ());

    // Full set
    assertNotNull (aReport.getTotal ());
    assertEquals (BigInteger.ZERO, aReport.getTotal ().getIncoming ());
    assertEquals (BigInteger.ONE, aReport.getTotal ().getOutgoing ());

    // Subsets
    assertEquals (2, aReport.getSubtotalCount ());
    assertEquals (1, aReport.getSubtotal ().stream ().filter (x -> x.getType ().equals (SubtotalKeyTP.TYPE)).count ());
    assertEquals (1,
                  aReport.getSubtotal ()
                         .stream ()
                         .filter (x -> x.getType ().equals (SubtotalKeySP_DT_PR.TYPE))
                         .count ());
    // Not present if only sending
    assertEquals (0,
                  aReport.getSubtotal ()
                         .stream ()
                         .filter (x -> x.getType ().equals (SubtotalKeySP_DT_PR_CC.TYPE))
                         .count ());

    // Avoid bloating the logs
    if (false)
      LOGGER.info (new TransactionStatisticsReport101Marshaller ().setFormattedOutput (true).getAsString (aReport));

    // Ensure it is valid XML
    final Document aDoc = new TransactionStatisticsReport101Marshaller ().getAsDocument (aReport);
    assertNotNull (aDoc);

    // Perform Schematron verification
    final SchematronOutputType aSVRL = TransactionStatisticsReportValidator.getSchematronTSR_10 ()
                                                                           .applySchematronValidationToSVRL (aDoc,
                                                                                                             null);
    assertEquals (0, SVRLHelper.getAllFailedAssertionsAndSuccessfulReports (aSVRL).size ());
  }

  @Test
  public void testCreateForOneReceivingTransmission () throws Exception
  {
    final OffsetDateTime aNow = PDTFactory.getCurrentOffsetDateTime ();
    final String sOtherSPID = "POP000002";
    final String sEndUserID = "abc";

    final PeppolReportingItem aItem = PeppolReportingItem.builder ()
                                                         .exchangeDateTime (aNow)
                                                         .directionReceiving ()
                                                         .c2ID (MY_SPID)
                                                         .c3ID (sOtherSPID)
                                                         .docTypeID (EPredefinedDocumentTypeIdentifier.INVOICE_EN16931_PEPPOL_V30)
                                                         .processID (EPredefinedProcessIdentifier.BIS3_BILLING)
                                                         .transportProtocolPeppolAS4v2 ()
                                                         .c1CountryCode ("FI")
                                                         .c4CountryCode ("DE")
                                                         .endUserID (sEndUserID)
                                                         .build ();

    // Create report with exactly one transaction
    final TransactionStatisticsReportType aReport = TransactionStatisticsReport.builder ()
                                                                               .monthOf (aNow)
                                                                               .reportingServiceProviderID (MY_SPID)
                                                                               .reportingItemList (new CommonsArrayList <> (aItem))
                                                                               .build ();

    // Check content
    assertEquals (CPeppolReporting.TSR_CUSTOMIZATION_ID_V10, aReport.getCustomizationIDValue ());
    assertEquals (CPeppolReporting.TSR_PROFILE_ID_V10, aReport.getProfileIDValue ());
    assertNotNull (aReport.getHeader ());

    assertNotNull (aReport.getHeader ().getReportPeriod ());
    assertEquals (XMLOffsetDate.of (aNow.toLocalDate ().withDayOfMonth (1)),
                  aReport.getHeader ().getReportPeriod ().getStartDate ());
    assertEquals (XMLOffsetDate.of (aNow.toLocalDate ().withDayOfMonth (1).plusMonths (1).minusDays (1)),
                  aReport.getHeader ().getReportPeriod ().getEndDate ());

    assertNotNull (aReport.getHeader ().getReporterID ());
    assertEquals (CPeppolReporting.SERVICE_PROVIDER_ID_SCHEME, aReport.getHeader ().getReporterID ().getSchemeID ());
    assertEquals (MY_SPID, aReport.getHeader ().getReporterIDValue ());

    // Full set
    assertNotNull (aReport.getTotal ());
    assertEquals (BigInteger.ONE, aReport.getTotal ().getIncoming ());
    assertEquals (BigInteger.ZERO, aReport.getTotal ().getOutgoing ());

    // Subsets
    assertEquals (3, aReport.getSubtotalCount ());
    assertEquals (1, aReport.getSubtotal ().stream ().filter (x -> x.getType ().equals (SubtotalKeyTP.TYPE)).count ());
    assertEquals (1,
                  aReport.getSubtotal ()
                         .stream ()
                         .filter (x -> x.getType ().equals (SubtotalKeySP_DT_PR.TYPE))
                         .count ());
    assertEquals (1,
                  aReport.getSubtotal ()
                         .stream ()
                         .filter (x -> x.getType ().equals (SubtotalKeySP_DT_PR_CC.TYPE))
                         .count ());

    // Avoid bloating the logs
    if (false)
      LOGGER.info (new TransactionStatisticsReport101Marshaller ().setFormattedOutput (true).getAsString (aReport));

    // Ensure it is valid XML
    final Document aDoc = new TransactionStatisticsReport101Marshaller ().getAsDocument (aReport);
    assertNotNull (aDoc);

    // Perform Schematron verification
    final SchematronOutputType aSVRL = TransactionStatisticsReportValidator.getSchematronTSR_10 ()
                                                                           .applySchematronValidationToSVRL (aDoc,
                                                                                                             null);
    assertEquals (0, SVRLHelper.getAllFailedAssertionsAndSuccessfulReports (aSVRL).size ());
  }

  @Test
  public void testCreateReport () throws Exception
  {
    final String sOtherSPID = "POP000002";
    final OffsetDateTime aNow = PDTFactory.getCurrentOffsetDateTime ();
    final String [] aEndUsers = { "a", "b", "c", "d", "e", "f", "g", "h", "i" };
    final IntFunction <String> fctGetEndUser = idx -> aEndUsers[idx % aEndUsers.length];

    final ICommonsList <PeppolReportingItem> aList = new CommonsArrayList <> ();
    for (int i = 0; i < 5; ++i)
      aList.add (PeppolReportingItem.builder ()
                                    .exchangeDateTime (aNow)
                                    .directionSending ()
                                    .c2ID (MY_SPID)
                                    .c3ID (sOtherSPID)
                                    .docTypeID (EPredefinedDocumentTypeIdentifier.INVOICE_EN16931_PEPPOL_V30)
                                    .processID (EPredefinedProcessIdentifier.BIS3_BILLING)
                                    .transportProtocolPeppolAS4v2 ()
                                    .c1CountryCode ("FI")
                                    .endUserID (fctGetEndUser.apply (i))
                                    .build ());
    for (int i = 0; i < 3; ++i)
      aList.add (PeppolReportingItem.builder ()
                                    .exchangeDateTime (aNow)
                                    .directionSending ()
                                    .c2ID (MY_SPID)
                                    .c3ID (sOtherSPID)
                                    .docTypeID (EPredefinedDocumentTypeIdentifier.INVOICE_EN16931_PEPPOL_V30)
                                    .processID (EPredefinedProcessIdentifier.BIS3_BILLING)
                                    .transportProtocolPeppolAS4v2 ()
                                    .c1CountryCode ("NO")
                                    .endUserID (fctGetEndUser.apply (i + 3))
                                    .build ());
    for (int i = 0; i < 4; ++i)
      aList.add (PeppolReportingItem.builder ()
                                    .exchangeDateTime (aNow)
                                    .directionSending ()
                                    .c2ID (MY_SPID)
                                    .c3ID (sOtherSPID)
                                    .docTypeID (EPredefinedDocumentTypeIdentifier.CREDITNOTE_EN16931_PEPPOL_V30)
                                    .processID (EPredefinedProcessIdentifier.BIS3_BILLING)
                                    .transportProtocolPeppolAS4v2 ()
                                    .c1CountryCode ("NO")
                                    .endUserID (fctGetEndUser.apply (i))
                                    .build ());
    for (int i = 0; i < 2; ++i)
      aList.add (PeppolReportingItem.builder ()
                                    .exchangeDateTime (aNow)
                                    .directionReceiving ()
                                    .c2ID (sOtherSPID)
                                    .c3ID (MY_SPID)
                                    .docTypeID (EPredefinedDocumentTypeIdentifier.CREDITNOTE_EN16931_PEPPOL_V30)
                                    .processID (EPredefinedProcessIdentifier.BIS3_BILLING)
                                    .transportProtocolPeppolAS4v2 ()
                                    .c1CountryCode ("NO")
                                    .c4CountryCode ("JP")
                                    .endUserID (fctGetEndUser.apply (i + 4))
                                    .build ());

    // Create report with many transactions
    final TransactionStatisticsReportType aReport = TransactionStatisticsReport.builder ()
                                                                               .monthOf (aNow)
                                                                               .reportingServiceProviderID (MY_SPID)
                                                                               .reportingItemList (aList)
                                                                               .build ();

    // Check content
    assertEquals (CPeppolReporting.TSR_CUSTOMIZATION_ID_V10, aReport.getCustomizationIDValue ());
    assertEquals (CPeppolReporting.TSR_PROFILE_ID_V10, aReport.getProfileIDValue ());
    assertNotNull (aReport.getHeader ());

    assertNotNull (aReport.getHeader ().getReportPeriod ());
    assertEquals (XMLOffsetDate.of (aNow.toLocalDate ().withDayOfMonth (1)),
                  aReport.getHeader ().getReportPeriod ().getStartDate ());
    assertEquals (XMLOffsetDate.of (aNow.toLocalDate ().withDayOfMonth (1).plusMonths (1).minusDays (1)),
                  aReport.getHeader ().getReportPeriod ().getEndDate ());

    assertNotNull (aReport.getHeader ().getReporterID ());
    assertEquals (CPeppolReporting.SERVICE_PROVIDER_ID_SCHEME, aReport.getHeader ().getReporterID ().getSchemeID ());
    assertEquals (MY_SPID, aReport.getHeader ().getReporterIDValue ());

    // Full set
    assertNotNull (aReport.getTotal ());
    assertEquals (2, aReport.getTotal ().getIncoming ().intValueExact ());
    assertEquals (12, aReport.getTotal ().getOutgoing ().intValueExact ());

    // 7 Subtotals
    assertEquals (4, aReport.getSubtotalCount ());

    // 1 Transport Protocol only
    assertEquals (1, aReport.getSubtotal ().stream ().filter (x -> x.getType ().equals (SubtotalKeyTP.TYPE)).count ());

    // 2 different subtotals
    assertEquals (2,
                  aReport.getSubtotal ()
                         .stream ()
                         .filter (x -> x.getType ().equals (SubtotalKeySP_DT_PR.TYPE))
                         .count ());

    // 1 different subtotal (only receivers)
    assertEquals (1,
                  aReport.getSubtotal ()
                         .stream ()
                         .filter (x -> x.getType ().equals (SubtotalKeySP_DT_PR_CC.TYPE))
                         .count ());

    // Avoid bloating the logs
    if (false)
      LOGGER.info (new TransactionStatisticsReport101Marshaller ().setFormattedOutput (true).getAsString (aReport));

    // Ensure it is valid XML
    final Document aDoc = new TransactionStatisticsReport101Marshaller ().getAsDocument (aReport);
    assertNotNull (aDoc);

    // Perform Schematron verification
    final SchematronOutputType aSVRL = TransactionStatisticsReportValidator.getSchematronTSR_10 ()
                                                                           .applySchematronValidationToSVRL (aDoc,
                                                                                                             null);
    assertEquals (0, SVRLHelper.getAllFailedAssertionsAndSuccessfulReports (aSVRL).size ());
  }
}
