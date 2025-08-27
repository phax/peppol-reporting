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

import com.helger.annotation.concurrent.Immutable;
import com.helger.config.IConfig;
import com.helger.db.api.config.JdbcConfigurationConfig;

import jakarta.annotation.Nonnull;

/**
 * Peppol Reporting JDBC configuration with lazy initialization.
 *
 * @author Philip Helger
 */
@Immutable
public class ReportingJdbcConfiguration extends JdbcConfigurationConfig
{
  public static final String CONFIG_PREFIX = "peppol.reporting.jdbc.";

  public ReportingJdbcConfiguration (@Nonnull final IConfig aConfig)
  {
    super (aConfig, CONFIG_PREFIX);
  }
}
