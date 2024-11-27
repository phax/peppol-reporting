/*
 * Copyright (C) 2022-2024 Philip Helger
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
package com.helger.peppol.reporting.backend.redis;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ELockType;
import com.helger.commons.annotation.IsSPIImplementation;
import com.helger.commons.annotation.MustBeLocked;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.OverrideOnDemand;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.concurrent.SimpleReadWriteLock;
import com.helger.commons.state.ESuccess;
import com.helger.commons.string.StringHelper;
import com.helger.config.IConfig;
import com.helger.peppol.reporting.api.PeppolReportingHelper;
import com.helger.peppol.reporting.api.PeppolReportingItem;
import com.helger.peppol.reporting.api.backend.IPeppolReportingBackendSPI;
import com.helger.peppol.reporting.api.backend.PeppolReportingBackendException;
import com.helger.peppolid.CIdentifier;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.exceptions.JedisException;

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
  public static final String CONFIG_PEPPOL_REPORTING_REDIS_USER = "peppol.reporting.redis.user";
  public static final String CONFIG_PEPPOL_REPORTING_REDIS_PASSWORD = "peppol.reporting.redis.password";
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
      LOGGER.error ("The Redis host name is missing in the configuration. See property '" +
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

    final String sUserName = aConfig.getAsString (CONFIG_PEPPOL_REPORTING_REDIS_USER);
    final String sPassword = aConfig.getAsString (CONFIG_PEPPOL_REPORTING_REDIS_PASSWORD);

    LOGGER.info ("Using Peppol Reporting Redis at '" +
                 sHost +
                 ":" +
                 nPort +
                 "'" +
                 (StringHelper.hasText (sUserName) ? " using username '" + sUserName + "'" : ""));

    return new JedisPool (sHost, nPort, sUserName, sPassword);
  }

  @Nonnull
  public ESuccess initBackend (@Nonnull final IConfig aConfig)
  {
    m_aRWLock.writeLocked ( () -> {
      if (m_aPool != null)
        throw new IllegalStateException ("The Peppol Reporting Redis backend was already initialized");

      m_aPool = createJedisPool (aConfig);
    });

    final JedisPool aPool = m_aRWLock.readLockedGet ( () -> m_aPool);
    if (aPool == null)
      return ESuccess.FAILURE;

    // Check connectivity
    try (final Jedis aJedis = aPool.getResource ())
    {
      aJedis.ping ();
    }
    catch (final JedisException ex)
    {
      LOGGER.error ("Failed to connect to the Peppol Reporting Redis backend", ex);
      // Reset pool in case of error
      m_aRWLock.writeLocked (this::_shutdown);
      return ESuccess.FAILURE;
    }

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

  @MustBeLocked (ELockType.WRITE)
  private void _shutdown ()
  {
    m_aPool.close ();
    m_aPool = null;
  }

  public void shutdownBackend ()
  {
    if (isInitialized ())
    {
      m_aRWLock.writeLocked ( () -> {
        LOGGER.info ("Shutting down Peppol Reporting Redis client");
        _shutdown ();
      });
    }
    else
      LOGGER.warn ("The Peppol Reporting Redis backend cannot be shutdown, because it was never properly initialized");
  }

  @Nonnull
  @Nonempty
  private static String _getDayKey (@Nonnull final LocalDate aDate)
  {
    return StringHelper.getLeadingZero (aDate.getYear (), 4) +
           StringHelper.getLeadingZero (aDate.getMonthValue (), 2) +
           StringHelper.getLeadingZero (aDate.getDayOfMonth (), 2);
  }

  public void storeReportingItem (@Nonnull final PeppolReportingItem aReportingItem) throws PeppolReportingBackendException
  {
    ValueEnforcer.notNull (aReportingItem, "ReportingItem");

    if (PeppolReportingHelper.isDocumentTypeEligableForReporting (aReportingItem.getDocTypeIDScheme (),
                                                                  aReportingItem.getDocTypeIDValue ()))
    {
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Trying to store Peppol Reporting Item in Redis");

      if (!isInitialized ())
        throw new IllegalStateException ("The Peppol Reporting Redis backend is not initialized");

      try (final Jedis aJedis = m_aPool.getResource ())
      {
        // Get new unique ID
        final long nID = aJedis.incr ("peppol:reporting:itemidx");

        final Transaction t = aJedis.multi ();

        // Store main data
        final String sMapKey = "peppol:reporting:item:" + nID;
        t.hset (sMapKey, PeppolReportingRedisHelper.toMap (aReportingItem));

        // add reference to list of entries per day
        t.lpush ("peppol:reporting:" + _getDayKey (aReportingItem.getExchangeDTUTC ().toLocalDate ()), sMapKey);
        t.exec ();
      }
      catch (final JedisException ex)
      {
        LOGGER.error ("Failed to store Peppol Reporting Item in Redis: " + ex.getMessage ());
        throw new PeppolReportingBackendException ("Failed to store Peppol Reporting Item in Redis", ex);
      }

      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Successfully stored Peppol Reporting Item in Redis");
    }
    else
    {
      LOGGER.info ("Not storing Peppol Reporting Item in Redis, as the document type is not eligable for reporting (" +
                   CIdentifier.getURIEncoded (aReportingItem.getDocTypeIDScheme (),
                                              aReportingItem.getDocTypeIDValue ()) +
                   ")");
    }
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
      LOGGER.debug ("Querying Peppol Reporting Items from Redis between " + aStartDateIncl + " and " + aEndDateIncl);

    if (!isInitialized ())
      throw new IllegalStateException ("The Peppol Reporting Redis backend is not initialized");

    int nCounter = 0;
    try (final Jedis aJedis = m_aPool.getResource ())
    {
      // Find between date, but order by exchange date and time
      LocalDate aCurDate = aStartDateIncl;
      while (aCurDate.compareTo (aEndDateIncl) <= 0)
      {
        final String sListKey = "peppol:reporting:" + _getDayKey (aCurDate);
        final List <String> aAllHashKeys = aJedis.lrange (sListKey, 0, -1);
        for (final String sKey : aAllHashKeys)
        {
          final Map <String, String> aHashMap = aJedis.hgetAll (sKey);
          final PeppolReportingItem aReportingItem = PeppolReportingRedisHelper.toDomain (aHashMap);
          aConsumer.accept (aReportingItem);

          nCounter++;
        }

        aCurDate = aCurDate.plusDays (1);
      }
    }

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Found a total of " + nCounter + " matching documents in Redis");
  }

  @Nonnull
  public Iterable <PeppolReportingItem> iterateReportingItems (@Nonnull final LocalDate aStartDateIncl,
                                                               @Nonnull final LocalDate aEndDateIncl) throws PeppolReportingBackendException
  {
    final ICommonsList <PeppolReportingItem> ret = new CommonsArrayList <> ();
    forEachReportingItem (aStartDateIncl, aEndDateIncl, ret::add);
    return ret;
  }
}
