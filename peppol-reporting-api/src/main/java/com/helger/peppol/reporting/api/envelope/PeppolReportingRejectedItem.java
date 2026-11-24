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

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.Immutable;
import com.helger.annotation.style.MustImplementEqualsAndHashcode;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.hashcode.HashCodeGenerator;
import com.helger.base.tostring.ToStringGenerator;

/**
 * A single reporting item of a {@link PeppolReportingEnvelope} that was rejected by the receiver,
 * together with the reason for the rejection.
 *
 * @author Philip Helger
 * @since 4.6.0
 */
@Immutable
@MustImplementEqualsAndHashcode
public final class PeppolReportingRejectedItem
{
  private final String m_sItemID;
  private final String m_sReason;

  /**
   * Constructor
   *
   * @param sItemID
   *        The ID of the rejected item. May neither be <code>null</code> nor empty.
   * @param sReason
   *        The human readable reason for the rejection. May neither be <code>null</code> nor empty.
   */
  public PeppolReportingRejectedItem (@NonNull @Nonempty final String sItemID,
                                      @NonNull @Nonempty final String sReason)
  {
    ValueEnforcer.notEmpty (sItemID, "ItemID");
    ValueEnforcer.notEmpty (sReason, "Reason");
    m_sItemID = sItemID;
    m_sReason = sReason;
  }

  /**
   * @return The ID of the rejected item. Neither <code>null</code> nor empty.
   */
  @NonNull
  @Nonempty
  public String getItemID ()
  {
    return m_sItemID;
  }

  /**
   * @return The human readable reason for the rejection. Neither <code>null</code> nor empty.
   */
  @NonNull
  @Nonempty
  public String getReason ()
  {
    return m_sReason;
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final PeppolReportingRejectedItem rhs = (PeppolReportingRejectedItem) o;
    return m_sItemID.equals (rhs.m_sItemID) && m_sReason.equals (rhs.m_sReason);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_sItemID).append (m_sReason).getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("ItemID", m_sItemID).append ("Reason", m_sReason).getToString ();
  }
}
