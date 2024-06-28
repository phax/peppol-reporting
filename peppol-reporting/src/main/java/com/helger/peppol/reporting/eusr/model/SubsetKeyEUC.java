/*
 * Copyright (C) 2022-2024 Philip Helger
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
package com.helger.peppol.reporting.eusr.model;

import javax.annotation.Nonnull;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.MustImplementEqualsAndHashcode;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.string.ToStringGenerator;

/**
 * Single Peppol EUSR Key for "End User Country".
 *
 * @author Philip Helger
 * @since 1.2.0
 */
@MustImplementEqualsAndHashcode
public final class SubsetKeyEUC implements IEUSRSubsetKey <SubsetKeyEUC>
{
  public static final String TYPE = "PerEUC";

  private final String m_sEndUserCC;

  public SubsetKeyEUC (@Nonnull @Nonempty final String sEndUserCountry)
  {
    ValueEnforcer.notEmpty (sEndUserCountry, "EndUserCountry");
    m_sEndUserCC = sEndUserCountry;
  }

  @Nonnull
  @Nonempty
  public String getEndUserCountryCode ()
  {
    return m_sEndUserCC;
  }

  public int compareTo (@Nonnull final SubsetKeyEUC rhs)
  {
    return m_sEndUserCC.compareTo (rhs.m_sEndUserCC);
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final SubsetKeyEUC rhs = (SubsetKeyEUC) o;
    return m_sEndUserCC.equals (rhs.m_sEndUserCC);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_sEndUserCC).getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("EndUserCC", m_sEndUserCC).getToString ();
  }
}
