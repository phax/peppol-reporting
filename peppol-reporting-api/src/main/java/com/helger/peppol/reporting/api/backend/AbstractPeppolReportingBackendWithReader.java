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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.peppol.reporting.api.PeppolReportingItem;

/**
 * Abstract base class for {@link IPeppolReportingBackend} implementations that can also read the
 * stored reporting items. It validates the arguments of
 * {@link #forEachReportingItem(LocalDate, LocalDate, Consumer)} and rejects calls after
 * {@link #close()}.
 *
 * @author Philip Helger
 * @since 5.0.0
 */
public abstract class AbstractPeppolReportingBackendWithReader extends AbstractPeppolReportingBackend implements
                                                               IPeppolReportingItemReader
{
  private static final Logger LOGGER = LoggerFactory.getLogger (AbstractPeppolReportingBackendWithReader.class);

  protected AbstractPeppolReportingBackendWithReader (@NonNull @Nonempty final String sDisplayName)
  {
    super (sDisplayName);
  }

  /**
   * Validate a date range for reading.
   *
   * @param aStartDateIncl
   *        The inclusive start date. May not be <code>null</code>.
   * @param aEndDateIncl
   *        The inclusive end date. May not be <code>null</code> and must not be before the start
   *        date.
   */
  protected static void validateDateRange (@NonNull final LocalDate aStartDateIncl,
                                           @NonNull final LocalDate aEndDateIncl)
  {
    ValueEnforcer.notNull (aStartDateIncl, "StartDateIncl");
    ValueEnforcer.notNull (aEndDateIncl, "EndDateIncl");
    ValueEnforcer.isTrue ( () -> aEndDateIncl.compareTo (aStartDateIncl) >= 0, "EndDateIncl must be >= StartDateIncl");
  }

  /**
   * Pass all reporting items in the provided date range to the consumer. The arguments are already
   * validated and the backend is not closed.
   *
   * @param aStartDateIncl
   *        The inclusive start date. Never <code>null</code>.
   * @param aEndDateIncl
   *        The inclusive end date. Never <code>null</code> and not before the start date.
   * @param aConsumer
   *        The consumer to be invoked for each reporting item. Never <code>null</code>.
   * @throws PeppolReportingBackendException
   *         In case of an unrecoverable error
   */
  protected abstract void forEachReportingItemInRange (@NonNull LocalDate aStartDateIncl,
                                                       @NonNull LocalDate aEndDateIncl,
                                                       @NonNull Consumer <? super PeppolReportingItem> aConsumer) throws PeppolReportingBackendException;

  public final void forEachReportingItem (@NonNull final LocalDate aStartDateIncl,
                                          @NonNull final LocalDate aEndDateIncl,
                                          @NonNull final Consumer <? super PeppolReportingItem> aConsumer) throws PeppolReportingBackendException
  {
    validateDateRange (aStartDateIncl, aEndDateIncl);
    ValueEnforcer.notNull (aConsumer, "Consumer");
    checkNotClosed ();

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Querying Peppol Reporting Items from " +
                    getDisplayName () +
                    " between " +
                    aStartDateIncl +
                    " and " +
                    aEndDateIncl);

    forEachReportingItemInRange (aStartDateIncl, aEndDateIncl, aConsumer);
  }
}
