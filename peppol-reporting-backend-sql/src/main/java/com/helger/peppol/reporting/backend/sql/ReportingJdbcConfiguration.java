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
package com.helger.peppol.reporting.backend.sql;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.annotation.PresentForCodeCoverage;
import com.helger.config.IConfig;
import com.helger.db.jdbc.executor.DBExecutor;

/**
 * Default JDBC configuration file properties
 *
 * @author Philip Helger
 */
@Immutable
public final class ReportingJdbcConfiguration
{
  private static final String CONFIG_PREFIX = "peppol.reporting.jdbc.";

  public static final String CONFIG_JDBC_DATABASE_TYPE = CONFIG_PREFIX + "database-type";
  public static final String CONFIG_JDBC_DRIVER = CONFIG_PREFIX + "driver";
  public static final String CONFIG_JDBC_URL = CONFIG_PREFIX + "url";
  public static final String CONFIG_JDBC_USER = CONFIG_PREFIX + "user";
  public static final String CONFIG_JDBC_PASSWORD = CONFIG_PREFIX + "password";
  public static final String CONFIG_JDBC_SCHEMA = CONFIG_PREFIX + "schema";

  public static final String CONFIG_JDBC_EXECUTION_TIME_WARNING_ENABLED = CONFIG_PREFIX +
                                                                          "execution-time-warning.enabled";
  public static final boolean DEFAULT_JDBC_EXECUTION_TIME_WARNING_ENABLED = true;
  public static final String CONFIG_JDBC_EXECUTION_TIME_WARNING_MS = CONFIG_PREFIX + "execution-time-warning.ms";

  public static final String CONFIG_JDBC_DEBUG_CONNECTIONS = CONFIG_PREFIX + "debug.connections";
  public static final boolean DEFAULT_JDBC_DEBUG_CONNECTIONS = false;
  public static final String CONFIG_JDBC_DEBUG_TRANSACTIONS = CONFIG_PREFIX + "debug.transactions";
  public static final boolean DEFAULT_JDBC_DEBUG_TRANSACTIONS = false;
  public static final String CONFIG_JDBC_DEBUG_SQL = CONFIG_PREFIX + "debug.sql";
  public static final boolean DEFAULT_JDBC_DEBUG_SQL = false;

  @PresentForCodeCoverage
  private static final ReportingJdbcConfiguration INSTANCE = new ReportingJdbcConfiguration ();

  private ReportingJdbcConfiguration ()
  {}

  @Nullable
  public static String getJdbcDatabaseType (@Nonnull final IConfig aConfig)
  {
    return aConfig.getAsString (CONFIG_JDBC_DATABASE_TYPE);
  }

  @Nullable
  public static String getJdbcDriver (@Nonnull final IConfig aConfig)
  {
    return aConfig.getAsString (CONFIG_JDBC_DRIVER);
  }

  @Nullable
  public static String getJdbcUrl (@Nonnull final IConfig aConfig)
  {
    return aConfig.getAsString (CONFIG_JDBC_URL);
  }

  @Nullable
  public static String getJdbcUser (@Nonnull final IConfig aConfig)
  {
    return aConfig.getAsString (CONFIG_JDBC_USER);
  }

  @Nullable
  public static String getJdbcPassword (@Nonnull final IConfig aConfig)
  {
    return aConfig.getAsString (CONFIG_JDBC_PASSWORD);
  }

  @Nullable
  public static String getJdbcSchema (@Nonnull final IConfig aConfig)
  {
    return aConfig.getAsString (CONFIG_JDBC_SCHEMA);
  }

  public static boolean isJdbcExecutionTimeWarningEnabled (@Nonnull final IConfig aConfig)
  {
    return aConfig.getAsBoolean (CONFIG_JDBC_EXECUTION_TIME_WARNING_ENABLED,
                                 DEFAULT_JDBC_EXECUTION_TIME_WARNING_ENABLED);
  }

  public static long getJdbcExecutionTimeWarningMilliseconds (@Nonnull final IConfig aConfig)
  {
    return aConfig.getAsLong (CONFIG_JDBC_EXECUTION_TIME_WARNING_MS, DBExecutor.DEFAULT_EXECUTION_DURATION_WARN_MS);
  }

  public static boolean isJdbcDebugConnections (@Nonnull final IConfig aConfig)
  {
    return aConfig.getAsBoolean (CONFIG_JDBC_DEBUG_CONNECTIONS, DEFAULT_JDBC_DEBUG_CONNECTIONS);
  }

  public static boolean isJdbcDebugTransaction (@Nonnull final IConfig aConfig)
  {
    return aConfig.getAsBoolean (CONFIG_JDBC_DEBUG_TRANSACTIONS, DEFAULT_JDBC_DEBUG_TRANSACTIONS);
  }

  public static boolean isJdbcDebugSQL (@Nonnull final IConfig aConfig)
  {
    return aConfig.getAsBoolean (CONFIG_JDBC_DEBUG_SQL, DEFAULT_JDBC_DEBUG_SQL);
  }
}
