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
package com.helger.peppol.reporting.backend.redis;

import org.jspecify.annotations.NonNull;

import com.helger.peppol.reporting.api.backend.IPeppolReportingBackendSPI;
import com.helger.peppol.reporting.testbackend.AbstractPeppolReportingBackendSPITest;

/**
 * SPI contract test for {@link PeppolReportingBackendRedisSPI}. The whole
 * suite is skipped (via {@link org.junit.Assume}) when no Redis instance is
 * reachable on the configured host.
 *
 * @author Philip Helger
 */
public final class PeppolReportingBackendRedisSPITest extends AbstractPeppolReportingBackendSPITest
{
  @Override
  @NonNull
  protected IPeppolReportingBackendSPI createBackend ()
  {
    return new PeppolReportingBackendRedisSPI ();
  }
}
