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
package com.helger.peppol.reporting.backend.sql;

import java.util.EnumSet;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.annotation.style.IsSPIImplementation;
import com.helger.annotation.style.OverrideOnDemand;
import com.helger.annotation.style.UsedViaReflection;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.string.StringImplode;
import com.helger.config.IConfig;
import com.helger.db.api.EDatabaseSystemType;
import com.helger.db.api.config.IJdbcConfiguration;
import com.helger.db.api.helper.DBSystemHelper;
import com.helger.db.flyway.FlywayConfiguration;
import com.helger.db.jdbc.DataSourceProviderFromJdbcConfiguration;
import com.helger.peppol.reporting.api.backend.IPeppolReportingBackendProviderSPI;
import com.helger.peppol.reporting.api.backend.PeppolReportingBackendException;

/**
 * SPI implementation of {@link IPeppolReportingBackendProviderSPI} that creates
 * {@link PeppolReportingBackendSql} instances. Creating a backend runs the Flyway database
 * migration, unless it is disabled in the configuration.
 *
 * @author Philip Helger
 */
@IsSPIImplementation
public class PeppolReportingBackendSqlSPI implements IPeppolReportingBackendProviderSPI
{
  public static final String BACKEND_ID = "sql";

  private static final Logger LOGGER = LoggerFactory.getLogger (PeppolReportingBackendSqlSPI.class);
  private static final EnumSet <EDatabaseSystemType> ALLOWED_DB_TYPES = EnumSet.of (EDatabaseSystemType.MYSQL,
                                                                                    EDatabaseSystemType.POSTGRESQL,
                                                                                    EDatabaseSystemType.SQLSERVER,
                                                                                    EDatabaseSystemType.DB2,
                                                                                    EDatabaseSystemType.ORACLE);

  @UsedViaReflection
  public PeppolReportingBackendSqlSPI ()
  {}

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
    return PeppolReportingBackendSql.DISPLAY_NAME;
  }

  /**
   * Create the data source provider to use.
   *
   * @param aJdbcConfig
   *        The JDBC configuration to use. Never <code>null</code>.
   * @return The data source provider and never <code>null</code>.
   */
  @NonNull
  @OverrideOnDemand
  protected DataSourceProviderFromJdbcConfiguration createReportingDataSourceProvider (@NonNull final IJdbcConfiguration aJdbcConfig)
  {
    return new DataSourceProviderFromJdbcConfiguration (aJdbcConfig);
  }

  @NonNull
  public PeppolReportingBackendSql createBackend (@NonNull final IConfig aConfig) throws PeppolReportingBackendException
  {
    ValueEnforcer.notNull (aConfig, "Config");

    // Init JDBC configuration
    final ReportingJdbcConfiguration aJdbcConfig = new ReportingJdbcConfiguration (aConfig);

    // Resolve database type
    final EDatabaseSystemType eDBType = aJdbcConfig.getJdbcDatabaseSystemType ();
    if (eDBType == null || !ALLOWED_DB_TYPES.contains (eDBType))
      throw new PeppolReportingBackendException ("The database type MUST be provided and MUST be one of " +
                                                 StringImplode.imploder ()
                                                              .source (ALLOWED_DB_TYPES, EDatabaseSystemType::getID)
                                                              .separator (", ")
                                                              .build () +
                                                 " - provided value is '" +
                                                 aJdbcConfig.getJdbcDatabaseType () +
                                                 "'");

    // Build Flyway configuration
    final ReportingFlywayConfigurationBuilder aBuilder = new ReportingFlywayConfigurationBuilder (aConfig, aJdbcConfig);
    final FlywayConfiguration aFlywayConfig = aBuilder.build ();

    // Run Flyway
    if (aFlywayConfig.isFlywayEnabled ())
    {
      try
      {
        ReportingFlywayMigrator.Singleton.INSTANCE.runFlyway (eDBType, aJdbcConfig, aFlywayConfig);
      }
      catch (final RuntimeException ex)
      {
        throw new PeppolReportingBackendException ("Failed to run the Peppol Reporting Flyway migration", ex);
      }
    }
    else
      LOGGER.warn ("Peppol Reporting Flyway Migration is disabled according to the configuration key '" +
                   aBuilder.getConfigKeyEnabled () +
                   "'");

    return new PeppolReportingBackendSql (aJdbcConfig,
                                          createReportingDataSourceProvider (aJdbcConfig),
                                          DBSystemHelper.getTableNamePrefix (eDBType, aJdbcConfig.getJdbcSchema ()));
  }
}
