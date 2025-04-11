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

import com.helger.config.IConfig;
import com.helger.db.api.flyway.FlywayConfigurationBuilderConfig;

/**
 * The specific Flyway Configuration builder for Peppol Reporting.
 *
 * @author Philip Helger
 * @since 3.1.0
 */
public class ReportingFlywayConfigurationBuilder extends FlywayConfigurationBuilderConfig
{
  public static final String FLYWAY_CONFIG_PREFIX = "peppol.reporting.flyway.";

  public ReportingFlywayConfigurationBuilder (@Nonnull final IConfig aConfig,
                                              @Nonnull final ReportingJdbcConfiguration aJdbcConfig)
  {
    super (aConfig, FLYWAY_CONFIG_PREFIX);

    // Fallback to other configuration values
    if (jdbcUrl () == null)
      jdbcUrl (aJdbcConfig.getJdbcUrl ());
    if (jdbcUser () == null)
      jdbcUser (aJdbcConfig.getJdbcUser ());
    if (jdbcPassword () == null)
      jdbcPassword (aJdbcConfig.getJdbcPassword ());
  }
}
