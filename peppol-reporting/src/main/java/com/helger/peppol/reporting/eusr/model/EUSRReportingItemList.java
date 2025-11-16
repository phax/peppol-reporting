/*
 * Copyright (C) 2022-2025 Philip Helger
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

import java.math.BigInteger;
import java.util.Map;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.Immutable;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.numeric.BigHelper;
import com.helger.collection.commons.CommonsHashSet;
import com.helger.collection.commons.CommonsTreeMap;
import com.helger.collection.commons.ICommonsSet;
import com.helger.collection.commons.ICommonsSortedMap;
import com.helger.peppol.reporting.api.PeppolReportingItem;
import com.helger.peppol.reporting.jaxb.eusr.v110.EndUserStatisticsReportType;
import com.helger.peppol.reporting.jaxb.eusr.v110.FullSetType;
import com.helger.peppol.reporting.jaxb.eusr.v110.SubsetKeyType;
import com.helger.peppol.reporting.jaxb.eusr.v110.SubsetType;

/**
 * This class represents a set of {@link PeppolReportingItem} objects for a single Reporting Period
 * used to create EUSR reports.
 *
 * @author Philip Helger
 * @since 1.2.0
 */
@Immutable
public class EUSRReportingItemList
{
  private EUSRReportingItemList ()
  {}

  private static final class EndUserCounter
  {
    private final ICommonsSet <String> m_aSenders = new CommonsHashSet <> ();
    private final ICommonsSet <String> m_aReceivers = new CommonsHashSet <> ();
    private final ICommonsSet <String> m_aSendersOrReceivers = new CommonsHashSet <> ();

    public void inc (@NonNull @Nonempty final String sEndUserID, final boolean bSending)
    {
      if (bSending)
        m_aSenders.add (sEndUserID);
      else
        m_aReceivers.add (sEndUserID);
      m_aSendersOrReceivers.add (sEndUserID);
    }

    @NonNull
    public BigInteger getSendingEndUserCount ()
    {
      return BigHelper.toBigInteger (m_aSenders.size ());
    }

    @NonNull
    public BigInteger getReceivingEndUserCount ()
    {
      return BigHelper.toBigInteger (m_aReceivers.size ());
    }

    @NonNull
    public BigInteger getSendingOrReceivingEndUserCount ()
    {
      return BigHelper.toBigInteger (m_aSendersOrReceivers.size ());
    }
  }

  @NonNull
  private static SubsetKeyType _createSubsetKey (@NonNull @Nonempty final String sMetaSchemeID,
                                                 @NonNull @Nonempty final String sSchemeID,
                                                 @NonNull @Nonempty final String sValue)
  {
    final SubsetKeyType ret = new SubsetKeyType ();
    ret.setMetaSchemeID (sMetaSchemeID);
    ret.setSchemeID (sSchemeID);
    ret.setValue (sValue);
    return ret;
  }

  public static void fillReportSubsets (@NonNull final Iterable <? extends PeppolReportingItem> aReportingItems,
                                        @NonNull final EndUserStatisticsReportType aReport)
  {
    ValueEnforcer.notNull (aReportingItems, "ReportingItems");
    ValueEnforcer.notNull (aReport, "Report");

    // For total full sets
    final ICommonsSet <String> aSendingEndUsers = new CommonsHashSet <> ();
    final ICommonsSet <String> aReceivingEndUsers = new CommonsHashSet <> ();
    final ICommonsSet <String> aSendingOrReceivingEndUsers = new CommonsHashSet <> ();

    // Create subsets
    final ICommonsSortedMap <SubsetKeyDT_PR, EndUserCounter> m_aMapDT_PR = new CommonsTreeMap <> ();
    final ICommonsSortedMap <SubsetKeyEUC, EndUserCounter> m_aMapEUC = new CommonsTreeMap <> ();
    final ICommonsSortedMap <SubsetKeyDT_EUC, EndUserCounter> m_aMapDT_EUC = new CommonsTreeMap <> ();
    final ICommonsSortedMap <SubsetKeyDT_PR_EUC, EndUserCounter> m_aMapDT_PR_EUC = new CommonsTreeMap <> ();

    for (final PeppolReportingItem aItem : aReportingItems)
    {
      final SubsetKeyDT_PR aKeyDT_PR = new SubsetKeyDT_PR (aItem.getDocTypeIDScheme (),
                                                           aItem.getDocTypeIDValue (),
                                                           aItem.getProcessIDScheme (),
                                                           aItem.getProcessIDValue ());
      final SubsetKeyEUC aKeyEUC = new SubsetKeyEUC (aItem.getEndUserCountryCode ());
      final SubsetKeyDT_EUC aKeyDT_EUC = new SubsetKeyDT_EUC (aItem.getDocTypeIDScheme (),
                                                              aItem.getDocTypeIDValue (),
                                                              aItem.getEndUserCountryCode ());
      final SubsetKeyDT_PR_EUC aKeyDT_PR_EUC = new SubsetKeyDT_PR_EUC (aItem.getDocTypeIDScheme (),
                                                                       aItem.getDocTypeIDValue (),
                                                                       aItem.getProcessIDScheme (),
                                                                       aItem.getProcessIDValue (),
                                                                       aItem.getEndUserCountryCode ());

      final String sEndUserID = aItem.getEndUserID ();
      final boolean bSending = aItem.isSending ();
      m_aMapDT_PR.computeIfAbsent (aKeyDT_PR, x -> new EndUserCounter ()).inc (sEndUserID, bSending);
      m_aMapEUC.computeIfAbsent (aKeyEUC, x -> new EndUserCounter ()).inc (sEndUserID, bSending);
      m_aMapDT_EUC.computeIfAbsent (aKeyDT_EUC, x -> new EndUserCounter ()).inc (sEndUserID, bSending);
      m_aMapDT_PR_EUC.computeIfAbsent (aKeyDT_PR_EUC, x -> new EndUserCounter ()).inc (sEndUserID, bSending);

      if (bSending)
        aSendingEndUsers.add (sEndUserID);
      else
        aReceivingEndUsers.add (sEndUserID);
      aSendingOrReceivingEndUsers.add (sEndUserID);
    }

    // Add full set
    {
      final FullSetType aFullSet = new FullSetType ();
      aFullSet.setSendingEndUsers (BigHelper.toBigInteger (aSendingEndUsers.size ()));
      aFullSet.setReceivingEndUsers (BigHelper.toBigInteger (aReceivingEndUsers.size ()));
      aFullSet.setSendingOrReceivingEndUsers (BigHelper.toBigInteger (aSendingOrReceivingEndUsers.size ()));
      aReport.setFullSet (aFullSet);
    }

    // Add to report
    for (final Map.Entry <SubsetKeyDT_PR, EndUserCounter> e : m_aMapDT_PR.entrySet ())
    {
      final SubsetKeyDT_PR aKey = e.getKey ();
      final EndUserCounter aVal = e.getValue ();

      final SubsetType aSubset = new SubsetType ();
      aSubset.setType (SubsetKeyDT_PR.TYPE);
      aSubset.addKey (_createSubsetKey (CEUSR.EUSR_METASCHEME_DT,
                                        aKey.getDocTypeIDScheme (),
                                        aKey.getDocTypeIDValue ()));
      aSubset.addKey (_createSubsetKey (CEUSR.EUSR_METASCHEME_PR,
                                        aKey.getProcessIDScheme (),
                                        aKey.getProcessIDValue ()));
      aSubset.setSendingEndUsers (aVal.getSendingEndUserCount ());
      aSubset.setReceivingEndUsers (aVal.getReceivingEndUserCount ());
      aSubset.setSendingOrReceivingEndUsers (aVal.getSendingOrReceivingEndUserCount ());
      aReport.addSubset (aSubset);
    }

    // Add to report
    for (final Map.Entry <SubsetKeyEUC, EndUserCounter> e : m_aMapEUC.entrySet ())
    {
      final SubsetKeyEUC aKey = e.getKey ();
      final EndUserCounter aVal = e.getValue ();

      final SubsetType aSubset = new SubsetType ();
      aSubset.setType (SubsetKeyEUC.TYPE);
      aSubset.addKey (_createSubsetKey (CEUSR.EUSR_METASCHEME_CC,
                                        CEUSR.EUSR_SCHEME_CC_END_USER_COUNTRY,
                                        aKey.getEndUserCountryCode ()));
      aSubset.setSendingEndUsers (aVal.getSendingEndUserCount ());
      aSubset.setReceivingEndUsers (aVal.getReceivingEndUserCount ());
      aSubset.setSendingOrReceivingEndUsers (aVal.getSendingOrReceivingEndUserCount ());
      aReport.addSubset (aSubset);
    }

    // Add to report
    for (final Map.Entry <SubsetKeyDT_EUC, EndUserCounter> e : m_aMapDT_EUC.entrySet ())
    {
      final SubsetKeyDT_EUC aKey = e.getKey ();
      final EndUserCounter aVal = e.getValue ();

      final SubsetType aSubset = new SubsetType ();
      aSubset.setType (SubsetKeyDT_EUC.TYPE);
      aSubset.addKey (_createSubsetKey (CEUSR.EUSR_METASCHEME_DT,
                                        aKey.getDocTypeIDScheme (),
                                        aKey.getDocTypeIDValue ()));
      aSubset.addKey (_createSubsetKey (CEUSR.EUSR_METASCHEME_CC,
                                        CEUSR.EUSR_SCHEME_CC_END_USER_COUNTRY,
                                        aKey.getEndUserCountryCode ()));
      aSubset.setSendingEndUsers (aVal.getSendingEndUserCount ());
      aSubset.setReceivingEndUsers (aVal.getReceivingEndUserCount ());
      aSubset.setSendingOrReceivingEndUsers (aVal.getSendingOrReceivingEndUserCount ());
      aReport.addSubset (aSubset);
    }

    // Add to report
    for (final Map.Entry <SubsetKeyDT_PR_EUC, EndUserCounter> e : m_aMapDT_PR_EUC.entrySet ())
    {
      final SubsetKeyDT_PR_EUC aKey = e.getKey ();
      final EndUserCounter aVal = e.getValue ();

      final SubsetType aSubset = new SubsetType ();
      aSubset.setType (SubsetKeyDT_PR_EUC.TYPE);
      aSubset.addKey (_createSubsetKey (CEUSR.EUSR_METASCHEME_DT,
                                        aKey.getDocTypeIDScheme (),
                                        aKey.getDocTypeIDValue ()));
      aSubset.addKey (_createSubsetKey (CEUSR.EUSR_METASCHEME_PR,
                                        aKey.getProcessIDScheme (),
                                        aKey.getProcessIDValue ()));
      aSubset.addKey (_createSubsetKey (CEUSR.EUSR_METASCHEME_CC,
                                        CEUSR.EUSR_SCHEME_CC_END_USER_COUNTRY,
                                        aKey.getEndUserCountryCode ()));
      aSubset.setSendingEndUsers (aVal.getSendingEndUserCount ());
      aSubset.setReceivingEndUsers (aVal.getReceivingEndUserCount ());
      aSubset.setSendingOrReceivingEndUsers (aVal.getSendingOrReceivingEndUserCount ());
      aReport.addSubset (aSubset);
    }
  }
}
