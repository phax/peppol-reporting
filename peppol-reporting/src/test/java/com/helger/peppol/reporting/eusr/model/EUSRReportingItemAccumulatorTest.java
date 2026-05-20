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

  private static final class TestUser
  {
    final String endUserID;
    final String countryCode;

    TestUser (final String endUserID, final String countryCode)
    {
      this.endUserID = endUserID;
      this.countryCode = countryCode;
    }
  }

  private static final TestUser [] TEST_USERS = { new TestUser ("a", "AT"),
                                                  new TestUser ("b", "BE"),
                                                  new TestUser ("c", "CY"),
                                                  new TestUser ("d", "AT"),
                                                  new TestUser ("e", "BE"),
                                                  new TestUser ("f", "CY") };

  private static ICommonsList <PeppolReportingItem> buildFixtures ()
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

  private static EndUserStatisticsReportType _runViaList (final Iterable <? extends PeppolReportingItem> items)
  {
    final EndUserStatisticsReportType aReport = new EndUserStatisticsReportType ();
    EUSRReportingItemList.fillReportSubsets (items, aReport);
    return aReport;
  }

  private static EndUserStatisticsReportType _runViaAccumulator (final Iterable <? extends PeppolReportingItem> items)
  {
    final EndUserStatisticsReportType aReport = new EndUserStatisticsReportType ();
    final EUSRReportingItemAccumulator acc = new EUSRReportingItemAccumulator ();
    for (final PeppolReportingItem item : items)
      acc.accept (item);
    acc.fillReport (aReport);
    return aReport;
  }

  private static void assertReportsEqual (final EndUserStatisticsReportType expected,
                                          final EndUserStatisticsReportType actual)
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
    final ICommonsList <PeppolReportingItem> fixtures = buildFixtures ();
    assertReportsEqual (_runViaList (fixtures), _runViaAccumulator (fixtures));
  }

  @Test
  public void testMultiBatchMatchesList ()
  {
    final ICommonsList <PeppolReportingItem> fixtures = buildFixtures ();
    final int size = fixtures.size ();
    final int third = size / 3;

    final List <PeppolReportingItem> batch1 = fixtures.subList (0, third);
    final List <PeppolReportingItem> batch2 = fixtures.subList (third, 2 * third);
    final List <PeppolReportingItem> batch3 = fixtures.subList (2 * third, size);

    final EndUserStatisticsReportType expected = _runViaList (fixtures);

    final EndUserStatisticsReportType actual = new EndUserStatisticsReportType ();
    final EUSRReportingItemAccumulator acc = new EUSRReportingItemAccumulator ();
    for (final PeppolReportingItem item : batch1)
      acc.accept (item);
    for (final PeppolReportingItem item : batch2)
      acc.accept (item);
    for (final PeppolReportingItem item : batch3)
      acc.accept (item);
    acc.fillReport (actual);

    assertReportsEqual (expected, actual);
  }

  @Test
  public void testEmptyInput ()
  {
    final EndUserStatisticsReportType aReport = new EndUserStatisticsReportType ();
    final EUSRReportingItemAccumulator acc = new EUSRReportingItemAccumulator ();
    acc.fillReport (aReport);

    assertNotNull (aReport.getFullSet ());
    assertEquals (BigInteger.ZERO, aReport.getFullSet ().getSendingEndUsers ());
    assertEquals (BigInteger.ZERO, aReport.getFullSet ().getReceivingEndUsers ());
    assertEquals (BigInteger.ZERO, aReport.getFullSet ().getSendingOrReceivingEndUsers ());
    assertEquals (0, aReport.getSubsetCount ());
  }
}

