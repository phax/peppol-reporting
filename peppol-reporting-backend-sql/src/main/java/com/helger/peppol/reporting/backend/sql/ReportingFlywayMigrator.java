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

import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.migration.JavaMigration;
import org.jspecify.annotations.NonNull;

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.db.api.EDatabaseSystemType;
import com.helger.db.flyway.FlywayConfiguration;
import com.helger.db.flyway.FlywayMigrationRunner;

/**
 * This class has the sole purpose of encapsulating the org.flywaydb classes, so that it's usage can
 * be turned off (for whatever reason).
 *
 * @author Philip Helger
 */
final class ReportingFlywayMigrator
{
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

  void runFlyway (@NonNull final EDatabaseSystemType eDBType,
                  @NonNull final ReportingJdbcConfiguration aJdbcConfig,
                  @NonNull final FlywayConfiguration aFlywayConfig)
  {
    ValueEnforcer.notNull (eDBType, "DBType");
    ValueEnforcer.notNull (aJdbcConfig, "JdbcConfig");
    ValueEnforcer.notNull (aFlywayConfig, "FlywayConfig");

    FlywayMigrationRunner.runFlyway (aJdbcConfig,
                                     aFlywayConfig,
                                     "db/reporting-" + eDBType.getID (),
                                     (JavaMigration []) null,
                                     (Callback []) null);
  }
}
