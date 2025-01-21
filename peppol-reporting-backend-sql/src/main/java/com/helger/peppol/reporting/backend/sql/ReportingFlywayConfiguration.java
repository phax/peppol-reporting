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

/**
 * Peppol Reporting Flyway configuration
 *
 * @author Philip Helger
 */
@Immutable
public final class ReportingFlywayConfiguration
{
  private static final String CONFIG_PREFIX = "peppol.reporting.flyway.";

  public static final String CONFIG_FLYWAY_ENABLED = CONFIG_PREFIX + "enabled";
  private static final boolean DEFAULT_FLYWAY_ENABLED = true;

  private static final String CONFIG_FLYWAY_JDBC_URL = CONFIG_PREFIX + "jdbc.url";
  private static final String CONFIG_FLYWAY_JDBC_USER = CONFIG_PREFIX + "jdbc.user";
  private static final String CONFIG_FLYWAY_JDBC_PASSWORD = CONFIG_PREFIX + "jdbc.password";

  private static final String CONFIG_FLYWAY_JDBC_SCHEMA_CREATE = CONFIG_PREFIX + "jdbc.schema-create";
  private static final boolean DEFAULT_FLYWAY_JDBC_SCHEMA_CREATE = false;

  private static final String CONFIG_FLYWAY_BASELINE_VERSION = CONFIG_PREFIX + "baseline.version";
  private static final int DEFAULT_FLYWAY_BASELINE_VERSION = 0;

  @PresentForCodeCoverage
  private static final ReportingFlywayConfiguration INSTANCE = new ReportingFlywayConfiguration ();

  private ReportingFlywayConfiguration ()
  {}

  public static boolean isFlywayEnabled (@Nonnull final IConfig aConfig)
  {
    return aConfig.getAsBoolean (CONFIG_FLYWAY_ENABLED, DEFAULT_FLYWAY_ENABLED);
  }

  @Nullable
  public static String getFlywayJdbcUrl (@Nonnull final IConfig aConfig)
  {
    final String ret = aConfig.getAsString (CONFIG_FLYWAY_JDBC_URL);
    return ret != null ? ret : ReportingJdbcConfiguration.getJdbcUrl (aConfig);
  }

  @Nullable
  public static String getFlywayJdbcUser (@Nonnull final IConfig aConfig)
  {
    final String ret = aConfig.getAsString (CONFIG_FLYWAY_JDBC_USER);
    return ret != null ? ret : ReportingJdbcConfiguration.getJdbcUser (aConfig);
  }

  @Nullable
  public static String getFlywayJdbcPassword (@Nonnull final IConfig aConfig)
  {
    final String ret = aConfig.getAsString (CONFIG_FLYWAY_JDBC_PASSWORD);
    return ret != null ? ret : ReportingJdbcConfiguration.getJdbcPassword (aConfig);
  }

  public static boolean isFlywaySchemaCreate (@Nonnull final IConfig aConfig)
  {
    return aConfig.getAsBoolean (CONFIG_FLYWAY_JDBC_SCHEMA_CREATE, DEFAULT_FLYWAY_JDBC_SCHEMA_CREATE);
  }

  public static int getFlywayBaselineVersion (@Nonnull final IConfig aConfig)
  {
    return aConfig.getAsInt (CONFIG_FLYWAY_BASELINE_VERSION, DEFAULT_FLYWAY_BASELINE_VERSION);
  }
}
