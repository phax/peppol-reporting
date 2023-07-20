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

import java.math.BigInteger;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.CommonsHashSet;
import com.helger.commons.collection.impl.CommonsTreeMap;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsSet;
import com.helger.commons.collection.impl.ICommonsSortedMap;
import com.helger.commons.math.MathHelper;
import com.helger.peppol.reporting.jaxb.eusr.v110.EndUserStatisticsReportType;
import com.helger.peppol.reporting.jaxb.eusr.v110.SubsetKeyType;
import com.helger.peppol.reporting.jaxb.eusr.v110.SubsetType;
import com.helper.peppol.reporting.model.PeppolReportingItem;

/**
 * This class represents a set of {@link PeppolReportingItem} objects for a
 * single Reporting Period used to create EUSR reports.
 *
 * @author Philip Helger
 */
public class EUSRReportingItemList
{
  private final ICommonsList <PeppolReportingItem> m_aList = new CommonsArrayList <> ();

  public EUSRReportingItemList ()
  {}

  public EUSRReportingItemList (@Nullable final PeppolReportingItem... aItems)
  {
    if (aItems != null)
      m_aList.addAll (aItems);
  }

  public EUSRReportingItemList (@Nullable final Iterable <? extends PeppolReportingItem> aItems)
  {
    if (aItems != null)
      m_aList.addAll (aItems);
  }

  @Nonnull
  public EUSRReportingItemList add (@Nonnull final PeppolReportingItem aItem)
  {
    ValueEnforcer.notNull (aItem, "Item");
    m_aList.add (aItem);
    return this;
  }

  /**
   * @return The number of unique sending end user IDs. Always &ge; 0.
   */
  @Nonnegative
  public int getSendingEndUserCount ()
  {
    return m_aList.stream ()
                  .filter (PeppolReportingItem::isSending)
                  .map (PeppolReportingItem::getEndUserID)
                  .collect (Collectors.toSet ())
                  .size ();
  }

  /**
   * @return The number of unique receiving end user IDs. Always &ge; 0.
   */
  @Nonnegative
  public int getReceivingEndUserCount ()
  {
    return m_aList.stream ()
                  .filter (PeppolReportingItem::isReceiving)
                  .map (PeppolReportingItem::getEndUserID)
                  .collect (Collectors.toSet ())
                  .size ();
  }

  /**
   * @return The number of unique end user IDs (independent of the message
   *         exchange direction). Always &ge; 0.
   */
  @Nonnegative
  public int getSendingOrReceivingEndUserCount ()
  {
    return m_aList.stream ().map (PeppolReportingItem::getEndUserID).collect (Collectors.toSet ()).size ();
  }

  private static final class EndUserCounter
  {
    private final ICommonsSet <String> m_aSenders = new CommonsHashSet <> ();
    private final ICommonsSet <String> m_aReceivers = new CommonsHashSet <> ();
    private final ICommonsSet <String> m_aSendersOrReceivers = new CommonsHashSet <> ();

    public void inc (@Nonnull @Nonempty final String sEndUserID, final boolean bSending)
    {
      if (bSending)
        m_aSenders.add (sEndUserID);
      else
        m_aReceivers.add (sEndUserID);
      m_aSendersOrReceivers.add (sEndUserID);
    }

    @Nonnull
    public BigInteger getSendingEndUserCount ()
    {
      return MathHelper.toBigInteger (m_aSenders.size ());
    }

    @Nonnull
    public BigInteger getReceivingEndUserCount ()
    {
      return MathHelper.toBigInteger (m_aReceivers.size ());
    }

    @Nonnull
    public BigInteger getSendingOrReceivingEndUserCount ()
    {
      return MathHelper.toBigInteger (m_aSendersOrReceivers.size ());
    }
  }

  @Nonnull
  private static SubsetKeyType _createSubsetKey (@Nonnull @Nonempty final String sMetaSchemeID,
                                                 @Nonnull @Nonempty final String sSchemeID,
                                                 @Nonnull @Nonempty final String sValue)
  {
    final SubsetKeyType ret = new SubsetKeyType ();
    ret.setMetaSchemeID (sMetaSchemeID);
    ret.setSchemeID (sSchemeID);
    ret.setValue (sValue);
    return ret;
  }

  public void fillReportSubsets (@Nonnull final EndUserStatisticsReportType aReport)
  {
    // Create subsets
    final ICommonsSortedMap <SubsetKeyDT_PR, EndUserCounter> m_aMapDT_PR = new CommonsTreeMap <> ();
    final ICommonsSortedMap <SubsetKeyEUC, EndUserCounter> m_aMapEUC = new CommonsTreeMap <> ();
    final ICommonsSortedMap <SubsetKeyDT_EUC, EndUserCounter> m_aMapDT_EUC = new CommonsTreeMap <> ();
    final ICommonsSortedMap <SubsetKeyDT_PR_EUC, EndUserCounter> m_aMapDT_PR_EUC = new CommonsTreeMap <> ();

    for (final PeppolReportingItem aItem : m_aList)
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
    }

    // Add to report
    for (final Map.Entry <SubsetKeyDT_PR, EndUserCounter> e : m_aMapDT_PR.entrySet ())
    {
      final SubsetKeyDT_PR aKey = e.getKey ();
      final EndUserCounter aVal = e.getValue ();

      final SubsetType aSubset = new SubsetType ();
      aSubset.setType (SubsetKeyDT_PR.TYPE);
      aSubset.addKey (_createSubsetKey ("DT", aKey.getDocTypeIDScheme (), aKey.getDocTypeIDValue ()));
      aSubset.addKey (_createSubsetKey ("PR", aKey.getProcessIDScheme (), aKey.getProcessIDValue ()));
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
      aSubset.addKey (_createSubsetKey ("CC", "EndUserCountry", aKey.getEndUserCountryCode ()));
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
      aSubset.addKey (_createSubsetKey ("DT", aKey.getDocTypeIDScheme (), aKey.getDocTypeIDValue ()));
      aSubset.addKey (_createSubsetKey ("CC", "EndUserCountry", aKey.getEndUserCountryCode ()));
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
      aSubset.addKey (_createSubsetKey ("DT", aKey.getDocTypeIDScheme (), aKey.getDocTypeIDValue ()));
      aSubset.addKey (_createSubsetKey ("PR", aKey.getProcessIDScheme (), aKey.getProcessIDValue ()));
      aSubset.addKey (_createSubsetKey ("CC", "EndUserCountry", aKey.getEndUserCountryCode ()));
      aSubset.setSendingEndUsers (aVal.getSendingEndUserCount ());
      aSubset.setReceivingEndUsers (aVal.getReceivingEndUserCount ());
      aSubset.setSendingOrReceivingEndUsers (aVal.getSendingOrReceivingEndUserCount ());
      aReport.addSubset (aSubset);
    }
  }
}
