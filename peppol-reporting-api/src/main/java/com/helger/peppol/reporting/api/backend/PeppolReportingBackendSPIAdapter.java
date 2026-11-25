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

import java.time.LocalDate;
import java.util.function.Consumer;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.GuardedBy;
import com.helger.annotation.concurrent.ThreadSafe;
import com.helger.base.concurrent.SimpleReadWriteLock;
import com.helger.base.state.ESuccess;
import com.helger.config.IConfig;
import com.helger.peppol.reporting.api.PeppolReportingItem;

/**
 * Implementation of the deprecated {@link IPeppolReportingBackendSPI} on top of
 * {@link PeppolReportingBackend#createBackend(IConfig)}. {@link #initBackend(IConfig)} selects and
 * creates the backend, {@link #shutdownBackend()} closes it.
 *
 * @author Philip Helger
 * @since 5.0.0
 * @deprecated Only exists for the deprecated {@link PeppolReportingBackend#getBackendService()}
 */
@Deprecated (forRemoval = true, since = "5.0.0")
@SuppressWarnings ("removal")
@ThreadSafe
final class PeppolReportingBackendSPIAdapter implements IPeppolReportingBackendSPI
{
  private static final Logger LOGGER = LoggerFactory.getLogger (PeppolReportingBackendSPIAdapter.class);

  private final SimpleReadWriteLock m_aRWLock = new SimpleReadWriteLock ();
  @GuardedBy ("m_aRWLock")
  private IPeppolReportingBackend m_aBackend;

  @Nullable
  private IPeppolReportingBackend _getBackendOrNull ()
  {
    return m_aRWLock.readLockedGet ( () -> m_aBackend);
  }

  @NonNull
  private IPeppolReportingBackend _getBackend ()
  {
    final IPeppolReportingBackend ret = _getBackendOrNull ();
    if (ret == null)
      throw new IllegalStateException ("The Peppol Reporting backend is not initialized");
    return ret;
  }

  @NonNull
  private IPeppolReportingItemReader _getReader () throws PeppolReportingBackendException
  {
    final IPeppolReportingBackend aBackend = _getBackend ();
    if (aBackend instanceof final IPeppolReportingItemReader aReader)
      return aReader;
    throw new PeppolReportingBackendException ("The Peppol Reporting " +
                                               aBackend.getDisplayName () +
                                               " backend does not support reading reporting items");
  }

  @NonNull
  @Nonempty
  public String getDisplayName ()
  {
    final IPeppolReportingBackend aBackend = _getBackendOrNull ();
    return aBackend != null ? aBackend.getDisplayName () : "uninitialized";
  }

  @NonNull
  public ESuccess initBackend (@NonNull final IConfig aConfig)
  {
    final IPeppolReportingBackend aNewBackend;
    try
    {
      aNewBackend = PeppolReportingBackend.createBackend (aConfig);
    }
    catch (final PeppolReportingBackendException ex)
    {
      LOGGER.error ("Failed to initialize the Peppol Reporting backend", ex);
      return ESuccess.FAILURE;
    }

    final IPeppolReportingBackend aOldBackend = m_aRWLock.writeLockedGet ( () -> {
      final IPeppolReportingBackend ret = m_aBackend;
      m_aBackend = aNewBackend;
      return ret;
    });
    if (aOldBackend != null)
    {
      LOGGER.warn ("The Peppol Reporting backend was already initialized - replacing it");
      aOldBackend.close ();
    }
    return ESuccess.SUCCESS;
  }

  public boolean isInitialized ()
  {
    return _getBackendOrNull () != null;
  }

  public void shutdownBackend ()
  {
    final IPeppolReportingBackend aOldBackend = m_aRWLock.writeLockedGet ( () -> {
      final IPeppolReportingBackend ret = m_aBackend;
      m_aBackend = null;
      return ret;
    });
    if (aOldBackend != null)
      aOldBackend.close ();
  }

  public void storeReportingItem (@NonNull final PeppolReportingItem aReportingItem) throws PeppolReportingBackendException
  {
    _getBackend ().storeReportingItem (aReportingItem);
  }

  @NonNull
  public Iterable <PeppolReportingItem> iterateReportingItems (@NonNull final LocalDate aStartDateIncl,
                                                               @NonNull final LocalDate aEndDateIncl) throws PeppolReportingBackendException
  {
    return _getReader ().iterateReportingItems (aStartDateIncl, aEndDateIncl);
  }

  @Override
  public void forEachReportingItem (@NonNull final LocalDate aStartDateIncl,
                                    @NonNull final LocalDate aEndDateIncl,
                                    @NonNull final Consumer <? super PeppolReportingItem> aConsumer) throws PeppolReportingBackendException
  {
    _getReader ().forEachReportingItem (aStartDateIncl, aEndDateIncl, aConsumer);
  }
}
