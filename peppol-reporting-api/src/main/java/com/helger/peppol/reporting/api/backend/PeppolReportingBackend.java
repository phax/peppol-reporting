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
package com.helger.peppol.reporting.api.backend;

import java.util.List;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.concurrent.GuardedBy;
import com.helger.annotation.concurrent.ThreadSafe;
import com.helger.base.concurrent.SimpleReadWriteLock;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.functional.IThrowingConsumer;
import com.helger.base.spi.ServiceLoaderHelper;
import com.helger.base.state.ESuccess;
import com.helger.config.IConfig;
import com.helger.peppol.reporting.api.PeppolReportingAPIVersion;

/**
 * This is the entry class for the reporting backend. It uses the SPI mechanism to load an instance
 * of {@link IPeppolReportingBackendSPI} which is then accessible via {@link #getBackendService()}.
 *
 * @author Philip Helger
 */
@ThreadSafe
public class PeppolReportingBackend
{
  private static final Logger LOGGER = LoggerFactory.getLogger (PeppolReportingBackend.class);

  private static final SimpleReadWriteLock RW_LOCK = new SimpleReadWriteLock ();
  @GuardedBy ("RW_LOCK")
  private static IPeppolReportingBackendSPI s_aBackendService = _loadBackendService ();

  @Nullable
  private static IPeppolReportingBackendSPI _loadBackendService ()
  {
    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Loading IPeppolReportingBackendSPI implementation");

    final List <IPeppolReportingBackendSPI> aBackends = ServiceLoaderHelper.getAllSPIImplementations (IPeppolReportingBackendSPI.class);
    final int nBackends = aBackends.size ();
    if (nBackends != 1)
    {
      if (nBackends == 0)
        LOGGER.error ("Failed to find any Peppol Reporting backend SPI implementation");
      else
        LOGGER.error ("Failed to find exactly one Peppol Reporting backend SPI implementations, but " +
                      nBackends +
                      " instances");
      return null;
    }

    final IPeppolReportingBackendSPI ret = aBackends.get (0);
    LOGGER.info ("Using IPeppolReportingBackendSPI implementation '" +
                 ret.getDisplayName () +
                 "/" +
                 PeppolReportingAPIVersion.BUILD_VERSION +
                 "'");
    return ret;
  }

  private PeppolReportingBackend ()
  {}

  /**
   * @return The loaded reporting backend implementation. May be <code>null</code> if no SPI
   *         implementation is registered.
   */
  @Nullable
  public static IPeppolReportingBackendSPI getBackendService ()
  {
    return RW_LOCK.readLockedGet ( () -> s_aBackendService);
  }

  /**
   * Explicitly set a backend service, in case loading via the SPI failed.
   *
   * @param aBackendService
   *        The backend service to use. May not be <code>null</code>.
   * @since 3.0.2
   */
  public static void setBackendService (@NonNull final IPeppolReportingBackendSPI aBackendService)
  {
    ValueEnforcer.notNull (aBackendService, "BackendService");

    RW_LOCK.writeLocked ( () -> s_aBackendService = aBackendService);
  }

  /**
   * @return <code>true</code> if a backend service is configured, <code>false</code> if not.
   * @since 2.1.1
   */
  public static boolean isBackendServiceConfigured ()
  {
    return getBackendService () != null;
  }

  /**
   * This is a helper method that ensures that all activities with an
   * {@link IPeppolReportingBackendSPI} are wrapped in the proper init and shutdown method calls.
   * Make sure to not call this method in a nested fashion or in multiple threads. This method is
   * primarily helpful for tests and single-threaded applications. To use the reporting backend in a
   * multi threaded environment, it is highly recommended that you extract the init and shutdown
   * calls into your application framework.
   *
   * @param aConfig
   *        The configuration required to start the backend. May not be <code>null</code>.
   * @param aBackendConsumer
   *        The consumer for the backend. May not be <code>null</code>.
   * @return {@link ESuccess} if it was successful or not.
   * @throws PeppolReportingBackendException
   *         if the backend consumer throws an exception
   */
  @NonNull
  public static ESuccess withBackendDo (@NonNull final IConfig aConfig,
                                        @NonNull final IThrowingConsumer <? super IPeppolReportingBackendSPI, PeppolReportingBackendException> aBackendConsumer) throws PeppolReportingBackendException
  {
    ValueEnforcer.notNull (aConfig, "Config");
    ValueEnforcer.notNull (aBackendConsumer, "BackendConsumer");

    final IPeppolReportingBackendSPI aBackend = getBackendService ();
    if (aBackend == null)
      return ESuccess.FAILURE;

    if (aBackend.isInitialized ())
    {
      // Immediately do something with the backend
      aBackendConsumer.accept (aBackend);
    }
    else
    {
      // Init, run, shutdown
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
    }
    return ESuccess.SUCCESS;
  }
}
