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
package com.helger.peppol.reporting.backend.inmemory;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.annotation.style.IsSPIImplementation;
import com.helger.config.IConfig;
import com.helger.peppol.reporting.api.backend.IPeppolReportingBackendProviderSPI;

/**
 * SPI implementation of {@link IPeppolReportingBackendProviderSPI} that creates
 * {@link PeppolReportingBackendInMemory} instances. Intended for testing only.
 *
 * @author Philip Helger
 */
@IsSPIImplementation
public class PeppolReportingBackendInMemorySPI implements IPeppolReportingBackendProviderSPI
{
  public static final String BACKEND_ID = "inmemory";

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
    return PeppolReportingBackendInMemory.DISPLAY_NAME;
  }

  @NonNull
  public PeppolReportingBackendInMemory createBackend (@NonNull final IConfig aConfig)
  {
    return new PeppolReportingBackendInMemory ();
  }
}
