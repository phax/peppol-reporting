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
package com.helger.peppol.reporting.tsr.model;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.peppol.reporting.api.PeppolReportingItem;
import com.helger.peppol.reporting.jaxb.tsr.v101.TransactionStatisticsReportType;

/**
 * This class represents a set of {@link PeppolReportingItem} objects for a single Reporting Period
 * used to create TSR reports.
 *
 * @author Philip Helger
 * @since 1.2.0
 */
@Immutable
public class TSRReportingItemList
{
  private TSRReportingItemList ()
  {}

  public static void fillReportSubsets (@NonNull final Iterable <? extends PeppolReportingItem> aList,
                                        @NonNull final TransactionStatisticsReportType aReport)
  {
    ValueEnforcer.notNull (aList, "List");
    ValueEnforcer.notNull (aReport, "Report");

    final TSRReportingItemAccumulator aAccumulator = new TSRReportingItemAccumulator ();
    for (final PeppolReportingItem aItem : aList)
      aAccumulator.accept (aItem);
    aAccumulator.fillReport (aReport);
  }
}
