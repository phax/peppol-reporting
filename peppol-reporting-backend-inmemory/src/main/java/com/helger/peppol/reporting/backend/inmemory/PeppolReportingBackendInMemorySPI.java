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
package com.helger.peppol.reporting.backend.inmemory;

import java.time.LocalDate;
import java.util.function.Consumer;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.GuardedBy;
import com.helger.annotation.style.IsSPIImplementation;
import com.helger.base.concurrent.SimpleReadWriteLock;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.state.ESuccess;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsHashMap;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsMap;
import com.helger.config.IConfig;
import com.helger.peppol.reporting.api.PeppolReportingHelper;
import com.helger.peppol.reporting.api.PeppolReportingItem;
import com.helger.peppol.reporting.api.backend.IPeppolReportingBackendSPI;
import com.helger.peppol.reporting.api.backend.PeppolReportingBackendException;
import com.helger.peppolid.CIdentifier;

/**
 * SPI implementation of {@link IPeppolReportingBackendSPI} that keeps all reporting items in
 * memory. Intended for testing only — data is lost on JVM restart.
 *
 * @author Philip Helger
 */
@IsSPIImplementation
public class PeppolReportingBackendInMemorySPI implements IPeppolReportingBackendSPI
{
  private static final Logger LOGGER = LoggerFactory.getLogger (PeppolReportingBackendInMemorySPI.class);

  private final SimpleReadWriteLock m_aRWLock = new SimpleReadWriteLock ();
  @GuardedBy ("m_aRWLock")
  private final ICommonsMap <LocalDate, ICommonsList <PeppolReportingItem>> m_aMap = new CommonsHashMap <> ();
  @GuardedBy ("m_aRWLock")
  private boolean m_bInitialized = false;

  @NonNull
  @Nonempty
  public String getDisplayName ()
  {
    return "InMemory";
  }

  @NonNull
  public ESuccess initBackend (@NonNull final IConfig aConfig)
  {
    m_aRWLock.writeLocked (() -> m_bInitialized = true);
    return ESuccess.SUCCESS;
  }

  public boolean isInitialized ()
  {
    return m_aRWLock.readLockedBoolean (() -> m_bInitialized);
  }

  public void shutdownBackend ()
  {
    m_aRWLock.writeLocked (() -> {
      m_aMap.clear ();
      m_bInitialized = false;
    });
  }

  public void storeReportingItem (@NonNull @Nonempty final String sItemID,
                                  @NonNull final PeppolReportingItem aReportingItem) throws PeppolReportingBackendException
  {
    ValueEnforcer.notEmpty (sItemID, "ItemID");
    ValueEnforcer.notNull (aReportingItem, "ReportingItem");

    if (PeppolReportingHelper.isDocumentTypeEligableForReporting (aReportingItem.getDocTypeIDScheme (),
                                                                  aReportingItem.getDocTypeIDValue ()))
    {
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Trying to store Peppol Reporting Item in memory");

      if (!isInitialized ())
        throw new IllegalStateException ("The Peppol Reporting InMemory backend is not initialized");

      m_aRWLock.writeLocked (() -> m_aMap.computeIfAbsent (aReportingItem.getExchangeDTUTC ().toLocalDate (),
                                                           k -> new CommonsArrayList <> ()).add (aReportingItem));

      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Successfully stored Peppol Reporting Item in memory");
    }
    else
    {
      LOGGER.info ("Not storing Peppol Reporting Item in memory, as the document type is not eligable for reporting (" +
                   CIdentifier.getURIEncoded (aReportingItem.getDocTypeIDScheme (),
                                              aReportingItem.getDocTypeIDValue ()) +
                   ")");
    }
  }

  public void forEachReportingItem (@NonNull final LocalDate aStartDateIncl,
                                    @NonNull final LocalDate aEndDateIncl,
                                    @NonNull final Consumer <? super PeppolReportingItem> aConsumer) throws PeppolReportingBackendException
  {
    ValueEnforcer.notNull (aStartDateIncl, "StartDateIncl");
    ValueEnforcer.notNull (aEndDateIncl, "EndDateIncl");
    ValueEnforcer.isTrue (() -> aEndDateIncl.compareTo (aStartDateIncl) >= 0, "EndDateIncl must be >= StartDateIncl");
    ValueEnforcer.notNull (aConsumer, "Consumer");

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Querying Peppol Reporting Items from in memory between " +
                    aStartDateIncl +
                    " and " +
                    aEndDateIncl);

    if (!isInitialized ())
      throw new IllegalStateException ("The Peppol Reporting InMemory backend is not initialized");

    LocalDate aCurDate = aStartDateIncl;
    while (aCurDate.compareTo (aEndDateIncl) <= 0)
    {
      // Copy the items of one day while holding the lock, so that the consumer
      // is invoked outside of the lock
      final LocalDate aDate = aCurDate;
      final ICommonsList <PeppolReportingItem> aAllItemsOfDate = m_aRWLock.readLockedGet ( () -> {
        final ICommonsList <PeppolReportingItem> aList = m_aMap.get (aDate);
        return aList == null ? null : aList.getClone ();
      });
      if (aAllItemsOfDate != null)
        aAllItemsOfDate.forEach (aConsumer);

      aCurDate = aCurDate.plusDays (1);
    }
  }

  @NonNull
  public Iterable <PeppolReportingItem> iterateReportingItems (@NonNull final LocalDate aStartDateIncl,
                                                               @NonNull final LocalDate aEndDateIncl) throws PeppolReportingBackendException
  {
    final ICommonsList <PeppolReportingItem> ret = new CommonsArrayList <> ();
    forEachReportingItem (aStartDateIncl, aEndDateIncl, ret::add);
    return ret;
  }
}
