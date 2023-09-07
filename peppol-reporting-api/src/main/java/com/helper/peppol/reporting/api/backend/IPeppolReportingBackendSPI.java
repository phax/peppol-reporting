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

import java.time.LocalDate;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.IsSPIInterface;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.name.IHasDisplayName;
import com.helper.peppol.reporting.api.PeppolReportingItem;

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
  @Nonnull
  @Nonempty
  String getDisplayName ();

  /**
   * Write a new {@link PeppolReportingItem} to the data storage.
   *
   * @param aReportingItem
   *        The reporting item to write. Must not be <code>null</code>.
   * @throws PeppolReportingBackendException
   *         In case of an unrecoverable error
   */
  void storeReportingItem (@Nonnull PeppolReportingItem aReportingItem) throws PeppolReportingBackendException;

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
   */
  void forEachReportingItem (@Nonnull LocalDate aStartDateIncl,
                             @Nonnull LocalDate aEndDateIncl,
                             @Nonnull Consumer <? super PeppolReportingItem> aConsumer) throws PeppolReportingBackendException;
}
