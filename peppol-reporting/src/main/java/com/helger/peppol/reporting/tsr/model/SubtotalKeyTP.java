/*
 * Copyright (C) 2022-2025 Philip Helger
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

import com.helger.annotation.Nonempty;
import com.helger.annotation.style.MustImplementEqualsAndHashcode;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.hashcode.HashCodeGenerator;
import com.helger.base.tostring.ToStringGenerator;

import jakarta.annotation.Nonnull;

/**
 * Single Peppol TSR Key for "Transport Protocol".
 *
 * @author Philip Helger
 * @since 1.2.0
 */
@MustImplementEqualsAndHashcode
public final class SubtotalKeyTP implements ITSRSubtotalKey <SubtotalKeyTP>
{
  public static final String TYPE = "PerTP";

  private final String m_sTP;

  public SubtotalKeyTP (@Nonnull @Nonempty final String sTP)
  {
    ValueEnforcer.notEmpty (sTP, "TransportProtocol");
    m_sTP = sTP;
  }

  @Nonnull
  @Nonempty
  public String getTransportProtocol ()
  {
    return m_sTP;
  }

  public int compareTo (@Nonnull final SubtotalKeyTP rhs)
  {
    return m_sTP.compareTo (rhs.m_sTP);
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final SubtotalKeyTP rhs = (SubtotalKeyTP) o;
    return m_sTP.equals (rhs.m_sTP);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_sTP).getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("TransportProtocol", m_sTP).getToString ();
  }
}
