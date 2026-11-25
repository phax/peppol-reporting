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
package com.helger.peppol.reporting.api.envelope;

import java.util.List;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.Immutable;
import com.helger.annotation.style.MustImplementEqualsAndHashcode;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.hashcode.HashCodeGenerator;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsHashSet;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsSet;
import com.helger.peppol.reporting.api.PeppolReportingItem;
import com.helger.peppol.reporting.api.PeppolReportingItemWithID;

/**
 * An envelope to transfer a batch of {@link PeppolReportingItem} objects from an Access Point to a
 * remote reporting collector. It contains the Seat ID of the reporting service provider, the ID of
 * the sending Access Point deployment (source ID) and between 1 and {@link #MAX_ITEMS} reporting
 * items, each with its own unique item ID. The combination of source ID and item ID identifies a
 * reporting item, so that a collector can ignore items that are submitted again.
 *
 * @author Philip Helger
 * @since 5.0.0
 */
@Immutable
@MustImplementEqualsAndHashcode
public final class PeppolReportingEnvelope
{
  /** The maximum length of the Seat ID */
  public static final int MAX_LEN_SEAT_ID = PeppolReportingItem.MAX_LEN_C2_ID;
  /** The maximum length of the source ID */
  public static final int MAX_LEN_SOURCE_ID = 64;
  /** The maximum number of reporting items in a single envelope */
  public static final int MAX_ITEMS = 100;

  private final String m_sSeatID;
  private final String m_sSourceID;
  private final ICommonsList <PeppolReportingItemWithID> m_aItems;

  /**
   * Constructor
   *
   * @param sSeatID
   *        The Seat ID of the reporting service provider. May neither be <code>null</code> nor empty
   *        and must not be longer than {@link #MAX_LEN_SEAT_ID} characters.
   * @param sSourceID
   *        The stable ID of the sending Access Point deployment. May neither be <code>null</code>
   *        nor empty and must not be longer than {@link #MAX_LEN_SOURCE_ID} characters.
   * @param aItems
   *        The reporting items with their IDs. May not be <code>null</code>, must contain between 1
   *        and {@link #MAX_ITEMS} non-<code>null</code> elements and all item IDs must be unique.
   * @throws IllegalArgumentException
   *         if one of the constraints is violated
   * @throws NullPointerException
   *         if a mandatory parameter is <code>null</code>
   */
  public PeppolReportingEnvelope (@NonNull @Nonempty final String sSeatID,
                                  @NonNull @Nonempty final String sSourceID,
                                  @NonNull @Nonempty final List <PeppolReportingItemWithID> aItems)
  {
    ValueEnforcer.notEmpty (sSeatID, "SeatID");
    ValueEnforcer.isTrue (sSeatID.length () <= MAX_LEN_SEAT_ID,
                          () -> "SeatID exceeds the maximum length of " + MAX_LEN_SEAT_ID);
    ValueEnforcer.notEmpty (sSourceID, "SourceID");
    ValueEnforcer.isTrue (sSourceID.length () <= MAX_LEN_SOURCE_ID,
                          () -> "SourceID exceeds the maximum length of " + MAX_LEN_SOURCE_ID);
    ValueEnforcer.notEmptyNoNullValue (aItems, "Items");
    ValueEnforcer.isTrue (aItems.size () <= MAX_ITEMS,
                          () -> "The envelope contains " +
                                aItems.size () +
                                " items but at most " +
                                MAX_ITEMS +
                                " are allowed");

    final ICommonsSet <String> aItemIDs = new CommonsHashSet <> ();
    for (final PeppolReportingItemWithID aItem : aItems)
      if (!aItemIDs.add (aItem.getItemID ()))
        throw new IllegalArgumentException ("The envelope contains the item ID '" +
                                            aItem.getItemID () +
                                            "' more than once");

    m_sSeatID = sSeatID;
    m_sSourceID = sSourceID;
    m_aItems = new CommonsArrayList <> (aItems);
  }

  /**
   * @return The Seat ID of the reporting service provider. Neither <code>null</code> nor empty.
   */
  @NonNull
  @Nonempty
  public String getSeatID ()
  {
    return m_sSeatID;
  }

  /**
   * @return The ID of the sending Access Point deployment. Neither <code>null</code> nor empty.
   */
  @NonNull
  @Nonempty
  public String getSourceID ()
  {
    return m_sSourceID;
  }

  /**
   * @return A copy of all contained reporting items with their IDs. Never <code>null</code> nor
   *         empty.
   */
  @NonNull
  @Nonempty
  @ReturnsMutableCopy
  public ICommonsList <PeppolReportingItemWithID> getAllItems ()
  {
    return m_aItems.getClone ();
  }

  /**
   * @return The number of contained reporting items. Always &gt; 0.
   */
  public int getItemCount ()
  {
    return m_aItems.size ();
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final PeppolReportingEnvelope rhs = (PeppolReportingEnvelope) o;
    return m_sSeatID.equals (rhs.m_sSeatID) && m_sSourceID.equals (rhs.m_sSourceID) && m_aItems.equals (rhs.m_aItems);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_sSeatID).append (m_sSourceID).append (m_aItems).getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("SeatID", m_sSeatID)
                                       .append ("SourceID", m_sSourceID)
                                       .append ("Items", m_aItems)
                                       .getToString ();
  }
}
