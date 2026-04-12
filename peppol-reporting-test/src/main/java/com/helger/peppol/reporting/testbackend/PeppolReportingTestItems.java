/*
 * Copyright (C) 2026 Philip Helger
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
package com.helger.peppol.reporting.testbackend;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.concurrent.Immutable;
import com.helger.peppol.reporting.api.EReportingDirection;
import com.helger.peppol.reporting.api.PeppolReportingItem;
import com.helger.peppolid.peppol.doctype.EPredefinedDocumentTypeIdentifier;
import com.helger.peppolid.peppol.process.EPredefinedProcessIdentifier;

/**
 * Builders for deterministic {@link PeppolReportingItem} instances used by the shared backend SPI
 * contract test suite. Determinism (no random fields, no "now" timestamps) is required so that
 * set-equality assertions over stored vs. retrieved items are stable.
 *
 * @author Philip Helger
 */
@Immutable
public final class PeppolReportingTestItems
{
  private PeppolReportingTestItems ()
  {}

  @NonNull
  private static PeppolReportingItem _build (@NonNull final LocalDate aDate,
                                             final int nIndex,
                                             @NonNull final EReportingDirection eDirection)
  {
    // Deterministic time based on the index, modulo to stay within a day.
    // Using seconds keeps a wide range of distinct values while remaining
    // human-readable in logs.
    final int nSecondOfDay = nIndex % (24 * 60 * 60);
    final OffsetDateTime aDT = OffsetDateTime.of (LocalDateTime.of (aDate, LocalTime.ofSecondOfDay (nSecondOfDay)),
                                                  ZoneOffset.UTC);

    return PeppolReportingItem.builder ()
                              .exchangeDateTime (aDT)
                              .direction (eDirection)
                              .c2ID ("pop000001")
                              .c3ID ("pop000002")
                              .docTypeID (EPredefinedDocumentTypeIdentifier.INVOICE_EN16931_PEPPOL_V30)
                              .processID (EPredefinedProcessIdentifier.BIS3_BILLING)
                              .transportProtocolPeppolAS4v2 ()
                              .c1CountryCode ("FI")
                              .c4CountryCode (eDirection.isSending () ? null : "DE")
                              .endUserID ("eu" + nIndex)
                              .build ();
  }

  /**
   * Build a {@link PeppolReportingItem} that uses a document type the SPI implementations must
   * filter out (TSR doctype). Used to verify that <code>storeReportingItem</code> silently drops
   * non-eligible items.
   *
   * @param aDate
   *        The exchange date. Must not be <code>null</code>.
   * @return A non-<code>null</code> non-eligible {@link PeppolReportingItem}.
   */
  @NonNull
  public static PeppolReportingItem nonEligibleItem (@NonNull final LocalDate aDate)
  {
    final OffsetDateTime aDT = OffsetDateTime.of (LocalDateTime.of (aDate, LocalTime.NOON), ZoneOffset.UTC);
    return PeppolReportingItem.builder ()
                              .exchangeDateTime (aDT)
                              .directionSending ()
                              .c2ID ("pop000001")
                              .c3ID ("pop000002")
                              // TSR document type — explicitly excluded by
                              // PeppolReportingHelper.isDocumentTypeEligableForReporting
                              .docTypeIDScheme ("busdox-docid-qns")
                              .docTypeIDValue ("urn:fdc:peppol:transaction-statistics-report:1.0::TransactionStatisticsReport##urn:fdc:peppol.eu:edec:trns:transaction-statistics-reporting:1.0::1.0")
                              .processID (EPredefinedProcessIdentifier.BIS3_BILLING)
                              .transportProtocolPeppolAS4v2 ()
                              .c1CountryCode ("FI")
                              .endUserID ("eu-non-eligible")
                              .build ();
  }

  /**
   * Build a sending {@link PeppolReportingItem} for the given exchange date, with the given index
   * encoded into the end user ID so that callers can generate distinct items per day.
   *
   * @param aDate
   *        The exchange date. Must not be <code>null</code>. The time component is set
   *        deterministically based on <code>nIndex</code>.
   * @param nIndex
   *        Sequence index, distinguishing items on the same day.
   * @return A non-<code>null</code> {@link PeppolReportingItem}.
   */
  @NonNull
  public static PeppolReportingItem sendingItem (@NonNull final LocalDate aDate, final int nIndex)
  {
    return _build (aDate, nIndex, EReportingDirection.SENDING);
  }

  /**
   * Build a receiving {@link PeppolReportingItem} for the given exchange date.
   *
   * @param aDate
   *        The exchange date. Must not be <code>null</code>.
   * @param nIndex
   *        Sequence index, distinguishing items on the same day.
   * @return A non-<code>null</code> {@link PeppolReportingItem}.
   */
  @NonNull
  public static PeppolReportingItem receivingItem (@NonNull final LocalDate aDate, final int nIndex)
  {
    return _build (aDate, nIndex, EReportingDirection.RECEIVING);
  }
}
