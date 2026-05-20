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
package com.helger.peppol.reporting.tsr.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.util.List;

import org.jspecify.annotations.NonNull;
import org.junit.Test;

import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.datetime.helper.PDTFactory;
import com.helger.peppol.reporting.api.PeppolReportingItem;
import com.helger.peppol.reporting.jaxb.tsr.v101.TransactionStatisticsReportType;
import com.helger.peppolid.peppol.doctype.EPredefinedDocumentTypeIdentifier;
import com.helger.peppolid.peppol.process.EPredefinedProcessIdentifier;

/**
 * Unit tests for {@link TSRReportingItemAccumulator}.
 */
public final class TSRReportingItemAccumulatorTest
{
  private static final String MY_SPID = "POP000360";
  private static final String OTHER_SPID = "POP000002";

  @NonNull
  private static ICommonsList <PeppolReportingItem> _buildFixtures ()
  {
    final OffsetDateTime aNow = PDTFactory.getCurrentOffsetDateTime ();
    final ICommonsList <PeppolReportingItem> aList = new CommonsArrayList <> ();

    // Sending items
    for (int i = 0; i < 5; i++)
    {
      aList.add (PeppolReportingItem.builder ()
                                    .exchangeDateTime (aNow)
                                    .directionSending ()
                                    .c2ID (MY_SPID)
                                    .c3ID (OTHER_SPID)
                                    .docTypeID (EPredefinedDocumentTypeIdentifier.INVOICE_EN16931_PEPPOL_V30)
                                    .processID (EPredefinedProcessIdentifier.BIS3_BILLING)
                                    .transportProtocolPeppolAS4v2 ()
                                    .c1CountryCode ("FI")
                                    .endUserID ("user" + i)
                                    .build ());
    }
    // Receiving items
    for (int i = 0; i < 4; i++)
    {
      aList.add (PeppolReportingItem.builder ()
                                    .exchangeDateTime (aNow)
                                    .directionReceiving ()
                                    .c2ID (OTHER_SPID)
                                    .c3ID (MY_SPID)
                                    .docTypeID (EPredefinedDocumentTypeIdentifier.CREDITNOTE_EN16931_PEPPOL_V30)
                                    .processID (EPredefinedProcessIdentifier.BIS3_BILLING)
                                    .transportProtocolPeppolAS4v2 ()
                                    .c1CountryCode ("DE")
                                    .c4CountryCode ("AT")
                                    .endUserID ("ruser" + i)
                                    .build ());
    }
    return aList;
  }

  @NonNull
  private static TransactionStatisticsReportType _runViaList (@NonNull final Iterable <? extends PeppolReportingItem> items)
  {
    final TransactionStatisticsReportType aReport = new TransactionStatisticsReportType ();
    TSRReportingItemList.fillReportSubsets (items, aReport);
    return aReport;
  }

  @NonNull
  private static TransactionStatisticsReportType _runViaAccumulator (@NonNull final Iterable <? extends PeppolReportingItem> aItems)
  {
    final TransactionStatisticsReportType aReport = new TransactionStatisticsReportType ();
    final TSRReportingItemAccumulator aAcc = new TSRReportingItemAccumulator ();
    for (final PeppolReportingItem aItem : aItems)
      aAcc.accept (aItem);
    aAcc.fillReport (aReport);
    return aReport;
  }

  private static void _assertReportsEqual (@NonNull final TransactionStatisticsReportType aExpected,
                                           @NonNull final TransactionStatisticsReportType aActual)
  {
    assertNotNull (aActual.getTotal ());
    assertEquals (aExpected.getTotal ().getIncoming (), aActual.getTotal ().getIncoming ());
    assertEquals (aExpected.getTotal ().getOutgoing (), aActual.getTotal ().getOutgoing ());
    assertEquals (aExpected.getSubtotalCount (), aActual.getSubtotalCount ());
  }

  @Test
  public void testAccumulatorMatchesList ()
  {
    final ICommonsList <PeppolReportingItem> aFixtures = _buildFixtures ();
    _assertReportsEqual (_runViaList (aFixtures), _runViaAccumulator (aFixtures));
  }

  @Test
  public void testMultiBatchMatchesList ()
  {
    final ICommonsList <PeppolReportingItem> aFixtures = _buildFixtures ();
    final int nSize = aFixtures.size ();
    final int nThird = nSize / 3;

    final List <PeppolReportingItem> aBatch1 = aFixtures.subList (0, nThird);
    final List <PeppolReportingItem> aBatch2 = aFixtures.subList (nThird, 2 * nThird);
    final List <PeppolReportingItem> aBatch3 = aFixtures.subList (2 * nThird, nSize);

    final TransactionStatisticsReportType aExpected = _runViaList (aFixtures);

    final TransactionStatisticsReportType aActual = new TransactionStatisticsReportType ();
    final TSRReportingItemAccumulator aAcc = new TSRReportingItemAccumulator ();
    for (final PeppolReportingItem item : aBatch1)
      aAcc.accept (item);
    for (final PeppolReportingItem item : aBatch2)
      aAcc.accept (item);
    for (final PeppolReportingItem item : aBatch3)
      aAcc.accept (item);
    aAcc.fillReport (aActual);

    _assertReportsEqual (aExpected, aActual);
  }

  @Test
  public void testEmptyInput ()
  {
    final TransactionStatisticsReportType aReport = new TransactionStatisticsReportType ();
    final TSRReportingItemAccumulator aAcc = new TSRReportingItemAccumulator ();
    aAcc.fillReport (aReport);

    assertNotNull (aReport.getTotal ());
    assertEquals (BigInteger.ZERO, aReport.getTotal ().getIncoming ());
    assertEquals (BigInteger.ZERO, aReport.getTotal ().getOutgoing ());
    assertEquals (0, aReport.getSubtotalCount ());
  }
}
