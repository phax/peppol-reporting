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
package com.helper.peppol.reporting.eusr.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.math.BigInteger;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.function.IntFunction;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.datetime.XMLOffsetDate;
import com.helger.peppol.reporting.jaxb.eusr.v110.EndUserStatisticsReportType;
import com.helger.peppolid.peppol.doctype.EPredefinedDocumentTypeIdentifier;
import com.helger.peppolid.peppol.process.EPredefinedProcessIdentifier;
import com.helger.schematron.svrl.SVRLHelper;
import com.helger.schematron.svrl.jaxb.SchematronOutputType;
import com.helper.peppol.reporting.api.CPeppolReporting;
import com.helper.peppol.reporting.api.EReportingDirection;
import com.helper.peppol.reporting.api.PeppolReportingItem;
import com.helper.peppol.reporting.eusr.EndUserStatisticsReport;
import com.helper.peppol.reporting.eusr.EndUserStatisticsReport110Marshaller;
import com.helper.peppol.reporting.eusr.EndUserStatisticsReportValidator;

public final class FuncTestCreateEUSR
{
  private static final Logger LOGGER = LoggerFactory.getLogger (FuncTestCreateEUSR.class);
  private static final String MY_SPID = "PDE000001";

  @Test
  public void testCreateEmpty () throws Exception
  {
    final OffsetDateTime aNow = PDTFactory.getCurrentOffsetDateTime ();

    // Create report without any transaction
    final EndUserStatisticsReportType aReport = EndUserStatisticsReport.builder ()
                                                                       .monthOf (aNow)
                                                                       .reportingServiceProviderID (MY_SPID)
                                                                       .reportingItemList (new CommonsArrayList <> ())
                                                                       .build ();

    // Check content
    assertEquals (CPeppolReporting.EUSR_CUSTOMIZATION_ID_V11, aReport.getCustomizationIDValue ());
    assertEquals (CPeppolReporting.EUSR_PROFILE_ID_V10, aReport.getProfileIDValue ());
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
    assertNotNull (aReport.getFullSet ());
    assertEquals (BigInteger.ZERO, aReport.getFullSet ().getSendingEndUsers ());
    assertEquals (BigInteger.ZERO, aReport.getFullSet ().getReceivingEndUsers ());
    assertEquals (BigInteger.ZERO, aReport.getFullSet ().getSendingOrReceivingEndUsers ());

    // Subset
    assertEquals (0, aReport.getSubsetCount ());

    // Avoid bloating the logs
    if (false)
      LOGGER.info (new EndUserStatisticsReport110Marshaller ().setFormattedOutput (true).getAsString (aReport));

    // Ensure it is valid XML
    final Document aDoc = new EndUserStatisticsReport110Marshaller ().getAsDocument (aReport);
    assertNotNull (aDoc);

    // Perform Schematron verification
    final SchematronOutputType aSVRL = EndUserStatisticsReportValidator.getSchematronEUSR_11 ()
                                                                       .applySchematronValidationToSVRL (aDoc, null);
    assertEquals (0, SVRLHelper.getAllFailedAssertionsAndSuccessfulReports (aSVRL).size ());
  }

  @Test
  public void testCreateForOneTransmission () throws Exception
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
    final EndUserStatisticsReportType aReport = EndUserStatisticsReport.builder ()
                                                                       .monthOf (aNow)
                                                                       .reportingServiceProviderID (MY_SPID)
                                                                       .reportingItemList (new CommonsArrayList <> (aItem))
                                                                       .build ();

    // Check content
    assertEquals (CPeppolReporting.EUSR_CUSTOMIZATION_ID_V11, aReport.getCustomizationIDValue ());
    assertEquals (CPeppolReporting.EUSR_PROFILE_ID_V10, aReport.getProfileIDValue ());
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
    assertNotNull (aReport.getFullSet ());
    assertEquals (BigInteger.ONE, aReport.getFullSet ().getSendingEndUsers ());
    assertEquals (BigInteger.ZERO, aReport.getFullSet ().getReceivingEndUsers ());
    assertEquals (BigInteger.ONE, aReport.getFullSet ().getSendingOrReceivingEndUsers ());

    // Subsets
    assertEquals (4, aReport.getSubsetCount ());
    assertEquals (1, aReport.getSubset ().stream ().filter (x -> x.getType ().equals (SubsetKeyEUC.TYPE)).count ());
    assertEquals (1, aReport.getSubset ().stream ().filter (x -> x.getType ().equals (SubsetKeyDT_EUC.TYPE)).count ());
    assertEquals (1,
                  aReport.getSubset ().stream ().filter (x -> x.getType ().equals (SubsetKeyDT_PR_EUC.TYPE)).count ());
    assertEquals (1, aReport.getSubset ().stream ().filter (x -> x.getType ().equals (SubsetKeyDT_PR.TYPE)).count ());

    // Avoid bloating the logs
    if (false)
      LOGGER.info (new EndUserStatisticsReport110Marshaller ().setFormattedOutput (true).getAsString (aReport));

    // Ensure it is valid XML
    final Document aDoc = new EndUserStatisticsReport110Marshaller ().getAsDocument (aReport);
    assertNotNull (aDoc);

    // Perform Schematron verification
    final SchematronOutputType aSVRL = EndUserStatisticsReportValidator.getSchematronEUSR_11 ()
                                                                       .applySchematronValidationToSVRL (aDoc, null);
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
    final EndUserStatisticsReportType aReport = EndUserStatisticsReport.builder ()
                                                                       .monthOf (aNow)
                                                                       .reportingServiceProviderID (MY_SPID)
                                                                       .reportingItemList (aList)
                                                                       .build ();

    // Check content
    assertEquals (CPeppolReporting.EUSR_CUSTOMIZATION_ID_V11, aReport.getCustomizationIDValue ());
    assertEquals (CPeppolReporting.EUSR_PROFILE_ID_V10, aReport.getProfileIDValue ());
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
    assertNotNull (aReport.getFullSet ());
    assertEquals (6, aReport.getFullSet ().getSendingEndUsers ().intValueExact ());
    assertEquals (2, aReport.getFullSet ().getReceivingEndUsers ().intValueExact ());
    assertEquals (6, aReport.getFullSet ().getSendingOrReceivingEndUsers ().intValueExact ());

    // Avoid bloating the logs
    if (false)
      LOGGER.info (new EndUserStatisticsReport110Marshaller ().setFormattedOutput (true).getAsString (aReport));

    // Ensure it is valid XML
    final Document aDoc = new EndUserStatisticsReport110Marshaller ().getAsDocument (aReport);
    assertNotNull (aDoc);

    // Perform Schematron verification
    final SchematronOutputType aSVRL = EndUserStatisticsReportValidator.getSchematronEUSR_11 ()
                                                                       .applySchematronValidationToSVRL (aDoc, null);
    assertEquals (0, SVRLHelper.getAllFailedAssertionsAndSuccessfulReports (aSVRL).size ());
  }

  @Test
  public void testCreateSpecAppendix1 () throws Exception
  {
    final OffsetDateTime aNow = PDTFactory.createLocalDate (2023, Month.DECEMBER, 1)
                                          .atStartOfDay ()
                                          .atOffset (ZoneOffset.UTC);
    final String sOtherSPID1 = "POP000001";
    final String [] aEndUsers = { "a", "b", "c", "d", "e", "f", "g", "h", "i" };
    final IntFunction <String> fctGetEndSendingUser = idx -> aEndUsers[idx % aEndUsers.length];
    final IntFunction <String> fctGetEndReceivingUser = idx -> aEndUsers[idx % (aEndUsers.length / 2)];

    final ICommonsList <PeppolReportingItem> aItems = new CommonsArrayList <> ();
    for (int i = 0; i < 40; ++i)
    {
      final int nCut = 18;
      aItems.add (PeppolReportingItem.builder ()
                                     .exchangeDateTime (aNow)
                                     .direction (i < nCut ? EReportingDirection.SENDING : EReportingDirection.RECEIVING)
                                     .c2ID (i < nCut ? MY_SPID : sOtherSPID1)
                                     .c3ID (i < nCut ? sOtherSPID1 : MY_SPID)
                                     .docTypeID (EPredefinedDocumentTypeIdentifier.INVOICE_EN16931_PEPPOL_V30)
                                     .processID (EPredefinedProcessIdentifier.BIS3_BILLING)
                                     .transportProtocolPeppolAS4v2 ()
                                     .c1CountryCode ("DE")
                                     .c4CountryCode (i < nCut ? null : "DE")
                                     .endUserID ((i < nCut ? fctGetEndSendingUser : fctGetEndReceivingUser).apply (i))
                                     .build ());
    }
    for (int i = 0; i < 2; ++i)
    {
      aItems.add (PeppolReportingItem.builder ()
                                     .exchangeDateTime (aNow)
                                     .directionReceiving ()
                                     .c2ID (sOtherSPID1)
                                     .c3ID (MY_SPID)
                                     .docTypeID (EPredefinedDocumentTypeIdentifier.CREDITNOTE_EN16931_PEPPOL_V30)
                                     .processID (EPredefinedProcessIdentifier.BIS3_BILLING)
                                     .transportProtocolPeppolAS4v2 ()
                                     .c1CountryCode ("BE")
                                     .c4CountryCode ("DE")
                                     .endUserID (fctGetEndReceivingUser.apply (i))
                                     .build ());
    }
    for (int i = 0; i < 13; ++i)
    {
      aItems.add (PeppolReportingItem.builder ()
                                     .exchangeDateTime (aNow)
                                     .directionSending ()
                                     .c2ID (MY_SPID)
                                     .c3ID (sOtherSPID1)
                                     .docTypeID (EPredefinedDocumentTypeIdentifier.ORDER_FDC_PEPPOL_EU_POACC_TRNS_ORDER_3)
                                     .processID (EPredefinedProcessIdentifier.BIS3_ORDERING)
                                     .transportProtocolPeppolAS4v2 ()
                                     .c1CountryCode ("NO")
                                     .endUserID (fctGetEndSendingUser.apply (i))
                                     .build ());
    }
    for (int i = 0; i < 11; ++i)
    {
      aItems.add (PeppolReportingItem.builder ()
                                     .exchangeDateTime (aNow)
                                     .directionReceiving ()
                                     .c2ID (sOtherSPID1)
                                     .c3ID (MY_SPID)
                                     .docTypeID (EPredefinedDocumentTypeIdentifier.ORDERRESPONSE_FDC_PEPPOL_EU_POACC_TRNS_ORDER_RESPONSE_3)
                                     .processID (EPredefinedProcessIdentifier.BIS3_ORDERING)
                                     .transportProtocolPeppolAS4v2 ()
                                     .c1CountryCode ("PT")
                                     .c4CountryCode ("NO")
                                     .endUserID (fctGetEndReceivingUser.apply (i))
                                     .build ());
    }

    // Create report with exactly one transaction
    final EndUserStatisticsReportType aReport = EndUserStatisticsReport.builder ()
                                                                       .monthOf (aNow)
                                                                       .reportingServiceProviderID (MY_SPID)
                                                                       .reportingItemList (aItems)
                                                                       .build ();

    // Avoid bloating the logs
    if (false)
      LOGGER.info (new EndUserStatisticsReport110Marshaller ().setFormattedOutput (true).getAsString (aReport));

    // Ensure it is valid XML
    final Document aDoc = new EndUserStatisticsReport110Marshaller ().getAsDocument (aReport);
    assertNotNull (aDoc);

    // Perform Schematron verification
    final SchematronOutputType aSVRL = EndUserStatisticsReportValidator.getSchematronEUSR_11 ()
                                                                       .applySchematronValidationToSVRL (aDoc, null);
    assertEquals (0, SVRLHelper.getAllFailedAssertionsAndSuccessfulReports (aSVRL).size ());
  }

  @Test
  public void testCreateSpecAppendix2_SendingOnly () throws Exception
  {
    final OffsetDateTime aNow = PDTFactory.createLocalDate (2023, Month.DECEMBER, 1)
                                          .atStartOfDay ()
                                          .atOffset (ZoneOffset.UTC);
    final String sOtherSPID1 = "POP000001";
    final String [] aEndUsers = { "a", "b", "c", "d", "e", "f", "g", "h", "i" };
    final IntFunction <String> fctGetEndSendingUser = idx -> aEndUsers[idx % aEndUsers.length];

    final ICommonsList <PeppolReportingItem> aItems = new CommonsArrayList <> ();
    for (int i = 0; i < 18; ++i)
    {
      aItems.add (PeppolReportingItem.builder ()
                                     .exchangeDateTime (aNow)
                                     .directionSending ()
                                     .c2ID (MY_SPID)
                                     .c3ID (sOtherSPID1)
                                     .docTypeID (EPredefinedDocumentTypeIdentifier.INVOICE_EN16931_PEPPOL_V30)
                                     .processID (EPredefinedProcessIdentifier.BIS3_BILLING)
                                     .transportProtocolPeppolAS4v2 ()
                                     .c1CountryCode ("DE")
                                     .endUserID (fctGetEndSendingUser.apply (i))
                                     .build ());
    }
    for (int i = 0; i < 3; ++i)
    {
      aItems.add (PeppolReportingItem.builder ()
                                     .exchangeDateTime (aNow)
                                     .directionSending ()
                                     .c2ID (MY_SPID)
                                     .c3ID (sOtherSPID1)
                                     .docTypeID (EPredefinedDocumentTypeIdentifier.INVOICE_EN16931_PEPPOL_V30)
                                     .processID (EPredefinedProcessIdentifier.BIS3_BILLING)
                                     .transportProtocolPeppolAS4v2 ()
                                     .c1CountryCode ("NO")
                                     .endUserID (fctGetEndSendingUser.apply (i))
                                     .build ());
    }
    for (int i = 0; i < 5; ++i)
    {
      aItems.add (PeppolReportingItem.builder ()
                                     .exchangeDateTime (aNow)
                                     .directionSending ()
                                     .c2ID (MY_SPID)
                                     .c3ID (sOtherSPID1)
                                     .docTypeID (EPredefinedDocumentTypeIdentifier.ORDER_FDC_PEPPOL_EU_POACC_TRNS_ORDER_3)
                                     .processID (EPredefinedProcessIdentifier.BIS3_ORDERING)
                                     .transportProtocolPeppolAS4v2 ()
                                     .c1CountryCode ("NO")
                                     .endUserID (fctGetEndSendingUser.apply (i))
                                     .build ());
    }

    // Create report with exactly one transaction
    final EndUserStatisticsReportType aReport = EndUserStatisticsReport.builder ()
                                                                       .monthOf (aNow)
                                                                       .reportingServiceProviderID (MY_SPID)
                                                                       .reportingItemList (aItems)
                                                                       .build ();

    // Avoid bloating the logs
    if (false)
      LOGGER.info (new EndUserStatisticsReport110Marshaller ().setFormattedOutput (true).getAsString (aReport));

    // Ensure it is valid XML
    final Document aDoc = new EndUserStatisticsReport110Marshaller ().getAsDocument (aReport);
    assertNotNull (aDoc);

    // Perform Schematron verification
    final SchematronOutputType aSVRL = EndUserStatisticsReportValidator.getSchematronEUSR_11 ()
                                                                       .applySchematronValidationToSVRL (aDoc, null);
    assertEquals (0, SVRLHelper.getAllFailedAssertionsAndSuccessfulReports (aSVRL).size ());
  }
}
