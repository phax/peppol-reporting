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
package com.helger.peppol.reporting.backend.inmemory;

import java.time.LocalDate;
import java.util.function.Consumer;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.GuardedBy;
import com.helger.annotation.concurrent.ThreadSafe;
import com.helger.base.concurrent.SimpleReadWriteLock;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsHashMap;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsMap;
import com.helger.peppol.reporting.api.PeppolReportingItem;
import com.helger.peppol.reporting.api.backend.AbstractPeppolReportingBackendWithReader;

/**
 * Peppol Reporting backend that keeps all reporting items in memory. Intended for testing only -
 * all data is lost when the backend is closed.
 *
 * @author Philip Helger
 * @since 5.0.0
 */
@ThreadSafe
public class PeppolReportingBackendInMemory extends AbstractPeppolReportingBackendWithReader
{
  public static final String DISPLAY_NAME = "InMemory";

  private final SimpleReadWriteLock m_aRWLock = new SimpleReadWriteLock ();
  @GuardedBy ("m_aRWLock")
  private final ICommonsMap <LocalDate, ICommonsList <PeppolReportingItem>> m_aMap = new CommonsHashMap <> ();

  public PeppolReportingBackendInMemory ()
  {
    super (DISPLAY_NAME);
  }

  @Override
  protected void storeEligibleReportingItem (@NonNull @Nonempty final String sItemID,
                                             @NonNull final PeppolReportingItem aReportingItem)
  {
    m_aRWLock.writeLocked ( () -> m_aMap.computeIfAbsent (aReportingItem.getExchangeDTUTC ().toLocalDate (),
                                                           k -> new CommonsArrayList <> ()).add (aReportingItem));
  }

  @Override
  protected void forEachReportingItemInRange (@NonNull final LocalDate aStartDateIncl,
                                              @NonNull final LocalDate aEndDateIncl,
                                              @NonNull final Consumer <? super PeppolReportingItem> aConsumer)
  {
    LocalDate aCurDate = aStartDateIncl;
    while (aCurDate.compareTo (aEndDateIncl) <= 0)
    {
      // Copy the items of one day while holding the lock, so that the consumer
      // is invoked outside of the lock
      final LocalDate aDate = aCurDate;
      final ICommonsList <PeppolReportingItem> aAllItemsOfDate = m_aRWLock.readLockedGet ( () -> {
        final ICommonsList <PeppolReportingItem> aList = m_aMap.get (aDate);
        return aList == null ? null : aList.getClone ();
      });
      if (aAllItemsOfDate != null)
        aAllItemsOfDate.forEach (aConsumer);

      aCurDate = aCurDate.plusDays (1);
    }
  }

  @Override
  protected void onClose ()
  {
    m_aRWLock.writeLocked (m_aMap::clear);
  }
}
