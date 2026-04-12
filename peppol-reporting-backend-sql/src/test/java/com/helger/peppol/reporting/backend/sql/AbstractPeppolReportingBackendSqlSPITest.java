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

import org.jspecify.annotations.NonNull;

import com.helger.config.Config;
import com.helger.config.IConfig;
import com.helger.config.source.resource.properties.ConfigurationSourceProperties;
import com.helger.io.resource.ClassPathResource;
import com.helger.peppol.reporting.api.backend.IPeppolReportingBackendSPI;
import com.helger.peppol.reporting.testbackend.AbstractPeppolReportingBackendSPITest;

/**
 * Base class for the SQL backend's SPI contract tests. Concrete subclasses pick the database
 * flavour by supplying the matching <code>application-*.properties</code> file. The whole suite is
 * skipped (via {@link org.junit.Assume}) when the underlying database server is not reachable.
 *
 * @author Philip Helger
 */
abstract class AbstractPeppolReportingBackendSqlSPITest extends AbstractPeppolReportingBackendSPITest
{
  @NonNull
  protected abstract String getConfigFileName ();

  @Override
  @NonNull
  protected final IPeppolReportingBackendSPI createBackend ()
  {
    return new PeppolReportingBackendSqlSPI ();
  }

  @Override
  @NonNull
  protected final IConfig getConfig ()
  {
    return new Config (new ConfigurationSourceProperties (new ClassPathResource (getConfigFileName (),
                                                                                 AbstractPeppolReportingBackendSqlSPITest.class.getClassLoader ())));
  }
}
