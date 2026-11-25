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

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.GuardedBy;
import com.helger.base.concurrent.SimpleReadWriteLock;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.peppol.reporting.api.PeppolReportingItem;

/**
 * A simple in-memory backend for testing.
 *
 * @author Philip Helger
 */
final class MockPeppolReportingBackend extends AbstractPeppolReportingBackendWithReader
{
  private final SimpleReadWriteLock m_aRWLock = new SimpleReadWriteLock ();
  @GuardedBy ("m_aRWLock")
  private final ICommonsList <PeppolReportingItem> m_aItems = new CommonsArrayList <> ();

  MockPeppolReportingBackend (@NonNull @Nonempty final String sDisplayName)
  {
    super (sDisplayName);
  }

  @Override
  protected void storeEligibleReportingItem (@NonNull @Nonempty final String sItemID,
                                             @NonNull final PeppolReportingItem aReportingItem)
  {
    m_aRWLock.writeLocked ( () -> m_aItems.add (aReportingItem));
  }

  @Override
  protected void forEachReportingItemInRange (@NonNull final LocalDate aStartDateIncl,
                                              @NonNull final LocalDate aEndDateIncl,
                                              @NonNull final Consumer <? super PeppolReportingItem> aConsumer)
  {
    for (final PeppolReportingItem aItem : m_aRWLock.readLockedGet (m_aItems::getClone))
    {
      final LocalDate aDate = aItem.getExchangeDTUTC ().toLocalDate ();
      if (aDate.compareTo (aStartDateIncl) >= 0 && aDate.compareTo (aEndDateIncl) <= 0)
        aConsumer.accept (aItem);
    }
  }
}
