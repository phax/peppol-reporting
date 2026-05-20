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
package com.helger.peppol.reporting.eusr.model;

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
import com.helger.peppol.reporting.jaxb.eusr.v110.EndUserStatisticsReportType;
import com.helger.peppolid.peppol.doctype.EPredefinedDocumentTypeIdentifier;
import com.helger.peppolid.peppol.process.EPredefinedProcessIdentifier;

/**
 * Unit tests for {@link EUSRReportingItemAccumulator}.
 */
public final class EUSRReportingItemAccumulatorTest
{
  private static final String MY_SPID = "PDE000001";
  private static final String OTHER_SPID = "POP000002";

  private static final record TestUser (String endUserID, String countryCode)
  {}

  private static final TestUser [] TEST_USERS = { new TestUser ("a", "AT"),
                                                  new TestUser ("b", "BE"),
                                                  new TestUser ("c", "CY"),
                                                  new TestUser ("d", "AT"),
                                                  new TestUser ("e", "BE"),
                                                  new TestUser ("f", "CY") };

  @NonNull
  private static ICommonsList <PeppolReportingItem> _buildFixtures ()
  {
    final OffsetDateTime aNow = PDTFactory.getCurrentOffsetDateTime ();
    final ICommonsList <PeppolReportingItem> aList = new CommonsArrayList <> ();

    // Sending items
    for (int i = 0; i < 5; i++)
    {
      final TestUser u = TEST_USERS[i % TEST_USERS.length];
      aList.add (PeppolReportingItem.builder ()
                                    .exchangeDateTime (aNow)
                                    .directionSending ()
                                    .c2ID (MY_SPID)
                                    .c3ID (OTHER_SPID)
                                    .docTypeID (EPredefinedDocumentTypeIdentifier.INVOICE_EN16931_PEPPOL_V30)
                                    .processID (EPredefinedProcessIdentifier.BIS3_BILLING)
                                    .transportProtocolPeppolAS4v2 ()
                                    .c1CountryCode (u.countryCode)
                                    .endUserID (u.endUserID)
                                    .build ());
    }
    // Receiving items
    for (int i = 0; i < 3; i++)
    {
      final TestUser u = TEST_USERS[i % TEST_USERS.length];
      aList.add (PeppolReportingItem.builder ()
                                    .exchangeDateTime (aNow)
                                    .directionReceiving ()
                                    .c2ID (OTHER_SPID)
                                    .c3ID (MY_SPID)
                                    .docTypeID (EPredefinedDocumentTypeIdentifier.CREDITNOTE_EN16931_PEPPOL_V30)
                                    .processID (EPredefinedProcessIdentifier.BIS3_BILLING)
                                    .transportProtocolPeppolAS4v2 ()
                                    .c1CountryCode ("NO")
                                    .c4CountryCode (u.countryCode)
                                    .endUserID (u.endUserID)
                                    .build ());
    }
    return aList;
  }

  @NonNull
  private static EndUserStatisticsReportType _runViaList (@NonNull final Iterable <? extends PeppolReportingItem> items)
  {
    final EndUserStatisticsReportType aReport = new EndUserStatisticsReportType ();
    EUSRReportingItemList.fillReportSubsets (items, aReport);
    return aReport;
  }

  @NonNull
  private static EndUserStatisticsReportType _runViaAccumulator (@NonNull final Iterable <? extends PeppolReportingItem> items)
  {
    final EndUserStatisticsReportType aReport = new EndUserStatisticsReportType ();
    final EUSRReportingItemAccumulator acc = new EUSRReportingItemAccumulator ();
    for (final PeppolReportingItem item : items)
      acc.accept (item);
    acc.fillReport (aReport);
    return aReport;
  }

  private static void _assertReportsEqual (@NonNull final EndUserStatisticsReportType expected,
                                           @NonNull final EndUserStatisticsReportType actual)
  {
    assertNotNull (actual.getFullSet ());
    assertEquals (expected.getFullSet ().getSendingEndUsers (), actual.getFullSet ().getSendingEndUsers ());
    assertEquals (expected.getFullSet ().getReceivingEndUsers (), actual.getFullSet ().getReceivingEndUsers ());
    assertEquals (expected.getFullSet ().getSendingOrReceivingEndUsers (),
                  actual.getFullSet ().getSendingOrReceivingEndUsers ());
    assertEquals (expected.getSubsetCount (), actual.getSubsetCount ());
  }

  @Test
  public void testAccumulatorMatchesList ()
  {
    final ICommonsList <PeppolReportingItem> fixtures = _buildFixtures ();
    _assertReportsEqual (_runViaList (fixtures), _runViaAccumulator (fixtures));
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

    final EndUserStatisticsReportType eAxpected = _runViaList (aFixtures);

    final EndUserStatisticsReportType aActual = new EndUserStatisticsReportType ();
    final EUSRReportingItemAccumulator aAcc = new EUSRReportingItemAccumulator ();
    for (final PeppolReportingItem item : aBatch1)
      aAcc.accept (item);
    for (final PeppolReportingItem item : aBatch2)
      aAcc.accept (item);
    for (final PeppolReportingItem item : aBatch3)
      aAcc.accept (item);
    aAcc.fillReport (aActual);

    _assertReportsEqual (eAxpected, aActual);
  }

  @Test
  public void testEmptyInput ()
  {
    final EndUserStatisticsReportType aReport = new EndUserStatisticsReportType ();
    final EUSRReportingItemAccumulator aAcc = new EUSRReportingItemAccumulator ();
    aAcc.fillReport (aReport);

    assertNotNull (aReport.getFullSet ());
    assertEquals (BigInteger.ZERO, aReport.getFullSet ().getSendingEndUsers ());
    assertEquals (BigInteger.ZERO, aReport.getFullSet ().getReceivingEndUsers ());
    assertEquals (BigInteger.ZERO, aReport.getFullSet ().getSendingOrReceivingEndUsers ());
    assertEquals (0, aReport.getSubsetCount ());
  }
}
