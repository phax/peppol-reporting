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

import java.time.Duration;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.db.api.config.IJdbcConfiguration;
import com.helger.db.jdbc.IHasDataSource;
import com.helger.db.jdbc.executor.DBExecutor;

/**
 * The Reporting specific DB Executor
 *
 * @author Philip Helger
 */
public final class ReportingDBExecutor extends DBExecutor
{
  private static final Logger LOGGER = LoggerFactory.getLogger (ReportingDBExecutor.class);

  public ReportingDBExecutor (@NonNull final IHasDataSource aDataSourceProvider,
                              @NonNull final IJdbcConfiguration aJdbcConfig)
  {
    super (aDataSourceProvider);

    // This is ONLY for debugging
    setDebugConnections (aJdbcConfig.isJdbcDebugConnections ());
    setDebugTransactions (aJdbcConfig.isJdbcDebugTransactions ());
    setDebugSQLStatements (aJdbcConfig.isJdbcDebugSQL ());

    if (aJdbcConfig.isJdbcExecutionTimeWarningEnabled ())
    {
      final Duration aWarnDuration = aJdbcConfig.getJdbcExecutionTimeWarning ();
      if (aWarnDuration.compareTo (Duration.ZERO) > 0)
        setExecutionWarnDuration (aWarnDuration);
      else
        if (LOGGER.isDebugEnabled ())
          LOGGER.debug ("Ignoring JDBC Execution Time Warning Milliseconds because it is invalid.");
    }
    else
    {
      // Zero means none
      setExecutionWarnDuration (Duration.ZERO);
    }
  }
}
