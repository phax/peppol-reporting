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
package com.helger.peppol.reporting.api.backend;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.annotation.style.IsSPIImplementation;
import com.helger.config.IConfig;

/**
 * Test provider with the backend ID "mock-b".
 *
 * @author Philip Helger
 */
@IsSPIImplementation
public final class MockBackendProviderBSPI implements IPeppolReportingBackendProviderSPI
{
  public static final String BACKEND_ID = "mock-b";

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
    return "MockB";
  }

  @NonNull
  public IPeppolReportingBackend createBackend (@NonNull final IConfig aConfig)
  {
    return new MockPeppolReportingBackend (getDisplayName ());
  }
}
