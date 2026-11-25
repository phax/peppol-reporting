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
import java.time.YearMonth;
import java.util.function.Consumer;

import org.jspecify.annotations.NonNull;

import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.peppol.reporting.api.PeppolReportingItem;

/**
 * Read access to stored {@link PeppolReportingItem} objects. It is implemented by all
 * {@link IPeppolReportingBackend} implementations that store the reporting items themselves and is
 * required to create reports. Use <code>instanceof</code> to check if a backend supports it.
 * <p>
 * <b>Ordering is unspecified.</b> Different backends return matching items in different orders
 * (e.g. insertion order, storage order, or sorted by exchange timestamp). Callers that require a
 * specific order must sort the result themselves. The only guarantee is that all items whose
 * exchange date (in UTC) falls within the inclusive range are returned exactly once.
 *
 * @author Philip Helger
 * @since 5.0.0
 */
public interface IPeppolReportingItemReader
{
  /**
   * Pass all {@link PeppolReportingItem} objects in the provided date range to the consumer. Both
   * the start and the end date are <b>inclusive</b>. Implementations should pass each item to the
   * consumer while reading it from the storage, without collecting all items in memory first. An
   * exception thrown by the consumer stops the iteration and is propagated to the caller.
   *
   * @param aStartDateIncl
   *        The date to start iterating, including this date. May not be <code>null</code>.
   * @param aEndDateIncl
   *        The date to stop iterating, including this date. May not be <code>null</code>. Must not
   *        be before the start date.
   * @param aConsumer
   *        The consumer to be invoked for each {@link PeppolReportingItem} object found. May not
   *        be <code>null</code>.
   * @throws PeppolReportingBackendException
   *         In case of an unrecoverable error
   * @throws IllegalStateException
   *         If the backend is already closed
   */
  void forEachReportingItem (@NonNull LocalDate aStartDateIncl,
                             @NonNull LocalDate aEndDateIncl,
                             @NonNull Consumer <? super PeppolReportingItem> aConsumer) throws PeppolReportingBackendException;

  /**
   * Pass all {@link PeppolReportingItem} objects in the provided month to the consumer.
   *
   * @param aYearMonth
   *        The year and month to iterate. May not be <code>null</code>.
   * @param aConsumer
   *        The consumer to be invoked for each {@link PeppolReportingItem} object found. May not
   *        be <code>null</code>.
   * @throws PeppolReportingBackendException
   *         In case of an unrecoverable error
   * @see #forEachReportingItem(LocalDate, LocalDate, Consumer)
   */
  default void forEachReportingItem (@NonNull final YearMonth aYearMonth,
                                     @NonNull final Consumer <? super PeppolReportingItem> aConsumer) throws PeppolReportingBackendException
  {
    forEachReportingItem (aYearMonth.atDay (1), aYearMonth.atEndOfMonth (), aConsumer);
  }

  /**
   * Get all {@link PeppolReportingItem} objects in the provided date range. Both the start and the
   * end date are <b>inclusive</b>. The default implementation collects all items in memory, so
   * prefer {@link #forEachReportingItem(LocalDate, LocalDate, Consumer)} for large amounts of
   * items.
   *
   * @param aStartDateIncl
   *        The date to start iterating, including this date. May not be <code>null</code>.
   * @param aEndDateIncl
   *        The date to stop iterating, including this date. May not be <code>null</code>. Must not
   *        be before the start date.
   * @return An {@link Iterable} over all matching reporting items. Never <code>null</code>.
   * @throws PeppolReportingBackendException
   *         In case of an unrecoverable error
   * @throws IllegalStateException
   *         If the backend is already closed
   */
  @NonNull
  default Iterable <PeppolReportingItem> iterateReportingItems (@NonNull final LocalDate aStartDateIncl,
                                                                @NonNull final LocalDate aEndDateIncl) throws PeppolReportingBackendException
  {
    final ICommonsList <PeppolReportingItem> ret = new CommonsArrayList <> ();
    forEachReportingItem (aStartDateIncl, aEndDateIncl, ret::add);
    return ret;
  }

  /**
   * Get all {@link PeppolReportingItem} objects in the provided month.
   *
   * @param aYearMonth
   *        The year and month to iterate. May not be <code>null</code>.
   * @return An {@link Iterable} over all matching reporting items. Never <code>null</code>.
   * @throws PeppolReportingBackendException
   *         In case of an unrecoverable error
   * @see #iterateReportingItems(LocalDate, LocalDate)
   */
  @NonNull
  default Iterable <PeppolReportingItem> iterateReportingItems (@NonNull final YearMonth aYearMonth) throws PeppolReportingBackendException
  {
    return iterateReportingItems (aYearMonth.atDay (1), aYearMonth.atEndOfMonth ());
  }
}
