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
import java.util.Iterator;

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
 * SPI implementation of {@link IPeppolReportingBackendSPI} for Redis.
 *
 * @author Philip Helger
 */
@IsSPIImplementation
public class PeppolReportingBackendInMemorySPI implements IPeppolReportingBackendSPI
{
  public static final String CONFIG_PEPPOL_REPORTING_REDIS_HOST = "peppol.reporting.redis.host";
  public static final String CONFIG_PEPPOL_REPORTING_REDIS_PORT = "peppol.reporting.redis.port";
  public static final int DEFAULT_REDIS_PORT = 6379;

  private static final Logger LOGGER = LoggerFactory.getLogger (PeppolReportingBackendInMemorySPI.class);

  private final SimpleReadWriteLock m_aRWLock = new SimpleReadWriteLock ();
  @GuardedBy ("m_aRWLock")
  private final ICommonsMap <LocalDate, ICommonsList <PeppolReportingItem>> m_aMap = new CommonsHashMap <> ();

  @NonNull
  @Nonempty
  public String getDisplayName ()
  {
    return "InMemory";
  }

  @NonNull
  public ESuccess initBackend (@NonNull final IConfig aConfig)
  {
    return ESuccess.SUCCESS;
  }

  public boolean isInitialized ()
  {
    return true;
  }

  public void shutdownBackend ()
  {}

  public void storeReportingItem (@NonNull final PeppolReportingItem aReportingItem) throws PeppolReportingBackendException
  {
    ValueEnforcer.notNull (aReportingItem, "ReportingItem");

    if (PeppolReportingHelper.isDocumentTypeEligableForReporting (aReportingItem.getDocTypeIDScheme (),
                                                                  aReportingItem.getDocTypeIDValue ()))
    {
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Trying to store Peppol Reporting Item in memory");

      m_aRWLock.writeLocked ( () -> m_aMap.computeIfAbsent (aReportingItem.getExchangeDTUTC ().toLocalDate (),
                                                            k -> new CommonsArrayList <> ())
                                          .add (aReportingItem));

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

  @NonNull
  public Iterable <PeppolReportingItem> iterateReportingItems (@NonNull final LocalDate aStartDateIncl,
                                                               @NonNull final LocalDate aEndDateIncl) throws PeppolReportingBackendException
  {
    ValueEnforcer.notNull (aStartDateIncl, "StartDateIncl");
    ValueEnforcer.notNull (aEndDateIncl, "EndDateIncl");
    ValueEnforcer.isTrue ( () -> aEndDateIncl.compareTo (aStartDateIncl) >= 0, "EndDateIncl must be >= StartDateIncl");

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Querying Peppol Reporting Items from in memory between " +
                    aStartDateIncl +
                    " and " +
                    aEndDateIncl);

    final Iterator <PeppolReportingItem> it = new Iterator <> ()
    {
      private LocalDate m_aCurDate = aStartDateIncl;
      private ICommonsList <PeppolReportingItem> m_aAllItemsOfDate;
      private int m_nDateIndex = 0;

      private void _findNextDayWithItems ()
      {
        m_nDateIndex = 0;

        // Find between date, but order by exchange date and time
        while (m_aCurDate.compareTo (aEndDateIncl) <= 0)
        {
          m_aAllItemsOfDate = m_aRWLock.readLockedGet ( () -> m_aMap.get (m_aCurDate));
          if (m_aAllItemsOfDate != null && m_aAllItemsOfDate.isNotEmpty ())
            break;

          m_aCurDate = m_aCurDate.plusDays (1);
        }
      }

      public boolean hasNext ()
      {
        if (m_aAllItemsOfDate == null)
        {
          // Initial call
          _findNextDayWithItems ();
          if (m_aAllItemsOfDate == null)
            return false;
        }

        if (m_nDateIndex >= m_aAllItemsOfDate.size ())
        {
          // Next day
          m_aCurDate = m_aCurDate.plusDays (1);
          m_aAllItemsOfDate = null;

          _findNextDayWithItems ();
          if (m_aAllItemsOfDate == null)
            return false;
        }

        return true;
      }

      @NonNull
      public PeppolReportingItem next ()
      {
        final PeppolReportingItem ret = m_aAllItemsOfDate.get (m_nDateIndex);
        m_nDateIndex++;
        return ret;
      }
    };
    return () -> it;
  }
}
