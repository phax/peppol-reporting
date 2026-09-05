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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.LocalDate;
import java.util.Map;

import org.junit.Test;

import com.helger.config.Config;
import com.helger.config.source.appl.ConfigurationSourceFunction;
import com.helger.peppol.reporting.api.backend.PeppolReportingBackendException;

/**
 * SQL query failure tests that do not require a running database.
 *
 * @author Philip Helger
 */
public final class PeppolReportingBackendSqlTest
{
  @Test
  public void testFailedQueryDoesNotReturnAnEmptyReport () throws Exception
  {
    // An unavailable driver fails deterministically before any network access.
    final Map <String, String> aSettings = Map.of ("peppol.reporting.jdbc.database-type",
                                                   "postgresql",
                                                   "peppol.reporting.jdbc.driver",
                                                   "unavailable.jdbc.Driver",
                                                   "peppol.reporting.jdbc.url",
                                                   "jdbc:unavailable:reporting",
                                                   "peppol.reporting.flyway.enabled",
                                                   "false");
    final PeppolReportingBackendSqlSPI aBackend = new PeppolReportingBackendSqlSPI ();
    try
    {
      assertTrue (aBackend.initBackend (new Config (new ConfigurationSourceFunction (aSettings::get))).isSuccess ());
      final LocalDate aDate = LocalDate.of (2026, 1, 1);
      try
      {
        aBackend.iterateReportingItems (aDate, aDate);
        fail ("A failed SQL query must not be reported as an empty result");
      }
      catch (final PeppolReportingBackendException ex)
      {
        // Expected: callers must not create a report from missing query results.
      }
    }
    finally
    {
      aBackend.shutdownBackend ();
    }
  }
}
