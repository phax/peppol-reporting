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
package com.helger.peppol.reporting.backend.redis;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.ThreadSafe;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.string.StringHelper;
import com.helger.peppol.reporting.api.PeppolReportingItem;
import com.helger.peppol.reporting.api.backend.AbstractPeppolReportingBackendWithReader;
import com.helger.peppol.reporting.api.backend.PeppolReportingBackendException;

import redis.clients.jedis.AbstractTransaction;
import redis.clients.jedis.RedisClient;
import redis.clients.jedis.exceptions.JedisException;

/**
 * Peppol Reporting backend that stores the reporting items in Redis.
 *
 * @author Philip Helger
 * @since 5.0.0
 */
@ThreadSafe
public class PeppolReportingBackendRedis extends AbstractPeppolReportingBackendWithReader
{
  public static final String DISPLAY_NAME = "Redis[Jedis]";

  private static final Logger LOGGER = LoggerFactory.getLogger (PeppolReportingBackendRedis.class);

  private final RedisClient m_aRedisClient;

  /**
   * Constructor.
   *
   * @param aRedisClient
   *        The Redis client to use. May not be <code>null</code>. It is closed together with this
   *        backend.
   */
  public PeppolReportingBackendRedis (@NonNull final RedisClient aRedisClient)
  {
    super (DISPLAY_NAME);
    ValueEnforcer.notNull (aRedisClient, "RedisClient");
    m_aRedisClient = aRedisClient;
  }

  @NonNull
  @Nonempty
  private static String _getDayKey (@NonNull final LocalDate aDate)
  {
    return StringHelper.getLeadingZero (aDate.getYear (), 4) +
           StringHelper.getLeadingZero (aDate.getMonthValue (), 2) +
           StringHelper.getLeadingZero (aDate.getDayOfMonth (), 2);
  }

  @Override
  protected void storeEligibleReportingItem (@NonNull @Nonempty final String sItemID,
                                             @NonNull final PeppolReportingItem aReportingItem) throws PeppolReportingBackendException
  {
    try
    {
      // Get new unique ID
      final long nID = m_aRedisClient.incr ("peppol:reporting:itemidx");

      try (final AbstractTransaction t = m_aRedisClient.multi ())
      {
        // Store main data
        final String sMapKey = "peppol:reporting:item:" + nID;
        t.hset (sMapKey, PeppolReportingRedisHelper.toMap (aReportingItem));

        // Append reference to list of entries per day. RPUSH (not LPUSH) is
        // used so that subsequent LRANGE 0 -1 reads return the items in
        // insertion order, matching the behaviour of the other backends.
        t.rpush ("peppol:reporting:" + _getDayKey (aReportingItem.getExchangeDTUTC ().toLocalDate ()), sMapKey);
        t.exec ();
      }
    }
    catch (final JedisException ex)
    {
      throw new PeppolReportingBackendException ("Failed to store Peppol Reporting Item in Redis", ex);
    }
  }

  @Override
  protected void forEachReportingItemInRange (@NonNull final LocalDate aStartDateIncl,
                                              @NonNull final LocalDate aEndDateIncl,
                                              @NonNull final Consumer <? super PeppolReportingItem> aConsumer)
  {
    int nCounter = 0;
    LocalDate aCurDate = aStartDateIncl;
    while (aCurDate.compareTo (aEndDateIncl) <= 0)
    {
      final String sListKey = "peppol:reporting:" + _getDayKey (aCurDate);
      final List <String> aAllHashKeys = m_aRedisClient.lrange (sListKey, 0, -1);
      for (final String sKey : aAllHashKeys)
      {
        final Map <String, String> aHashMap = m_aRedisClient.hgetAll (sKey);
        aConsumer.accept (PeppolReportingRedisHelper.toDomain (aHashMap));
        nCounter++;
      }

      // Next day
      aCurDate = aCurDate.plusDays (1);
    }

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Found a total of " + nCounter + " matching documents in Redis");
  }

  @Override
  protected void onClose ()
  {
    m_aRedisClient.close ();
  }
}
