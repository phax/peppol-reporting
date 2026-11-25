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
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.concurrent.GuardedBy;
import com.helger.annotation.concurrent.ThreadSafe;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.concurrent.SimpleReadWriteLock;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.functional.IThrowingConsumer;
import com.helger.base.spi.ServiceLoaderHelper;
import com.helger.base.state.ESuccess;
import com.helger.base.string.StringHelper;
import com.helger.base.string.StringImplode;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.config.IConfig;
import com.helger.peppol.reporting.api.PeppolReportingAPIVersion;

/**
 * This is the entry class for the reporting backend. It finds all
 * {@link IPeppolReportingBackendProviderSPI} implementations via the SPI mechanism, selects one
 * based on the configuration and creates the backend:
 *
 * <pre>
 * try (final IPeppolReportingBackend aBackend = PeppolReportingBackend.createBackend (aConfig))
 * {
 *   aBackend.storeReportingItem (sItemID, aReportingItem);
 * }
 * </pre>
 *
 * @author Philip Helger
 */
@ThreadSafe
public final class PeppolReportingBackend
{
  /**
   * The configuration property containing the ID of the backend to use. Only required if more than
   * one backend is on the classpath.
   *
   * @since 5.0.0
   */
  public static final String CONFIG_PEPPOL_REPORTING_BACKEND = "peppol.reporting.backend";

  private static final Logger LOGGER = LoggerFactory.getLogger (PeppolReportingBackend.class);

  /**
   * Lazily loads all providers exactly once.
   */
  private static final class ProviderHolder
  {
    private static final ICommonsList <IPeppolReportingBackendProviderSPI> PROVIDERS = new CommonsArrayList <> (ServiceLoaderHelper.getAllSPIImplementations (IPeppolReportingBackendProviderSPI.class));
  }

  private static final SimpleReadWriteLock RW_LOCK = new SimpleReadWriteLock ();
  @Deprecated (forRemoval = true, since = "5.0.0")
  @SuppressWarnings ("removal")
  @GuardedBy ("RW_LOCK")
  private static IPeppolReportingBackendSPI s_aBackendService;

  private PeppolReportingBackend ()
  {}

  /**
   * @return All backend providers found via the SPI mechanism. Never <code>null</code> but maybe
   *         empty.
   * @since 5.0.0
   */
  @NonNull
  @ReturnsMutableCopy
  public static ICommonsList <IPeppolReportingBackendProviderSPI> getAllBackendProviders ()
  {
    return ProviderHolder.PROVIDERS.getClone ();
  }

  @NonNull
  private static String _getAllBackendIDs (@NonNull final ICommonsList <IPeppolReportingBackendProviderSPI> aProviders)
  {
    return StringImplode.imploder ()
                        .source (aProviders, IPeppolReportingBackendProviderSPI::getBackendID)
                        .separator (", ")
                        .build ();
  }

  /**
   * Find the backend provider to use. If the configuration property
   * {@link #CONFIG_PEPPOL_REPORTING_BACKEND} is set, the provider with that backend ID (compared
   * case-insensitively) is used. If it is not set, the only available provider is used.
   *
   * @param aConfig
   *        The configuration to use. May not be <code>null</code>.
   * @return The selected provider and never <code>null</code>.
   * @throws PeppolReportingBackendException
   *         If no provider matches, or if the selection is ambiguous
   * @since 5.0.0
   */
  @NonNull
  public static IPeppolReportingBackendProviderSPI getBackendProvider (@NonNull final IConfig aConfig) throws PeppolReportingBackendException
  {
    ValueEnforcer.notNull (aConfig, "Config");

    final ICommonsList <IPeppolReportingBackendProviderSPI> aProviders = ProviderHolder.PROVIDERS;
    if (aProviders.isEmpty ())
      throw new PeppolReportingBackendException ("No Peppol Reporting backend provider was found on the classpath");

    final String sBackendID = aConfig.getAsString (CONFIG_PEPPOL_REPORTING_BACKEND);
    if (StringHelper.isEmpty (sBackendID))
    {
      if (aProviders.size () > 1)
        throw new PeppolReportingBackendException ("Found " +
                                                   aProviders.size () +
                                                   " Peppol Reporting backend providers (" +
                                                   _getAllBackendIDs (aProviders) +
                                                   ") - use the configuration property '" +
                                                   CONFIG_PEPPOL_REPORTING_BACKEND +
                                                   "' to select one");
      return aProviders.getFirstOrNull ();
    }

    final ICommonsList <IPeppolReportingBackendProviderSPI> aMatches = aProviders.getAll (x -> x.getBackendID ()
                                                                                                .equalsIgnoreCase (sBackendID));
    if (aMatches.isEmpty ())
      throw new PeppolReportingBackendException ("The Peppol Reporting backend '" +
                                                 sBackendID +
                                                 "' configured in '" +
                                                 CONFIG_PEPPOL_REPORTING_BACKEND +
                                                 "' was not found. Available backends: " +
                                                 _getAllBackendIDs (aProviders));
    if (aMatches.size () > 1)
      throw new PeppolReportingBackendException ("Found " +
                                                 aMatches.size () +
                                                 " Peppol Reporting backend providers with the ID '" +
                                                 sBackendID +
                                                 "'");
    return aMatches.getFirstOrNull ();
  }

  /**
   * Select the backend provider via {@link #getBackendProvider(IConfig)} and create the backend.
   * The caller is responsible for closing the returned backend.
   *
   * @param aConfig
   *        The configuration to use. May not be <code>null</code>.
   * @return The created backend and never <code>null</code>.
   * @throws PeppolReportingBackendException
   *         If no provider could be selected or if the backend could not be created
   * @since 5.0.0
   */
  @NonNull
  public static IPeppolReportingBackend createBackend (@NonNull final IConfig aConfig) throws PeppolReportingBackendException
  {
    final IPeppolReportingBackendProviderSPI aProvider = getBackendProvider (aConfig);
    LOGGER.info ("Creating Peppol Reporting backend '" +
                 aProvider.getDisplayName () +
                 "/" +
                 PeppolReportingAPIVersion.BUILD_VERSION +
                 "'");
    return aProvider.createBackend (aConfig);
  }

  /**
   * @return The backend service. Unless one was set explicitly via
   *         {@link #setBackendService(IPeppolReportingBackendSPI)}, this is an adapter that selects
   *         and creates the backend in {@link IPeppolReportingBackendSPI#initBackend(IConfig)} via
   *         {@link #createBackend(IConfig)}. May be <code>null</code> if no backend provider was
   *         found.
   * @deprecated Use {@link #createBackend(IConfig)} instead
   */
  @Deprecated (forRemoval = true, since = "5.0.0")
  @SuppressWarnings ("removal")
  @Nullable
  public static IPeppolReportingBackendSPI getBackendService ()
  {
    final IPeppolReportingBackendSPI ret = RW_LOCK.readLockedGet (() -> s_aBackendService);
    if (ret != null)
      return ret;

    if (ProviderHolder.PROVIDERS.isEmpty ())
    {
      LOGGER.error ("Failed to find any Peppol Reporting backend provider");
      return null;
    }

    return RW_LOCK.writeLockedGet (() -> {
      if (s_aBackendService == null)
        s_aBackendService = new PeppolReportingBackendSPIAdapter ();
      return s_aBackendService;
    });
  }

  /**
   * Explicitly set a backend service.
   *
   * @param aBackendService
   *        The backend service to use. May not be <code>null</code>.
   * @since 3.0.2
   * @deprecated Use {@link IPeppolReportingBackendProviderSPI#createBackend(IConfig)} of the
   *             desired provider instead
   */
  @Deprecated (forRemoval = true, since = "5.0.0")
  @SuppressWarnings ("removal")
  public static void setBackendService (@NonNull final IPeppolReportingBackendSPI aBackendService)
  {
    ValueEnforcer.notNull (aBackendService, "BackendService");

    RW_LOCK.writeLocked (() -> s_aBackendService = aBackendService);
  }

  /**
   * @return <code>true</code> if a backend service is configured, <code>false</code> if not.
   * @since 2.1.1
   * @deprecated Use {@link #getAllBackendProviders()} instead
   */
  @Deprecated (forRemoval = true, since = "5.0.0")
  public static boolean isBackendServiceConfigured ()
  {
    return getBackendService () != null;
  }

  /**
   * This is a helper method that ensures that all activities with an
   * {@link IPeppolReportingBackendSPI} are wrapped in the proper init and shutdown method calls.
   * Make sure to not call this method in a nested fashion or in multiple threads.
   *
   * @param aConfig
   *        The configuration required to start the backend. May not be <code>null</code>.
   * @param aBackendConsumer
   *        The consumer for the backend. May not be <code>null</code>.
   * @return {@link ESuccess} if it was successful or not.
   * @throws PeppolReportingBackendException
   *         if the backend consumer throws an exception
   * @deprecated Use {@link #createBackend(IConfig)} in a try-with-resources block instead
   */
  @Deprecated (forRemoval = true, since = "5.0.0")
  @SuppressWarnings ("removal")
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
