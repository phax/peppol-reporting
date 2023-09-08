/*
 * Copyright (C) 2022-2023 Philip Helger
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
package com.helper.peppol.reporting.api.backend;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.functional.IThrowingConsumer;
import com.helger.commons.lang.ServiceLoaderHelper;
import com.helger.commons.state.ESuccess;
import com.helger.config.IConfig;

/**
 * This is the entry class for the reporting backend. It uses the SPI mechanism
 * to load an instance of {@link IPeppolReportingBackendSPI} which is then
 * accessible via {@link #getBackendService()}.
 *
 * @author Philip Helger
 */
@Immutable
public class PeppolReportingBackend
{
  private static final Logger LOGGER = LoggerFactory.getLogger (PeppolReportingBackend.class);

  private static final IPeppolReportingBackendSPI BACKEND_SERVICE = _loadBackendService ();

  private PeppolReportingBackend ()
  {}

  @Nonnull
  private static IPeppolReportingBackendSPI _loadBackendService ()
  {
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Loading IPeppolReportingBackendSPI implementations");

    final ICommonsList <IPeppolReportingBackendSPI> aBackends = ServiceLoaderHelper.getAllSPIImplementations (IPeppolReportingBackendSPI.class);
    final int nBackends = aBackends.size ();
    if (nBackends != 1)
    {
      throw new IllegalStateException ("Failed to find exactly one backend SPI implementations, but " +
                                       nBackends +
                                       " instances");
    }

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Found exactly one IPeppolReportingBackendSPI implementation");
    return aBackends.getFirst ();
  }

  @Nonnull
  public static IPeppolReportingBackendSPI getBackendService ()
  {
    return BACKEND_SERVICE;
  }

  /**
   * This is a helper method that ensures that all activities with an
   * {@link IPeppolReportingBackendSPI} are wrapped in the proper init and
   * shutdown method calls.
   *
   * @param aConfig
   *        The configuration required to start the backend. May not be
   *        <code>null</code>.
   * @param aBackendConsumer
   *        The consumer for the backend. May not be <code>null</code>.
   * @return {@link ESuccess} if it was successful or not.
   * @throws PeppolReportingBackendException
   *         if the backend consumer throws an exception
   */
  @Nonnull
  public static ESuccess withBackendDo (@Nonnull final IConfig aConfig,
                                        @Nonnull final IThrowingConsumer <? super IPeppolReportingBackendSPI, PeppolReportingBackendException> aBackendConsumer) throws PeppolReportingBackendException
  {
    ValueEnforcer.notNull (aConfig, "Config");
    ValueEnforcer.notNull (aBackendConsumer, "BackendConsumer");

    final IPeppolReportingBackendSPI aBackend = getBackendService ();
    if (aBackend.initBackend (aConfig).isFailure ())
      return ESuccess.FAILURE;

    try
    {
      // Do something with the backend
      aBackendConsumer.accept (aBackend);
    }
    finally
    {
      aBackend.shutdownBackend ();
    }
    return ESuccess.SUCCESS;
  }
}
