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

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.callback.BaseCallback;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.flywaydb.core.api.resolver.ResolvedMigration;
import org.flywaydb.core.internal.info.MigrationInfoImpl;
import org.flywaydb.core.internal.jdbc.DriverDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.string.StringHelper;
import com.helger.config.IConfig;
import com.helger.db.api.flyway.FlywayConfiguration;

/**
 * This class has the sole purpose of encapsulating the org.flywaydb classes, so that it's usage can
 * be turned off (for whatever reason).
 *
 * @author Philip Helger
 */
final class ReportingFlywayMigrator
{
  private static final Logger LOGGER = LoggerFactory.getLogger (ReportingFlywayMigrator.Singleton.class);

  // Indirection level to not load org.flyway classes by default
  @Immutable
  public static final class Singleton
  {
    static final ReportingFlywayMigrator INSTANCE = new ReportingFlywayMigrator ();

    private Singleton ()
    {}
  }

  private ReportingFlywayMigrator ()
  {}

  void runFlyway (@Nonnull final EReportingDatabaseType eDBType, @Nonnull final IConfig aConfig)
  {
    ValueEnforcer.notNull (eDBType, "DBType");

    LOGGER.info ("Starting to run Flyway for DB type " + eDBType);

    final Callback aCallbackLogging = new BaseCallback ()
    {
      public void handle (@Nonnull final Event aEvent, @Nullable final Context aContext)
      {
        LOGGER.info ("Flyway: Event " + aEvent.getId ());
        if (aEvent == Event.AFTER_EACH_MIGRATE && aContext != null)
        {
          final MigrationInfo aMI = aContext.getMigrationInfo ();
          if (aMI instanceof MigrationInfoImpl)
          {
            final ResolvedMigration aRM = ((MigrationInfoImpl) aMI).getResolvedMigration ();
            if (aRM != null)
              LOGGER.info ("  Performed migration: " + aRM);
          }
        }
      }
    };
    final Callback aCallbackAudit = new BaseCallback ()
    {
      public void handle (@Nonnull final Event aEvent, @Nullable final Context aContext)
      {
        if (aEvent == Event.AFTER_EACH_MIGRATE && aContext != null)
        {
          final MigrationInfo aMI = aContext.getMigrationInfo ();
          if (aMI instanceof MigrationInfoImpl)
          {
            final ResolvedMigration aRM = ((MigrationInfoImpl) aMI).getResolvedMigration ();
            if (aRM != null && aRM.getVersion ().isAtLeast ("7"))
              LOGGER.info ("  SQL Migration success: " +
                           aRM.getVersion ().toString () +
                           " / " +
                           aRM.getDescription () +
                           " / " +
                           aRM.getScript () +
                           " / " +
                           aRM.getType ().name () +
                           " / " +
                           aRM.getPhysicalLocation ());
          }
        }
      }
    };

    final FlywayConfiguration aFlywayConfig = new ReportingFlywayConfigurationBuilder (aConfig).build ();

    // The JDBC driver is the same as for main connection
    final FluentConfiguration aFlywayFluentConfig = Flyway.configure ()
                                                          .dataSource (new DriverDataSource (ReportingFlywayMigrator.class.getClassLoader (),
                                                                                             ReportingJdbcConfiguration.getJdbcDriver (aConfig),
                                                                                             aFlywayConfig.getFlywayJdbcUrl (),
                                                                                             aFlywayConfig.getFlywayJdbcUser (),
                                                                                             aFlywayConfig.getFlywayJdbcPassword ()));

    // Required for creating DB tables
    aFlywayFluentConfig.baselineOnMigrate (true);

    // Disable validation, because DDL comments are also taken into
    // consideration
    aFlywayFluentConfig.validateOnMigrate (false);

    // Version 1 is the baseline
    aFlywayFluentConfig.baselineVersion (Integer.toString (aFlywayConfig.getFlywayBaselineVersion ()))
                       .baselineDescription ("Peppol Reporting Baseline");

    // Separate directory per DB type
    aFlywayFluentConfig.locations ("db/reporting-" + eDBType.getID ());

    // Avoid scanning the ClassPath by enumerating them explicitly
    if (false)
      aFlywayFluentConfig.javaMigrations ();

    // Callbacks
    aFlywayFluentConfig.callbacks (aCallbackLogging, aCallbackAudit);

    // Flyway to handle the DB schema?
    final String sSchema = ReportingJdbcConfiguration.getJdbcSchema (aConfig);
    if (StringHelper.hasText (sSchema))
    {
      // Use the schema only, if it is explicitly configured
      // The default schema name is ["$user", public] and as such unusable
      aFlywayFluentConfig.schemas (sSchema);
    }
    // If no schema is specified, schema create should also be disabled
    aFlywayFluentConfig.createSchemas (aFlywayConfig.isFlywaySchemaCreate ());

    final Flyway aFlyway = aFlywayFluentConfig.load ();
    if (false)
      aFlyway.validate ();
    aFlyway.migrate ();

    LOGGER.info ("Finished running Flyway");
  }
}
