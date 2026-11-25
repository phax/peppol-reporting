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

import com.helger.annotation.Nonnegative;
import com.helger.annotation.concurrent.Immutable;
import com.helger.annotation.style.MustImplementEqualsAndHashcode;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.hashcode.HashCodeGenerator;
import com.helger.base.tostring.ToStringGenerator;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;

/**
 * The response of a remote reporting collector to the submission of a
 * {@link PeppolReportingEnvelope}. An envelope is accepted or rejected as a whole:
 * <ul>
 * <li>If it is accepted, the list of rejected items is empty. The number of newly stored items and
 * the number of items that were already stored before (duplicates) are provided.</li>
 * <li>If it is rejected, none of the items were stored, and the list of rejected items names the
 * invalid items, so that the sender can set them aside and submit the remaining items again.</li>
 * </ul>
 *
 * @author Philip Helger
 * @since 4.6.0
 */
@Immutable
@MustImplementEqualsAndHashcode
public final class PeppolReportingSubmissionResponse
{
  private final int m_nAcceptedCount;
  private final int m_nDuplicateCount;
  private final ICommonsList <PeppolReportingRejectedItem> m_aRejectedItems;

  /**
   * Constructor
   *
   * @param nAcceptedCount
   *        The number of newly stored items. Must be &ge; 0.
   * @param nDuplicateCount
   *        The number of items that were already stored before and were therefore ignored. Must be
   *        &ge; 0.
   * @param aRejectedItems
   *        The rejected items. May not be <code>null</code> but may be empty. If it is not empty,
   *        the accepted and the duplicate count must both be 0.
   */
  public PeppolReportingSubmissionResponse (@Nonnegative final int nAcceptedCount,
                                            @Nonnegative final int nDuplicateCount,
                                            @NonNull final List <PeppolReportingRejectedItem> aRejectedItems)
  {
    ValueEnforcer.isGE0 (nAcceptedCount, "AcceptedCount");
    ValueEnforcer.isGE0 (nDuplicateCount, "DuplicateCount");
    ValueEnforcer.notNullNoNullValue (aRejectedItems, "RejectedItems");
    ValueEnforcer.isTrue (aRejectedItems.isEmpty () || (nAcceptedCount == 0 && nDuplicateCount == 0),
                          "A rejected envelope cannot contain accepted or duplicate items");
    m_nAcceptedCount = nAcceptedCount;
    m_nDuplicateCount = nDuplicateCount;
    m_aRejectedItems = new CommonsArrayList <> (aRejectedItems);
  }

  /**
   * @return The number of newly stored items. Always &ge; 0.
   */
  @Nonnegative
  public int getAcceptedCount ()
  {
    return m_nAcceptedCount;
  }

  /**
   * @return The number of items that were already stored before and were therefore ignored. Always
   *         &ge; 0.
   */
  @Nonnegative
  public int getDuplicateCount ()
  {
    return m_nDuplicateCount;
  }

  /**
   * @return <code>true</code> if the envelope was accepted, <code>false</code> if it was rejected.
   */
  public boolean isAccepted ()
  {
    return m_aRejectedItems.isEmpty ();
  }

  /**
   * @return A copy of all rejected items. Never <code>null</code> but maybe empty.
   */
  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <PeppolReportingRejectedItem> getAllRejectedItems ()
  {
    return m_aRejectedItems.getClone ();
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final PeppolReportingSubmissionResponse rhs = (PeppolReportingSubmissionResponse) o;
    return m_nAcceptedCount == rhs.m_nAcceptedCount &&
           m_nDuplicateCount == rhs.m_nDuplicateCount &&
           m_aRejectedItems.equals (rhs.m_aRejectedItems);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_nAcceptedCount)
                                       .append (m_nDuplicateCount)
                                       .append (m_aRejectedItems)
                                       .getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("AcceptedCount", m_nAcceptedCount)
                                       .append ("DuplicateCount", m_nDuplicateCount)
                                       .append ("RejectedItems", m_aRejectedItems)
                                       .getToString ();
  }

  /**
   * Create a response for an accepted envelope.
   *
   * @param nAcceptedCount
   *        The number of newly stored items. Must be &ge; 0.
   * @param nDuplicateCount
   *        The number of items that were already stored before. Must be &ge; 0.
   * @return The new response and never <code>null</code>.
   */
  @NonNull
  public static PeppolReportingSubmissionResponse createAccepted (@Nonnegative final int nAcceptedCount,
                                                                  @Nonnegative final int nDuplicateCount)
  {
    return new PeppolReportingSubmissionResponse (nAcceptedCount, nDuplicateCount, new CommonsArrayList <> ());
  }

  /**
   * Create a response for a rejected envelope.
   *
   * @param aRejectedItems
   *        The rejected items. May neither be <code>null</code> nor empty.
   * @return The new response and never <code>null</code>.
   */
  @NonNull
  public static PeppolReportingSubmissionResponse createRejected (@NonNull final List <PeppolReportingRejectedItem> aRejectedItems)
  {
    ValueEnforcer.notEmpty (aRejectedItems, "RejectedItems");
    return new PeppolReportingSubmissionResponse (0, 0, aRejectedItems);
  }
}
