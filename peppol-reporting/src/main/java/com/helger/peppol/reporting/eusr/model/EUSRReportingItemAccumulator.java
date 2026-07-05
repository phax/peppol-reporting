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

import java.util.Map;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
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
import com.helger.peppolid.peppol.PeppolIdentifierHelper;
import com.helger.peppolid.peppol.doctype.EPredefinedDocumentTypeIdentifier;

/**
 * Accumulator for EUSR reporting items that supports batched (streaming) input.
 * <p>
 * Use {@link #accept(PeppolReportingItem)} to feed items one by one (potentially across many
 * batches), then call {@link #fillReport(EndUserStatisticsReportType)} to populate the report.
 * </p>
 * <p>
 * <strong>Thread-safety:</strong> This class is <em>not</em> thread-safe. Callers must not share an
 * instance across threads without external synchronisation.
 * </p>
 *
 * @author Philip Helger
 * @since 4.1.4
 */
public class EUSRReportingItemAccumulator
{
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
  }

  // Full-set counters
  private final ICommonsSet <String> m_aSendingEndUsers = new CommonsHashSet <> ();
  private final ICommonsSet <String> m_aReceivingEndUsers = new CommonsHashSet <> ();
  private final ICommonsSet <String> m_aSendingOrReceivingEndUsers = new CommonsHashSet <> ();

  // Subset maps
  private final ICommonsSortedMap <SubsetKeyDT_PR, EndUserCounter> m_aMapDT_PR = new CommonsTreeMap <> ();
  private final ICommonsSortedMap <SubsetKeyEUC, EndUserCounter> m_aMapEUC = new CommonsTreeMap <> ();
  private final ICommonsSortedMap <SubsetKeyDT_EUC, EndUserCounter> m_aMapDT_EUC = new CommonsTreeMap <> ();
  private final ICommonsSortedMap <SubsetKeyDT_PR_EUC, EndUserCounter> m_aMapDT_PR_EUC = new CommonsTreeMap <> ();

  public EUSRReportingItemAccumulator ()
  {}

  private static boolean _isMLSDocType (@NonNull final PeppolReportingItem aItem)
  {
    return PeppolIdentifierHelper.DOCUMENT_TYPE_SCHEME_BUSDOX_DOCID_QNS.equals (aItem.getDocTypeIDScheme ()) &&
      EPredefinedDocumentTypeIdentifier.PEPPOL_MLS_1_0.getValue ().equals (aItem.getDocTypeIDValue ());
  }

  /**
   * Accept a single {@link PeppolReportingItem} and accumulate its data into the internal state.
   * May be called multiple times, across multiple batches, before {@link #fillReport}.
   *
   * @param aItem
   *        The reporting item; must not be {@code null}.
   */
  public void accept (@NonNull final PeppolReportingItem aItem)
  {
    // explicit avoid counting MLS message for EUSR (see SPOG on MLS)
    if (!_isMLSDocType (aItem))
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
        m_aSendingEndUsers.add (sEndUserID);
      else
        m_aReceivingEndUsers.add (sEndUserID);
      m_aSendingOrReceivingEndUsers.add (sEndUserID);
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

  /**
   * Write the accumulated data as FullSet and Subsets into the given report. Call this after all
   * {@link #accept} calls have been made.
   *
   * @param aReport
   *        The report to populate; must not be {@code null}.
   */
  public void fillReport (@NonNull final EndUserStatisticsReportType aReport)
  {
    // Add full set
    {
      final FullSetType aFullSet = new FullSetType ();
      aFullSet.setSendingEndUsers (BigHelper.toBigInteger (m_aSendingEndUsers.size ()));
      aFullSet.setReceivingEndUsers (BigHelper.toBigInteger (m_aReceivingEndUsers.size ()));
      aFullSet.setSendingOrReceivingEndUsers (BigHelper.toBigInteger (m_aSendingOrReceivingEndUsers.size ()));
      aReport.setFullSet (aFullSet);
    }

    // DT+PR subsets
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
      aSubset.setSendingEndUsers (BigHelper.toBigInteger (aVal.m_aSenders.size ()));
      aSubset.setReceivingEndUsers (BigHelper.toBigInteger (aVal.m_aReceivers.size ()));
      aSubset.setSendingOrReceivingEndUsers (BigHelper.toBigInteger (aVal.m_aSendersOrReceivers.size ()));
      aReport.addSubset (aSubset);
    }

    // EUC subsets
    for (final Map.Entry <SubsetKeyEUC, EndUserCounter> e : m_aMapEUC.entrySet ())
    {
      final SubsetKeyEUC aKey = e.getKey ();
      final EndUserCounter aVal = e.getValue ();

      final SubsetType aSubset = new SubsetType ();
      aSubset.setType (SubsetKeyEUC.TYPE);
      aSubset.addKey (_createSubsetKey (CEUSR.EUSR_METASCHEME_CC,
                                        CEUSR.EUSR_SCHEME_CC_END_USER_COUNTRY,
                                        aKey.getEndUserCountryCode ()));
      aSubset.setSendingEndUsers (BigHelper.toBigInteger (aVal.m_aSenders.size ()));
      aSubset.setReceivingEndUsers (BigHelper.toBigInteger (aVal.m_aReceivers.size ()));
      aSubset.setSendingOrReceivingEndUsers (BigHelper.toBigInteger (aVal.m_aSendersOrReceivers.size ()));
      aReport.addSubset (aSubset);
    }

    // DT+EUC subsets
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
      aSubset.setSendingEndUsers (BigHelper.toBigInteger (aVal.m_aSenders.size ()));
      aSubset.setReceivingEndUsers (BigHelper.toBigInteger (aVal.m_aReceivers.size ()));
      aSubset.setSendingOrReceivingEndUsers (BigHelper.toBigInteger (aVal.m_aSendersOrReceivers.size ()));
      aReport.addSubset (aSubset);
    }

    // DT+PR+EUC subsets
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
      aSubset.setSendingEndUsers (BigHelper.toBigInteger (aVal.m_aSenders.size ()));
      aSubset.setReceivingEndUsers (BigHelper.toBigInteger (aVal.m_aReceivers.size ()));
      aSubset.setSendingOrReceivingEndUsers (BigHelper.toBigInteger (aVal.m_aSendersOrReceivers.size ()));
      aReport.addSubset (aSubset);
    }
  }
}
