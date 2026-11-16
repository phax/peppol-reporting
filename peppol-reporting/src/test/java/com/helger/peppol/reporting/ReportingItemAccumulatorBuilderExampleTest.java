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
package com.helger.peppol.reporting;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;

import org.jspecify.annotations.NonNull;
import org.junit.Test;
import org.w3c.dom.Document;

import com.helger.base.functional.IThrowingConsumer;
import com.helger.base.state.ESuccess;
import com.helger.base.wrapper.Wrapper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.config.ConfigFactory;
import com.helger.peppol.reporting.api.PeppolReportingItem;
import com.helger.peppol.reporting.api.backend.IPeppolReportingBackendSPI;
import com.helger.peppol.reporting.api.backend.PeppolReportingBackend;
import com.helger.peppol.reporting.api.backend.PeppolReportingBackendException;
import com.helger.peppol.reporting.eusr.EndUserStatisticsReport;
import com.helger.peppol.reporting.eusr.EndUserStatisticsReportValidator;
import com.helger.peppol.reporting.eusr.model.EUSRReportingItemAccumulator;
import com.helger.peppol.reporting.jaxb.eusr.EndUserStatisticsReport110Marshaller;
import com.helger.peppol.reporting.jaxb.eusr.v110.EndUserStatisticsReportType;
import com.helger.peppol.reporting.jaxb.tsr.TransactionStatisticsReport101Marshaller;
import com.helger.peppol.reporting.jaxb.tsr.v101.TransactionStatisticsReportType;
import com.helger.peppol.reporting.tsr.TransactionStatisticsReport;
import com.helger.peppol.reporting.tsr.TransactionStatisticsReportValidator;
import com.helger.peppol.reporting.tsr.model.TSRReportingItemAccumulator;
import com.helger.peppolid.peppol.doctype.EPredefinedDocumentTypeIdentifier;
import com.helger.peppolid.peppol.process.EPredefinedProcessIdentifier;
import com.helger.schematron.svrl.SVRLHelper;
import com.helger.schematron.svrl.jaxb.SchematronOutputType;

/**
 * Example showing how to create EUSR and TSR reports without keeping all reporting items of the
 * reporting period in memory. The pattern is:
 * <ol>
 * <li>Create an empty accumulator ({@link EUSRReportingItemAccumulator} or
 * {@link TSRReportingItemAccumulator})</li>
 * <li>Feed all reporting items of the reporting period into it, one by one. Here this is done via
 * {@link IPeppolReportingBackendSPI#forEachReportingItem(YearMonth, java.util.function.Consumer)}
 * of the in-memory backend, but any other source works the same way.</li>
 * <li>Pass the filled accumulator to the report builder via <code>reportingItemAccumulator</code>
 * and build the report as usual</li>
 * </ol>
 * At no point are all reporting items of the reporting period kept in a list - the test items are
 * generated one by one as well. The resulting reports are identical to the ones created directly
 * from the generated items.
 *
 * @author Philip Helger
 */
public final class ReportingItemAccumulatorBuilderExampleTest
{
  private static final String MY_SPID = "PDE000001";
  private static final String OTHER_SPID = "POP000002";
  private static final YearMonth REPORTING_PERIOD = YearMonth.of (2026, 10);
  private static final String [] END_USER_IDS = { "a", "b", "c", "d", "e" };
  private static final String [] COUNTRY_CODES = { "AT", "BE", "CY", "DE", "FI" };
  private static final int ITEM_COUNT = 200;

  /**
   * Create reporting items spread over all days of {@link #REPORTING_PERIOD} and pass each of them
   * to the provided consumer, without keeping them in a list. Every third item is a received one,
   * all others are sent ones.
   *
   * @param aConsumer
   *        The consumer to be invoked for each created item. May not be <code>null</code>.
   * @throws PeppolReportingBackendException
   *         If the consumer throws it
   */
  private static void _forEachReportingItem (@NonNull final IThrowingConsumer <? super PeppolReportingItem, PeppolReportingBackendException> aConsumer) throws PeppolReportingBackendException
  {
    for (int i = 0; i < ITEM_COUNT; ++i)
    {
      final OffsetDateTime aExchangeDT = REPORTING_PERIOD.atDay (1 + i % REPORTING_PERIOD.lengthOfMonth ())
                                                         .atTime (10, i % 60)
                                                         .atOffset (ZoneOffset.UTC);
      final int nEndUser = i % END_USER_IDS.length;
      final PeppolReportingItem.Builder aBuilder = PeppolReportingItem.builder ()
                                                                      .exchangeDateTime (aExchangeDT)
                                                                      .docTypeID (i % 2 == 0 ? EPredefinedDocumentTypeIdentifier.INVOICE_EN16931_PEPPOL_V30
                                                                                             : EPredefinedDocumentTypeIdentifier.CREDITNOTE_EN16931_PEPPOL_V30)
                                                                      .processID (EPredefinedProcessIdentifier.BIS3_BILLING)
                                                                      .transportProtocolPeppolAS4v2 ()
                                                                      .c1CountryCode (COUNTRY_CODES[nEndUser])
                                                                      .endUserID (END_USER_IDS[nEndUser]);
      if (i % 3 == 0)
        aBuilder.directionReceiving ()
                .c2ID (OTHER_SPID)
                .c3ID (MY_SPID)
                .c4CountryCode (COUNTRY_CODES[(nEndUser + 1) % COUNTRY_CODES.length]);
      else
        aBuilder.directionSending ().c2ID (MY_SPID).c3ID (OTHER_SPID);
      aConsumer.accept (aBuilder.build ());
    }
  }

  @Test
  public void testEUSRWithAccumulator () throws Exception
  {
    final Wrapper <EndUserStatisticsReportType> aReportWrapper = new Wrapper <> ();
    final ESuccess eSuccess = PeppolReportingBackend.withBackendDo (ConfigFactory.getDefaultConfig (), aBackend -> {
      // In a real application, each item is stored when a message is sent or
      // received
      _forEachReportingItem (aBackend::storeReportingItem);

      // 1. Create an empty accumulator
      final EUSRReportingItemAccumulator aAccumulator = new EUSRReportingItemAccumulator ();

      // 2. Feed all items of the reporting period into it, one by one
      aBackend.forEachReportingItem (REPORTING_PERIOD, aAccumulator::accept);

      // 3. Pass the filled accumulator to the builder
      aReportWrapper.set (EndUserStatisticsReport.builder ()
                                                 .monthOf (REPORTING_PERIOD)
                                                 .reportingServiceProviderID (MY_SPID)
                                                 .reportingItemAccumulator (aAccumulator)
                                                 .build ());
    });
    assertTrue (eSuccess.isSuccess ());

    final EndUserStatisticsReportType aReport = aReportWrapper.get ();
    assertNotNull (aReport);
    assertEquals (BigInteger.valueOf (END_USER_IDS.length), aReport.getFullSet ().getSendingOrReceivingEndUsers ());

    // The report is identical to the one created directly from the generated
    // items, without the backend
    final EUSRReportingItemAccumulator aExpectedAccumulator = new EUSRReportingItemAccumulator ();
    _forEachReportingItem (aExpectedAccumulator::accept);
    final EndUserStatisticsReportType aExpectedReport = EndUserStatisticsReport.builder ()
                                                                               .monthOf (REPORTING_PERIOD)
                                                                               .reportingServiceProviderID (MY_SPID)
                                                                               .reportingItemAccumulator (aExpectedAccumulator)
                                                                               .build ();
    final EndUserStatisticsReport110Marshaller aMarshaller = new EndUserStatisticsReport110Marshaller ();
    assertEquals (aMarshaller.getAsString (aExpectedReport), aMarshaller.getAsString (aReport));

    // And it is valid
    final Document aDoc = aMarshaller.getAsDocument (aReport);
    assertNotNull (aDoc);
    final SchematronOutputType aSVRL = EndUserStatisticsReportValidator.getSchematronEUSR_11 ()
                                                                       .applySchematronValidationToSVRL (aDoc, null);
    assertEquals (0, SVRLHelper.getAllFailedAssertionsAndSuccessfulReports (aSVRL).size ());
  }

  @Test
  public void testTSRWithAccumulator () throws Exception
  {
    final Wrapper <TransactionStatisticsReportType> aReportWrapper = new Wrapper <> ();
    final ESuccess eSuccess = PeppolReportingBackend.withBackendDo (ConfigFactory.getDefaultConfig (), aBackend -> {
      // In a real application, each item is stored when a message is sent or
      // received
      _forEachReportingItem (aBackend::storeReportingItem);

      // 1. Create an empty accumulator
      final TSRReportingItemAccumulator aAccumulator = new TSRReportingItemAccumulator ();

      // 2. Feed all items of the reporting period into it, one by one
      aBackend.forEachReportingItem (REPORTING_PERIOD, aAccumulator::accept);

      // 3. Pass the filled accumulator to the builder
      aReportWrapper.set (TransactionStatisticsReport.builder ()
                                                     .monthOf (REPORTING_PERIOD)
                                                     .reportingServiceProviderID (MY_SPID)
                                                     .reportingItemAccumulator (aAccumulator)
                                                     .build ());
    });
    assertTrue (eSuccess.isSuccess ());

    final TransactionStatisticsReportType aReport = aReportWrapper.get ();
    assertNotNull (aReport);
    assertEquals (BigInteger.valueOf (ITEM_COUNT),
                  aReport.getTotal ().getIncoming ().add (aReport.getTotal ().getOutgoing ()));

    // The report is identical to the one created directly from the generated
    // items, without the backend
    final TSRReportingItemAccumulator aExpectedAccumulator = new TSRReportingItemAccumulator ();
    _forEachReportingItem (aExpectedAccumulator::accept);
    final TransactionStatisticsReportType aExpectedReport = TransactionStatisticsReport.builder ()
                                                                                       .monthOf (REPORTING_PERIOD)
                                                                                       .reportingServiceProviderID (MY_SPID)
                                                                                       .reportingItemAccumulator (aExpectedAccumulator)
                                                                                       .build ();
    final TransactionStatisticsReport101Marshaller aMarshaller = new TransactionStatisticsReport101Marshaller ();
    assertEquals (aMarshaller.getAsString (aExpectedReport), aMarshaller.getAsString (aReport));

    // And it is valid
    final Document aDoc = aMarshaller.getAsDocument (aReport);
    assertNotNull (aDoc);
    final SchematronOutputType aSVRL = TransactionStatisticsReportValidator.getSchematronTSR_10 ()
                                                                           .applySchematronValidationToSVRL (aDoc,
                                                                                                             null);
    assertEquals (0, SVRLHelper.getAllFailedAssertionsAndSuccessfulReports (aSVRL).size ());
  }

  @Test
  public void testLastItemSourceWins () throws PeppolReportingBackendException
  {
    // This test is about reportingItemList itself, so a list is needed here
    final ICommonsList <PeppolReportingItem> aItems = new CommonsArrayList <> ();
    _forEachReportingItem (aItems::add);
    final EndUserStatisticsReport.Builder11 aBuilder = EndUserStatisticsReport.builder ()
                                                                              .monthOf (REPORTING_PERIOD)
                                                                              .reportingServiceProviderID (MY_SPID);

    // Neither an item list nor an accumulator is set
    assertFalse (aBuilder.isComplete (false));

    // The empty accumulator replaces the item list
    aBuilder.reportingItemList (aItems).reportingItemAccumulator (new EUSRReportingItemAccumulator ());
    assertTrue (aBuilder.isComplete (false));
    assertEquals (BigInteger.ZERO, aBuilder.build ().getFullSet ().getSendingOrReceivingEndUsers ());

    // The item list replaces the accumulator
    aBuilder.reportingItemList (aItems);
    assertEquals (BigInteger.valueOf (END_USER_IDS.length),
                  aBuilder.build ().getFullSet ().getSendingOrReceivingEndUsers ());
  }
}
