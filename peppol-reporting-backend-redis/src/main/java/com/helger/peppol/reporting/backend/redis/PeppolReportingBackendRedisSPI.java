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

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.annotation.style.IsSPIImplementation;
import com.helger.annotation.style.OverrideOnDemand;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.string.StringHelper;
import com.helger.config.IConfig;
import com.helger.peppol.reporting.api.backend.IPeppolReportingBackendProviderSPI;
import com.helger.peppol.reporting.api.backend.PeppolReportingBackendException;

import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.RedisClient;
import redis.clients.jedis.exceptions.JedisException;

/**
 * SPI implementation of {@link IPeppolReportingBackendProviderSPI} that creates
 * {@link PeppolReportingBackendRedis} instances.
 *
 * @author Philip Helger
 */
@IsSPIImplementation
public class PeppolReportingBackendRedisSPI implements IPeppolReportingBackendProviderSPI
{
  public static final String BACKEND_ID = "redis";

  public static final String CONFIG_PEPPOL_REPORTING_REDIS_HOST = "peppol.reporting.redis.host";
  public static final String CONFIG_PEPPOL_REPORTING_REDIS_PORT = "peppol.reporting.redis.port";
  public static final String CONFIG_PEPPOL_REPORTING_REDIS_USER = "peppol.reporting.redis.user";
  public static final String CONFIG_PEPPOL_REPORTING_REDIS_PASSWORD = "peppol.reporting.redis.password";
  public static final int DEFAULT_REDIS_PORT = 6379;

  private static final Logger LOGGER = LoggerFactory.getLogger (PeppolReportingBackendRedisSPI.class);

  @NonNull
  @Nonempty
  public String getBackendID ()
  {
    return BACKEND_ID;
  }

  @NonNull
  @Nonempty
  public String getDisplayName ()
  {
    return PeppolReportingBackendRedis.DISPLAY_NAME;
  }

  /**
   * Create the Redis client based on the configuration.
   *
   * @param aConfig
   *        The configuration to use. Never <code>null</code>.
   * @return The Redis client and never <code>null</code>.
   * @throws PeppolReportingBackendException
   *         If the configuration is invalid
   * @since 5.0.0 - before it was called <code>createJedisPool</code>
   */
  @NonNull
  @OverrideOnDemand
  protected RedisClient createRedisClient (@NonNull final IConfig aConfig) throws PeppolReportingBackendException
  {
    final String sHost = aConfig.getAsString (CONFIG_PEPPOL_REPORTING_REDIS_HOST);
    if (StringHelper.isEmpty (sHost))
      throw new PeppolReportingBackendException ("The Redis host name is missing in the configuration. See property '" +
                                                 CONFIG_PEPPOL_REPORTING_REDIS_HOST +
                                                 "'");

    final int nPort = aConfig.getAsInt (CONFIG_PEPPOL_REPORTING_REDIS_PORT, DEFAULT_REDIS_PORT);
    if (nPort < 0)
      throw new PeppolReportingBackendException ("The Redis port is invalid in the configuration. See property '" +
                                                 CONFIG_PEPPOL_REPORTING_REDIS_PORT +
                                                 "'");

    final String sUserName = aConfig.getAsString (CONFIG_PEPPOL_REPORTING_REDIS_USER);
    final String sPassword = aConfig.getAsString (CONFIG_PEPPOL_REPORTING_REDIS_PASSWORD);

    LOGGER.info ("Using Peppol Reporting Redis at '" +
                 sHost +
                 ":" +
                 nPort +
                 "'" +
                 (StringHelper.isNotEmpty (sUserName) ? " using username '" + sUserName + "'" : ""));

    return RedisClient.builder ()
                      .hostAndPort (sHost, nPort)
                      .clientConfig (DefaultJedisClientConfig.builder ().user (sUserName).password (sPassword).build ())
                      .build ();
  }

  @NonNull
  public PeppolReportingBackendRedis createBackend (@NonNull final IConfig aConfig) throws PeppolReportingBackendException
  {
    ValueEnforcer.notNull (aConfig, "Config");

    final RedisClient aRedisClient = createRedisClient (aConfig);

    // Check connectivity
    try
    {
      aRedisClient.ping ();
    }
    catch (final JedisException ex)
    {
      aRedisClient.close ();
      throw new PeppolReportingBackendException ("Failed to connect to the Peppol Reporting Redis backend", ex);
    }

    return new PeppolReportingBackendRedis (aRedisClient);
  }
}
