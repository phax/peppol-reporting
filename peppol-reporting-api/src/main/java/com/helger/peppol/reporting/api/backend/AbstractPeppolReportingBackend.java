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

import java.util.concurrent.atomic.AtomicBoolean;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.annotation.style.OverrideOnDemand;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.peppol.reporting.api.PeppolReportingHelper;
import com.helger.peppol.reporting.api.PeppolReportingItem;
import com.helger.peppolid.CIdentifier;

/**
 * Abstract base class for {@link IPeppolReportingBackend} implementations. It validates the
 * arguments, rejects calls after {@link #close()} and skips reporting items with document types that
 * are not eligible for reporting.
 *
 * @author Philip Helger
 * @since 5.0.0
 */
public abstract class AbstractPeppolReportingBackend implements IPeppolReportingBackend
{
  private static final Logger LOGGER = LoggerFactory.getLogger (AbstractPeppolReportingBackend.class);

  private final String m_sDisplayName;
  private final AtomicBoolean m_aClosed = new AtomicBoolean (false);

  protected AbstractPeppolReportingBackend (@NonNull @Nonempty final String sDisplayName)
  {
    ValueEnforcer.notEmpty (sDisplayName, "DisplayName");
    m_sDisplayName = sDisplayName;
  }

  @NonNull
  @Nonempty
  public final String getDisplayName ()
  {
    return m_sDisplayName;
  }

  /**
   * @return <code>true</code> if {@link #close()} was called, <code>false</code> if not.
   */
  public final boolean isClosed ()
  {
    return m_aClosed.get ();
  }

  /**
   * Ensure that the backend was not yet closed.
   *
   * @throws IllegalStateException
   *         If the backend is already closed
   */
  protected final void checkNotClosed ()
  {
    if (isClosed ())
      throw new IllegalStateException ("The Peppol Reporting " + m_sDisplayName + " backend is already closed");
  }

  /**
   * Store a reporting item that is eligible for reporting. The arguments are already validated and
   * the backend is not closed.
   *
   * @param sItemID
   *        The unique ID of the reporting item. Neither <code>null</code> nor empty.
   * @param aReportingItem
   *        The reporting item to store. Never <code>null</code>.
   * @throws PeppolReportingBackendException
   *         In case of an unrecoverable error
   */
  protected abstract void storeEligibleReportingItem (@NonNull @Nonempty String sItemID,
                                                      @NonNull PeppolReportingItem aReportingItem) throws PeppolReportingBackendException;

  public final void storeReportingItem (@NonNull @Nonempty final String sItemID,
                                        @NonNull final PeppolReportingItem aReportingItem) throws PeppolReportingBackendException
  {
    ValueEnforcer.notEmpty (sItemID, "ItemID");
    ValueEnforcer.notNull (aReportingItem, "ReportingItem");
    checkNotClosed ();

    if (PeppolReportingHelper.isDocumentTypeEligableForReporting (aReportingItem.getDocTypeIDScheme (),
                                                                  aReportingItem.getDocTypeIDValue ()))
    {
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Trying to store Peppol Reporting Item '" + sItemID + "' in " + m_sDisplayName);

      storeEligibleReportingItem (sItemID, aReportingItem);

      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Successfully stored Peppol Reporting Item '" + sItemID + "' in " + m_sDisplayName);
    }
    else
    {
      LOGGER.info ("Not storing Peppol Reporting Item '" +
                   sItemID +
                   "' in " +
                   m_sDisplayName +
                   ", as the document type is not eligible for reporting (" +
                   CIdentifier.getURIEncoded (aReportingItem.getDocTypeIDScheme (),
                                              aReportingItem.getDocTypeIDValue ()) +
                   ")");
    }
  }

  /**
   * Release all resources of the backend. Called exactly once by {@link #close()}.
   */
  @OverrideOnDemand
  protected void onClose ()
  {}

  public final void close ()
  {
    if (m_aClosed.compareAndSet (false, true))
    {
      LOGGER.info ("Closing Peppol Reporting " + m_sDisplayName + " backend");
      onClose ();
    }
  }
}
