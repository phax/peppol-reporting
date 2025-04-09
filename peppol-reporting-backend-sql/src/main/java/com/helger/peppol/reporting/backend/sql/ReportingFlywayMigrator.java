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
import com.helger.db.api.EDatabaseSystemType;
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

  void runFlyway (@Nonnull final EDatabaseSystemType eDBType,
                  @Nonnull final ReportingJdbcConfiguration aJdbcConfig,
                  final FlywayConfiguration aFlywayConfig)
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

    // The JDBC driver is the same as for main connection
    final FluentConfiguration aActualFlywayConfig = Flyway.configure ()
                                                          .dataSource (new DriverDataSource (ReportingFlywayMigrator.class.getClassLoader (),
                                                                                             aJdbcConfig.getJdbcDriver (),
                                                                                             aFlywayConfig.getFlywayJdbcUrl (),
                                                                                             aFlywayConfig.getFlywayJdbcUser (),
                                                                                             aFlywayConfig.getFlywayJdbcPassword ()));

    // Required for creating DB tables
    aActualFlywayConfig.baselineOnMigrate (true);

    // Disable validation, because DDL comments are also taken into
    // consideration
    aActualFlywayConfig.validateOnMigrate (false);

    // Version 1 is the baseline
    aActualFlywayConfig.baselineVersion (Integer.toString (aFlywayConfig.getFlywayBaselineVersion ()))
                       .baselineDescription ("Peppol Reporting Baseline");

    // Separate directory per DB type
    aActualFlywayConfig.locations ("db/reporting-" + eDBType.getID ());

    // Avoid scanning the ClassPath by enumerating them explicitly
    if (false)
      aActualFlywayConfig.javaMigrations ();

    // Callbacks
    aActualFlywayConfig.callbacks (aCallbackLogging, aCallbackAudit);

    // Flyway to handle the DB schema?
    final String sSchema = aJdbcConfig.getJdbcSchema ();
    if (StringHelper.hasText (sSchema))
    {
      // Use the schema only, if it is explicitly configured
      // The default schema name is ["$user", public] and as such unusable
      aActualFlywayConfig.schemas (sSchema);
    }
    // If no schema is specified, schema create should also be disabled
    aActualFlywayConfig.createSchemas (aFlywayConfig.isFlywaySchemaCreate ());

    final Flyway aFlyway = aActualFlywayConfig.load ();
    if (false)
      aFlyway.validate ();
    aFlyway.migrate ();

    LOGGER.info ("Finished running Flyway");
  }
}
