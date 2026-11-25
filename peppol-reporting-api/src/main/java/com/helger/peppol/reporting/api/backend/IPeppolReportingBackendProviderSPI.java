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
import com.helger.annotation.style.IsSPIInterface;
import com.helger.base.name.IHasDisplayName;
import com.helger.config.IConfig;

/**
 * The SPI interface to provide a Peppol Reporting backend. Implementations are found via the
 * {@link java.util.ServiceLoader} mechanism and must be stateless - all state belongs to the
 * {@link IPeppolReportingBackend} created by {@link #createBackend(IConfig)}. Use
 * {@link PeppolReportingBackend#createBackend(IConfig)} to select a provider based on the
 * configuration and to create the backend.
 *
 * @author Philip Helger
 * @since 5.0.0
 */
@IsSPIInterface
public interface IPeppolReportingBackendProviderSPI extends IHasDisplayName
{
  /**
   * @return The unique ID of the backend, used to select it via the configuration property
   *         {@link PeppolReportingBackend#CONFIG_PEPPOL_REPORTING_BACKEND}. It is compared
   *         case-insensitively. Neither <code>null</code> nor empty.
   */
  @NonNull
  @Nonempty
  String getBackendID ();

  /**
   * @return The display name of the backend. That is mainly for logging purposes. Neither
   *         <code>null</code> nor empty.
   */
  @NonNull
  @Nonempty
  String getDisplayName ();

  /**
   * Create a new, ready to use backend, based on the provided configuration. The caller is
   * responsible for closing the returned backend.
   *
   * @param aConfig
   *        The configuration to use. May not be <code>null</code>.
   * @return The created backend and never <code>null</code>.
   * @throws PeppolReportingBackendException
   *         If the configuration is invalid or the backend could not be created
   */
  @NonNull
  IPeppolReportingBackend createBackend (@NonNull IConfig aConfig) throws PeppolReportingBackendException;
}
