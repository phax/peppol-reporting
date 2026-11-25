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
package com.helger.peppol.reporting.api;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.Immutable;
import com.helger.annotation.style.MustImplementEqualsAndHashcode;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.hashcode.HashCodeGenerator;
import com.helger.base.tostring.ToStringGenerator;

/**
 * A {@link PeppolReportingItem} together with a unique item ID. The item ID is assigned by the
 * party that creates the reporting item (e.g. the transaction ID of the Access Point) and must stay
 * the same when the same item is submitted again, so that a receiver can detect duplicates.
 *
 * @author Philip Helger
 * @since 5.0.0
 */
@Immutable
@MustImplementEqualsAndHashcode
public final class PeppolReportingItemWithID
{
  /** The maximum length of an item ID */
  public static final int MAX_LEN_ITEM_ID = 128;

  private final String m_sItemID;
  private final PeppolReportingItem m_aReportingItem;

  /**
   * Constructor
   *
   * @param sItemID
   *        The unique item ID. May neither be <code>null</code> nor empty and must not be longer
   *        than {@link #MAX_LEN_ITEM_ID} characters.
   * @param aReportingItem
   *        The reporting item. May not be <code>null</code>.
   */
  public PeppolReportingItemWithID (@NonNull @Nonempty final String sItemID,
                                    @NonNull final PeppolReportingItem aReportingItem)
  {
    ValueEnforcer.notEmpty (sItemID, "ItemID");
    ValueEnforcer.isTrue (() -> sItemID.length () <= MAX_LEN_ITEM_ID,
                          () -> "ItemID exceeds the maximum length of " + MAX_LEN_ITEM_ID);
    ValueEnforcer.notNull (aReportingItem, "ReportingItem");
    m_sItemID = sItemID;
    m_aReportingItem = aReportingItem;
  }

  /**
   * @return The unique item ID. Neither <code>null</code> nor empty.
   */
  @NonNull
  @Nonempty
  public String getItemID ()
  {
    return m_sItemID;
  }

  /**
   * @return The contained reporting item. Never <code>null</code>.
   */
  @NonNull
  public PeppolReportingItem getReportingItem ()
  {
    return m_aReportingItem;
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final PeppolReportingItemWithID rhs = (PeppolReportingItemWithID) o;
    return m_sItemID.equals (rhs.m_sItemID) && m_aReportingItem.equals (rhs.m_aReportingItem);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_sItemID).append (m_aReportingItem).getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("ItemID", m_sItemID)
                                       .append ("ReportingItem", m_aReportingItem)
                                       .getToString ();
  }
}
