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
package com.helper.peppol.reporting.backend.redis;

import java.time.LocalDate;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;

import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.IsSPIImplementation;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.OverrideOnDemand;
import com.helger.commons.concurrent.SimpleReadWriteLock;
import com.helger.commons.mutable.MutableInt;
import com.helger.commons.state.ESuccess;
import com.helger.commons.string.StringHelper;
import com.helger.config.IConfig;
import com.helper.peppol.reporting.api.PeppolReportingItem;
import com.helper.peppol.reporting.api.backend.IPeppolReportingBackendSPI;
import com.helper.peppol.reporting.api.backend.PeppolReportingBackendException;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;

import redis.clients.jedis.JedisPool;

/**
 * SPI implementation of {@link IPeppolReportingBackendSPI} for Redis.
 *
 * @author Philip Helger
 */
@IsSPIImplementation
public class PeppolReportingBackendRedisSPI implements IPeppolReportingBackendSPI
{
  public static final String CONFIG_PEPPOL_REPORTING_REDIS_HOST = "peppol.reporting.redis.host";
  public static final String CONFIG_PEPPOL_REPORTING_REDIS_PORT = "peppol.reporting.redis.port";
  public static final int DEFAULT_REDIS_PORT = 6379;

  private static final Logger LOGGER = LoggerFactory.getLogger (PeppolReportingBackendRedisSPI.class);

  private final SimpleReadWriteLock m_aRWLock = new SimpleReadWriteLock ();
  @GuardedBy ("m_aRWLock")
  private JedisPool m_aPool;

  @Nonnull
  @Nonempty
  public String getDisplayName ()
  {
    return "Redis[Jedis]";
  }

  @Nullable
  @OverrideOnDemand
  protected JedisPool createJedisPool (@Nonnull final IConfig aConfig)
  {
    final String sHost = aConfig.getAsString (CONFIG_PEPPOL_REPORTING_REDIS_HOST);
    if (StringHelper.hasNoText (sHost))
    {
      LOGGER.error ("The Redis hostname is missing in the configuration. See property '" +
                    CONFIG_PEPPOL_REPORTING_REDIS_HOST +
                    "'");
      return null;
    }

    final int nPort = aConfig.getAsInt (CONFIG_PEPPOL_REPORTING_REDIS_PORT, DEFAULT_REDIS_PORT);
    if (nPort < 0)
    {
      LOGGER.error ("The Redis port name is missing in the configuration. See property '" +
                    CONFIG_PEPPOL_REPORTING_REDIS_PORT +
                    "'");
      return null;
    }

    LOGGER.info ("Using Peppol Reporting Redis at '" + sHost + ":" + nPort + "'");
    return new JedisPool (sHost, nPort);
  }

  @Nonnull
  public ESuccess initBackend (@Nonnull final IConfig aConfig)
  {
    m_aRWLock.writeLocked ( () -> {
      if (m_aPool != null)
        throw new IllegalStateException ("The Peppol Reporting Redis backend was already initialized");

      m_aPool = createJedisPool (aConfig);
    });

    if (!isInitialized ())
    {
      // Error was already logged
      return ESuccess.FAILURE;
    }

    return ESuccess.SUCCESS;
  }

  public boolean isInitialized ()
  {
    return m_aRWLock.readLockedBoolean ( () -> m_aPool != null);
  }

  public void shutdownBackend ()
  {
    if (isInitialized ())
    {
      m_aRWLock.writeLocked ( () -> {
        LOGGER.info ("Shutting down Peppol Reporting Redis client");
        m_aPool.close ();
        m_aPool = null;
      });
    }
    else
      LOGGER.warn ("The Peppol Reporting Redis backend cannot be shutdown, because it was never properly initialized");
  }

  public void storeReportingItem (@Nonnull final PeppolReportingItem aReportingItem) throws PeppolReportingBackendException
  {
    ValueEnforcer.notNull (aReportingItem, "ReportingItem");

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Trying to store Peppol Reporting Item in Redis");

    if (!isInitialized ())
      throw new IllegalStateException ("The Peppol Reporting Redis backend is not initialized");
    // Write to collection
    if (!_getCollection ().insertOne (PeppolReportingMongoDBHelper.toBson (aReportingItem)).wasAcknowledged ())
      throw new IllegalStateException ("Failed to insert into Peppol Reporting MongoDB Collection");

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Successfully stored Peppol Reporting Item in MongoDB");
  }

  public void forEachReportingItem (@Nonnull final LocalDate aStartDateIncl,
                                    @Nonnull final LocalDate aEndDateIncl,
                                    @Nonnull final Consumer <? super PeppolReportingItem> aConsumer) throws PeppolReportingBackendException
  {
    ValueEnforcer.notNull (aStartDateIncl, "StartDateIncl");
    ValueEnforcer.notNull (aEndDateIncl, "EndDateIncl");
    ValueEnforcer.notNull (aConsumer, "Consumer");

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Querying Peppol Reporting Items from MongoDB between " + aStartDateIncl + " and " + aEndDateIncl);

    if (!isInitialized ())
      throw new IllegalStateException ("The Peppol Reporting MongoDB backend is not initialized");

    // Find between date, but order by exchange date and time
    final Bson aFilter = Filters.and (Filters.gte (PeppolReportingMongoDBHelper.BSON_EXCHANGEDATE, aStartDateIncl),
                                      Filters.lte (PeppolReportingMongoDBHelper.BSON_EXCHANGEDATE, aEndDateIncl));

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Using MongoDB filter '" + aFilter.toBsonDocument ().toJson () + "'");

    final MutableInt aCounter = new MutableInt (0);
    _getCollection ().find (aFilter)
                     .sort (Sorts.ascending (PeppolReportingMongoDBHelper.BSON_EXCHANGEDT))
                     .forEach (x -> {
                       aCounter.inc ();
                       aConsumer.accept (PeppolReportingMongoDBHelper.toDomain (x));
                     });

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Found a total of " + aCounter.intValue () + " matching documents in MongoDB");
  }
}
