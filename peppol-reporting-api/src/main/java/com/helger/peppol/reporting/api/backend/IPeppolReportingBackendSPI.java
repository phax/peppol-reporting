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

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.function.Consumer;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.annotation.style.IsSPIInterface;
import com.helger.base.name.IHasDisplayName;
import com.helger.base.state.ESuccess;
import com.helger.config.IConfig;
import com.helger.peppol.reporting.api.PeppolReportingItem;

/**
 * A generic interface for writing and reading reporting items.
 *
 * @author Philip Helger
 * @since 2.1.0
 */
@IsSPIInterface
public interface IPeppolReportingBackendSPI extends IHasDisplayName
{
  /**
   * @return The display name name of the backend that is used. That is mainly
   *         for logging purposes.
   */
  @NonNull
  @Nonempty
  String getDisplayName ();

  /**
   * Initialize the backend using the provided configuration data.
   *
   * @param aConfig
   *        The configuration value provider to be used. Never
   *        <code>null</code>.
   * @return {@link ESuccess}
   */
  @NonNull
  ESuccess initBackend (@NonNull IConfig aConfig);

  /**
   * This method indicates if {@link #initBackend(IConfig)} was called and
   * delivered success and {@link #shutdownBackend()} was not yet called.
   *
   * @return <code>true</code> if this backend is already successfully
   *         initialized, <code>false</code> if not.
   */
  boolean isInitialized ();

  /**
   * Shutdown the backend. This may only be called if backend initialization was
   * successful.
   */
  void shutdownBackend ();

  /**
   * Write a new {@link PeppolReportingItem} to the data storage.
   *
   * @param aReportingItem
   *        The reporting item to write. Must not be <code>null</code>.
   * @throws PeppolReportingBackendException
   *         In case of an unrecoverable error
   */
  void storeReportingItem (@NonNull PeppolReportingItem aReportingItem) throws PeppolReportingBackendException;

  /**
   * Iterate all {@link PeppolReportingItem} objects in the provided date range,
   * in the correct order. If the iteration is lazy or eager depends on the
   * backing implementation.
   *
   * @param aStartDateIncl
   *        The date to start iterating, including this date. May not be
   *        <code>null</code>.
   * @param aEndDateIncl
   *        The date to stop iterating, including this date. May not be
   *        <code>null</code>. Must not be before the start date.
   * @return An {@link Iterable} over all matching reporting items.
   * @throws PeppolReportingBackendException
   *         In case of an unrecoverable error
   * @since 2.2.0
   */
  @NonNull
  Iterable <PeppolReportingItem> iterateReportingItems (@NonNull LocalDate aStartDateIncl,
                                                        @NonNull LocalDate aEndDateIncl) throws PeppolReportingBackendException;

  /**
   * Iterate all {@link PeppolReportingItem} objects in the provided month, in
   * the correct order. If the iteration is lazy or eager depends on the backing
   * implementation.
   *
   * @param aYearMonth
   *        The year and month to iterate. May not be <code>null</code>.
   * @return An {@link Iterable} over all matching reporting items.
   * @throws PeppolReportingBackendException
   *         In case of an unrecoverable error
   * @see #iterateReportingItems(LocalDate, LocalDate)
   * @since 2.2.0
   */
  @NonNull
  default Iterable <PeppolReportingItem> iterateReportingItems (@NonNull final YearMonth aYearMonth) throws PeppolReportingBackendException
  {
    return iterateReportingItems (aYearMonth.atDay (1), aYearMonth.atEndOfMonth ());
  }

  /**
   * Iterate all {@link PeppolReportingItem} objects in the provided date range,
   * in the correct order.
   *
   * @param aStartDateIncl
   *        The date to start iterating, including this date. May not be
   *        <code>null</code>.
   * @param aEndDateIncl
   *        The date to stop iterating, including this date. May not be
   *        <code>null</code>. Must not be before the start date.
   * @param aConsumer
   *        The consumer to be invoked for each {@link PeppolReportingItem}
   *        object found.
   * @throws PeppolReportingBackendException
   *         In case of an unrecoverable error
   * @see #iterateReportingItems(LocalDate, LocalDate)
   */
  default void forEachReportingItem (@NonNull final LocalDate aStartDateIncl,
                                     @NonNull final LocalDate aEndDateIncl,
                                     @NonNull final Consumer <? super PeppolReportingItem> aConsumer) throws PeppolReportingBackendException
  {
    iterateReportingItems (aStartDateIncl, aEndDateIncl).forEach (aConsumer);
  }

  /**
   * Iterate all {@link PeppolReportingItem} objects in the provided month, in
   * the correct order.
   *
   * @param aYearMonth
   *        The year and month to iterate. May not be <code>null</code>.
   * @param aConsumer
   *        The consumer to be invoked for each {@link PeppolReportingItem}
   *        object found.
   * @throws PeppolReportingBackendException
   *         In case of an unrecoverable error
   * @see #forEachReportingItem(LocalDate, LocalDate, Consumer)
   * @see #iterateReportingItems(YearMonth)
   */
  default void forEachReportingItem (@NonNull final YearMonth aYearMonth,
                                     @NonNull final Consumer <? super PeppolReportingItem> aConsumer) throws PeppolReportingBackendException
  {
    iterateReportingItems (aYearMonth).forEach (aConsumer);
  }
}
