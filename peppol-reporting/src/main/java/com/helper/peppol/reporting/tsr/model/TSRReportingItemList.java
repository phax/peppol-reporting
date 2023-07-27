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

import java.math.BigInteger;
import java.util.Map;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.CommonsTreeMap;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsSortedMap;
import com.helger.commons.math.MathHelper;
import com.helger.peppol.reporting.jaxb.tsr.v101.SubtotalKeyType;
import com.helger.peppol.reporting.jaxb.tsr.v101.SubtotalType;
import com.helger.peppol.reporting.jaxb.tsr.v101.TransactionStatisticsReportType;
import com.helper.peppol.reporting.api.CPeppolReporting;
import com.helper.peppol.reporting.api.PeppolReportingItem;

/**
 * This class represents a set of {@link PeppolReportingItem} objects for a
 * single Reporting Period used to create TSR reports.
 *
 * @author Philip Helger
 * @since 1.2.0
 */
public class TSRReportingItemList
{
  private final ICommonsList <PeppolReportingItem> m_aList = new CommonsArrayList <> ();

  public TSRReportingItemList ()
  {}

  public TSRReportingItemList (@Nullable final PeppolReportingItem... aItems)
  {
    if (aItems != null)
      m_aList.addAll (aItems);
  }

  public TSRReportingItemList (@Nullable final Iterable <? extends PeppolReportingItem> aItems)
  {
    if (aItems != null)
      m_aList.addAll (aItems);
  }

  @Nonnull
  public TSRReportingItemList add (@Nonnull final PeppolReportingItem aItem)
  {
    ValueEnforcer.notNull (aItem, "Item");
    m_aList.add (aItem);
    return this;
  }

  /**
   * @return The number of total incoming, received messages. Always &ge; 0.
   */
  @Nonnegative
  public long getTotalIncomingCount ()
  {
    return m_aList.stream ().filter (PeppolReportingItem::isReceiving).count ();
  }

  /**
   * @return The number of total outgoing, sent messages. Always &ge; 0.
   */
  @Nonnegative
  public long getTotalOutgoingCount ()
  {
    return m_aList.stream ().filter (PeppolReportingItem::isSending).count ();
  }

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

    @Nonnull
    public BigInteger getIncomingCount ()
    {
      return MathHelper.toBigInteger (m_nIncoming);
    }

    @Nonnull
    public BigInteger getOutgoingCount ()
    {
      return MathHelper.toBigInteger (m_nOutgoing);
    }
  }

  @Nonnull
  private static SubtotalKeyType _createSubtotalKey (@Nonnull @Nonempty final String sMetaSchemeID,
                                                     @Nonnull @Nonempty final String sSchemeID,
                                                     @Nonnull @Nonempty final String sValue)
  {
    final SubtotalKeyType ret = new SubtotalKeyType ();
    ret.setMetaSchemeID (sMetaSchemeID);
    ret.setSchemeID (sSchemeID);
    ret.setValue (sValue);
    return ret;
  }

  public void fillReportSubsets (@Nonnull final TransactionStatisticsReportType aReport)
  {
    // Create subsets
    final ICommonsSortedMap <SubtotalKeyTP, TransactionCounter> m_aMapTP = new CommonsTreeMap <> ();
    final ICommonsSortedMap <SubtotalKeySP_DT_PR, TransactionCounter> m_aMapSP_DT_PR = new CommonsTreeMap <> ();
    final ICommonsSortedMap <SubtotalKeySP_DT_PR_CC, TransactionCounter> m_aMapSP_DT_PR_CC = new CommonsTreeMap <> ();

    for (final PeppolReportingItem aItem : m_aList)
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
        // This can only be counted for incoming messages, as senders never have
        // the C4 ID
        final SubtotalKeySP_DT_PR_CC aKeySP_DT_PR_CC = new SubtotalKeySP_DT_PR_CC (aItem.getOtherServiceProviderID (),
                                                                                   aItem.getDocTypeIDScheme (),
                                                                                   aItem.getDocTypeIDValue (),
                                                                                   aItem.getProcessIDScheme (),
                                                                                   aItem.getProcessIDValue (),
                                                                                   aItem.getC1CountryCode (),
                                                                                   aItem.getC4CountryCode ());
        m_aMapSP_DT_PR_CC.computeIfAbsent (aKeySP_DT_PR_CC, x -> new TransactionCounter ()).inc (bIncoming);
      }
    }

    // Add to report
    for (final Map.Entry <SubtotalKeyTP, TransactionCounter> e : m_aMapTP.entrySet ())
    {
      final SubtotalKeyTP aKey = e.getKey ();
      final TransactionCounter aVal = e.getValue ();

      final SubtotalType aSubtotal = new SubtotalType ();
      aSubtotal.setType (SubtotalKeyTP.TYPE);
      aSubtotal.addKey (_createSubtotalKey (CTSR.TSR_METASCHEME_TP,
                                            CTSR.TSR_SCHEME_TP_PEPPOL,
                                            aKey.getTransportProtocol ()));
      aSubtotal.setIncoming (aVal.getIncomingCount ());
      aSubtotal.setOutgoing (aVal.getOutgoingCount ());
      aReport.addSubtotal (aSubtotal);
    }

    // Add to report
    for (final Map.Entry <SubtotalKeySP_DT_PR, TransactionCounter> e : m_aMapSP_DT_PR.entrySet ())
    {
      final SubtotalKeySP_DT_PR aKey = e.getKey ();
      final TransactionCounter aVal = e.getValue ();

      final SubtotalType aSubtotal = new SubtotalType ();
      aSubtotal.setType (SubtotalKeySP_DT_PR.TYPE);
      aSubtotal.addKey (_createSubtotalKey (CTSR.TSR_METASCHEME_SP,
                                            CPeppolReporting.SERVICE_PROVIDER_ID_SCHEME,
                                            aKey.getServiceProviderID ()));
      aSubtotal.addKey (_createSubtotalKey (CTSR.TSR_METASCHEME_DT,
                                            aKey.getDocTypeIDScheme (),
                                            aKey.getDocTypeIDValue ()));
      aSubtotal.addKey (_createSubtotalKey (CTSR.TSR_METASCHEME_PR,
                                            aKey.getProcessIDScheme (),
                                            aKey.getProcessIDValue ()));
      aSubtotal.setIncoming (aVal.getIncomingCount ());
      aSubtotal.setOutgoing (aVal.getOutgoingCount ());
      aReport.addSubtotal (aSubtotal);
    }

    // Add to report
    for (final Map.Entry <SubtotalKeySP_DT_PR_CC, TransactionCounter> e : m_aMapSP_DT_PR_CC.entrySet ())
    {
      final SubtotalKeySP_DT_PR_CC aKey = e.getKey ();
      final TransactionCounter aVal = e.getValue ();

      final SubtotalType aSubtotal = new SubtotalType ();
      aSubtotal.setType (SubtotalKeySP_DT_PR_CC.TYPE);
      aSubtotal.addKey (_createSubtotalKey (CTSR.TSR_METASCHEME_SP,
                                            CPeppolReporting.SERVICE_PROVIDER_ID_SCHEME,
                                            aKey.getServiceProviderID ()));
      aSubtotal.addKey (_createSubtotalKey (CTSR.TSR_METASCHEME_DT,
                                            aKey.getDocTypeIDScheme (),
                                            aKey.getDocTypeIDValue ()));
      aSubtotal.addKey (_createSubtotalKey (CTSR.TSR_METASCHEME_PR,
                                            aKey.getProcessIDScheme (),
                                            aKey.getProcessIDValue ()));
      aSubtotal.addKey (_createSubtotalKey (CTSR.TSR_METASCHEME_CC,
                                            CTSR.TSR_SCHEME_CC_SENDER_COUNTRY,
                                            aKey.getC1CountryCode ()));
      aSubtotal.addKey (_createSubtotalKey (CTSR.TSR_METASCHEME_CC,
                                            CTSR.TSR_SCHEME_CC_RECEIVER_COUNTRY,
                                            aKey.getC4CountryCode ()));
      aSubtotal.setIncoming (aVal.getIncomingCount ());
      // Must always be zero
      aSubtotal.setOutgoing (aVal.getOutgoingCount ());
      aReport.addSubtotal (aSubtotal);
    }
  }
}
