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

import java.util.Map;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.base.numeric.BigHelper;
import com.helger.collection.commons.CommonsTreeMap;
import com.helger.collection.commons.ICommonsSortedMap;
import com.helger.peppol.reporting.api.CPeppolReporting;
import com.helger.peppol.reporting.api.PeppolReportingItem;
import com.helger.peppol.reporting.jaxb.tsr.v101.IncomingOutgoingType;
import com.helger.peppol.reporting.jaxb.tsr.v101.SubtotalKeyType;
import com.helger.peppol.reporting.jaxb.tsr.v101.SubtotalType;
import com.helger.peppol.reporting.jaxb.tsr.v101.TransactionStatisticsReportType;

/**
 * Accumulator for TSR reporting items that supports batched (streaming) input.
 * <p>
 * Use {@link #accept(PeppolReportingItem)} to feed items one by one (potentially across many
 * batches), then call {@link #fillReport(TransactionStatisticsReportType)} to populate the report.
 * </p>
 * <p>
 * <strong>Thread-safety:</strong> This class is <em>not</em> thread-safe. Callers must not share
 * an instance across threads without external synchronisation.
 * </p>
 *
 * @author Philip Helger
 * @since 4.1.4
 */
public class TSRReportingItemAccumulator
{
  private static final class TransactionCounter
  {
    private long m_nIncoming = 0;
    private long m_nOutgoing = 0;

    public void inc (final boolean bIncoming)
    {
      if (bIncoming)
        m_nIncoming++;
      else
        m_nOutgoing++;
    }
  }

  // Total counters
  private long m_nTotalIncoming = 0;
  private long m_nTotalOutgoing = 0;

  // Subtotal maps
  private final ICommonsSortedMap <SubtotalKeyTP, TransactionCounter> m_aMapTP = new CommonsTreeMap <> ();
  private final ICommonsSortedMap <SubtotalKeySP_DT_PR, TransactionCounter> m_aMapSP_DT_PR = new CommonsTreeMap <> ();
  private final ICommonsSortedMap <SubtotalKeySP_DT_PR_CC, TransactionCounter> m_aMapSP_DT_PR_CC = new CommonsTreeMap <> ();

  public TSRReportingItemAccumulator ()
  {}

  /**
   * Accept a single {@link PeppolReportingItem} and accumulate its data into the internal state.
   * May be called multiple times, across multiple batches, before {@link #fillReport}.
   *
   * @param aItem
   *        The reporting item; must not be {@code null}.
   */
  public void accept (@NonNull final PeppolReportingItem aItem)
  {
    final SubtotalKeyTP aKeyTP = new SubtotalKeyTP (aItem.getTransportProtocol ());
    final SubtotalKeySP_DT_PR aKeySP_DT_PR = new SubtotalKeySP_DT_PR (aItem.getOtherServiceProviderID (),
                                                                      aItem.getDocTypeIDScheme (),
                                                                      aItem.getDocTypeIDValue (),
                                                                      aItem.getProcessIDScheme (),
                                                                      aItem.getProcessIDValue ());

    final boolean bIncoming = aItem.isReceiving ();
    m_aMapTP.computeIfAbsent (aKeyTP, x -> new TransactionCounter ()).inc (bIncoming);
    m_aMapSP_DT_PR.computeIfAbsent (aKeySP_DT_PR, x -> new TransactionCounter ()).inc (bIncoming);

    if (bIncoming)
    {
      m_nTotalIncoming++;

      // This can only be counted for incoming messages, as senders never have the C4 ID
      final SubtotalKeySP_DT_PR_CC aKeySP_DT_PR_CC = new SubtotalKeySP_DT_PR_CC (aItem.getOtherServiceProviderID (),
                                                                                 aItem.getDocTypeIDScheme (),
                                                                                 aItem.getDocTypeIDValue (),
                                                                                 aItem.getProcessIDScheme (),
                                                                                 aItem.getProcessIDValue (),
                                                                                 aItem.getC1CountryCode (),
                                                                                 aItem.getC4CountryCode ());
      m_aMapSP_DT_PR_CC.computeIfAbsent (aKeySP_DT_PR_CC, x -> new TransactionCounter ()).inc (bIncoming);
    }
    else
      m_nTotalOutgoing++;
  }

  @NonNull
  private static SubtotalKeyType _createSubtotalKey (@NonNull @Nonempty final String sMetaSchemeID,
                                                     @NonNull @Nonempty final String sSchemeID,
                                                     @NonNull @Nonempty final String sValue)
  {
    final SubtotalKeyType ret = new SubtotalKeyType ();
    ret.setMetaSchemeID (sMetaSchemeID);
    ret.setSchemeID (sSchemeID);
    ret.setValue (sValue);
    return ret;
  }

  /**
   * Write the accumulated data as Total and Subtotals into the given report.
   * Call this after all {@link #accept} calls have been made.
   *
   * @param aReport
   *        The report to populate; must not be {@code null}.
   */
  public void fillReport (@NonNull final TransactionStatisticsReportType aReport)
  {
    // Add Total
    {
      final IncomingOutgoingType aTotal = new IncomingOutgoingType ();
      aTotal.setIncoming (BigHelper.toBigInteger (m_nTotalIncoming));
      aTotal.setOutgoing (BigHelper.toBigInteger (m_nTotalOutgoing));
      aReport.setTotal (aTotal);
    }

    // TP subtotals
    for (final Map.Entry <SubtotalKeyTP, TransactionCounter> e : m_aMapTP.entrySet ())
    {
      final SubtotalKeyTP aKey = e.getKey ();
      final TransactionCounter aVal = e.getValue ();

      final SubtotalType aSubtotal = new SubtotalType ();
      aSubtotal.setType (SubtotalKeyTP.TYPE);
      aSubtotal.addKey (_createSubtotalKey (CTSR.TSR_METASCHEME_TP, CTSR.TSR_SCHEME_TP_PEPPOL, aKey.getTransportProtocol ()));
      aSubtotal.setIncoming (BigHelper.toBigInteger (aVal.m_nIncoming));
      aSubtotal.setOutgoing (BigHelper.toBigInteger (aVal.m_nOutgoing));
      aReport.addSubtotal (aSubtotal);
    }

    // SP+DT+PR subtotals
    for (final Map.Entry <SubtotalKeySP_DT_PR, TransactionCounter> e : m_aMapSP_DT_PR.entrySet ())
    {
      final SubtotalKeySP_DT_PR aKey = e.getKey ();
      final TransactionCounter aVal = e.getValue ();

      final SubtotalType aSubtotal = new SubtotalType ();
      aSubtotal.setType (SubtotalKeySP_DT_PR.TYPE);
      aSubtotal.addKey (_createSubtotalKey (CTSR.TSR_METASCHEME_SP,
                                            CPeppolReporting.SERVICE_PROVIDER_ID_SCHEME,
                                            aKey.getServiceProviderID ()));
      aSubtotal.addKey (_createSubtotalKey (CTSR.TSR_METASCHEME_DT, aKey.getDocTypeIDScheme (), aKey.getDocTypeIDValue ()));
      aSubtotal.addKey (_createSubtotalKey (CTSR.TSR_METASCHEME_PR, aKey.getProcessIDScheme (), aKey.getProcessIDValue ()));
      aSubtotal.setIncoming (BigHelper.toBigInteger (aVal.m_nIncoming));
      aSubtotal.setOutgoing (BigHelper.toBigInteger (aVal.m_nOutgoing));
      aReport.addSubtotal (aSubtotal);
    }

    // SP+DT+PR+CC subtotals
    for (final Map.Entry <SubtotalKeySP_DT_PR_CC, TransactionCounter> e : m_aMapSP_DT_PR_CC.entrySet ())
    {
      final SubtotalKeySP_DT_PR_CC aKey = e.getKey ();
      final TransactionCounter aVal = e.getValue ();

      final SubtotalType aSubtotal = new SubtotalType ();
      aSubtotal.setType (SubtotalKeySP_DT_PR_CC.TYPE);
      aSubtotal.addKey (_createSubtotalKey (CTSR.TSR_METASCHEME_SP,
                                            CPeppolReporting.SERVICE_PROVIDER_ID_SCHEME,
                                            aKey.getServiceProviderID ()));
      aSubtotal.addKey (_createSubtotalKey (CTSR.TSR_METASCHEME_DT, aKey.getDocTypeIDScheme (), aKey.getDocTypeIDValue ()));
      aSubtotal.addKey (_createSubtotalKey (CTSR.TSR_METASCHEME_PR, aKey.getProcessIDScheme (), aKey.getProcessIDValue ()));
      aSubtotal.addKey (_createSubtotalKey (CTSR.TSR_METASCHEME_CC, CTSR.TSR_SCHEME_CC_SENDER_COUNTRY, aKey.getC1CountryCode ()));
      aSubtotal.addKey (_createSubtotalKey (CTSR.TSR_METASCHEME_CC, CTSR.TSR_SCHEME_CC_RECEIVER_COUNTRY, aKey.getC4CountryCode ()));
      aSubtotal.setIncoming (BigHelper.toBigInteger (aVal.m_nIncoming));
      // Must always be zero
      aSubtotal.setOutgoing (BigHelper.toBigInteger (aVal.m_nOutgoing));
      aReport.addSubtotal (aSubtotal);
    }
  }
}

