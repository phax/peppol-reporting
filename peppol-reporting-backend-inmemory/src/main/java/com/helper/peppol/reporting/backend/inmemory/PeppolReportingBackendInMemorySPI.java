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
package com.helper.peppol.reporting.backend.inmemory;

import java.time.LocalDate;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.IsSPIImplementation;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.CommonsHashMap;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsMap;
import com.helger.commons.concurrent.SimpleReadWriteLock;
import com.helger.commons.state.ESuccess;
import com.helger.config.IConfig;
import com.helper.peppol.reporting.api.PeppolReportingItem;
import com.helper.peppol.reporting.api.backend.IPeppolReportingBackendSPI;
import com.helper.peppol.reporting.api.backend.PeppolReportingBackendException;

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

  @Nonnull
  @Nonempty
  public String getDisplayName ()
  {
    return "InMemory";
  }

  @Nonnull
  public ESuccess initBackend (@Nonnull final IConfig aConfig)
  {
    return ESuccess.SUCCESS;
  }

  public boolean isInitialized ()
  {
    return true;
  }

  public void shutdownBackend ()
  {}

  public void storeReportingItem (@Nonnull final PeppolReportingItem aReportingItem) throws PeppolReportingBackendException
  {
    ValueEnforcer.notNull (aReportingItem, "ReportingItem");

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Trying to store Peppol Reporting Item in memory");

    m_aRWLock.writeLocked ( () -> m_aMap.computeIfAbsent (aReportingItem.getExchangeDTUTC ().toLocalDate (),
                                                          k -> new CommonsArrayList <> ())
                                        .add (aReportingItem));

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Successfully stored Peppol Reporting Item in memory");
  }

  public void forEachReportingItem (@Nonnull final LocalDate aStartDateIncl,
                                    @Nonnull final LocalDate aEndDateIncl,
                                    @Nonnull final Consumer <? super PeppolReportingItem> aConsumer) throws PeppolReportingBackendException
  {
    ValueEnforcer.notNull (aStartDateIncl, "StartDateIncl");
    ValueEnforcer.notNull (aEndDateIncl, "EndDateIncl");
    ValueEnforcer.isTrue ( () -> aEndDateIncl.compareTo (aStartDateIncl) >= 0, "EndDateIncl must be >= StartDateIncl");
    ValueEnforcer.notNull (aConsumer, "Consumer");

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Querying Peppol Reporting Items from in memory between " +
                    aStartDateIncl +
                    " and " +
                    aEndDateIncl);

    m_aRWLock.readLocked ( () -> {
      int nCounter = 0;

      // Find between date, but order by exchange date and time
      LocalDate aCurDate = aStartDateIncl;
      while (aCurDate.compareTo (aEndDateIncl) <= 0)
      {
        final ICommonsList <PeppolReportingItem> aAllItemsOfDay = m_aMap.get (aCurDate);
        if (aAllItemsOfDay != null)
        {
          aAllItemsOfDay.forEach (aConsumer);
          nCounter += aAllItemsOfDay.size ();
        }

        aCurDate = aCurDate.plusDays (1);
      }

      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Found a total of " + nCounter + " matching documents in memory");
    });
  }
}
