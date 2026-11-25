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
package com.helger.peppol.reporting.backend.httpclient;

import java.util.List;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.peppol.reporting.api.backend.PeppolReportingBackendException;
import com.helger.peppol.reporting.api.envelope.PeppolReportingRejectedItem;

/**
 * Exception thrown by {@link PeppolReportingBackendHttpClient} if the remote collector rejected an
 * envelope because of invalid reporting items. None of the items of the rejected envelope were
 * stored by the collector. The caller should set the rejected items aside and store the remaining
 * items again.
 *
 * @author Philip Helger
 * @since 5.0.0
 */
public class PeppolReportingSubmissionRejectedException extends PeppolReportingBackendException
{
  private final ICommonsList <PeppolReportingRejectedItem> m_aRejectedItems;

  public PeppolReportingSubmissionRejectedException (@NonNull final String sMsg,
                                                     @NonNull @Nonempty final List <PeppolReportingRejectedItem> aRejectedItems)
  {
    super (sMsg);
    m_aRejectedItems = new CommonsArrayList <> (aRejectedItems);
  }

  /**
   * @return A copy of all items rejected by the collector. Never <code>null</code> nor empty.
   */
  @NonNull
  @Nonempty
  @ReturnsMutableCopy
  public ICommonsList <PeppolReportingRejectedItem> getAllRejectedItems ()
  {
    return m_aRejectedItems.getClone ();
  }
}
