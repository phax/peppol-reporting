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
package com.helger.peppol.reporting.eusr.model;

import javax.annotation.Nonnull;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.MustImplementEqualsAndHashcode;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.string.ToStringGenerator;

/**
 * Single Peppol EUSR Key for "Document Type ID and End User Country".
 *
 * @author Philip Helger
 * @since 1.2.0
 */
@MustImplementEqualsAndHashcode
public final class SubsetKeyDT_EUC implements IEUSRSubsetKey <SubsetKeyDT_EUC>
{
  public static final String TYPE = "PerDT-EUC";

  private final String m_sDocTypeIDScheme;
  private final String m_sDocTypeIDValue;
  private final String m_sEndUserCC;

  public SubsetKeyDT_EUC (@Nonnull @Nonempty final String sDocTypeIDScheme,
                          @Nonnull @Nonempty final String sDocTypeIDValue,
                          @Nonnull @Nonempty final String sEndUserCountry)
  {
    ValueEnforcer.notEmpty (sDocTypeIDScheme, "DocTypeIDScheme");
    ValueEnforcer.notEmpty (sDocTypeIDValue, "DocTypeIDValue");
    ValueEnforcer.notEmpty (sEndUserCountry, "EndUserCountry");
    m_sDocTypeIDScheme = sDocTypeIDScheme;
    m_sDocTypeIDValue = sDocTypeIDValue;
    m_sEndUserCC = sEndUserCountry;
  }

  @Nonnull
  @Nonempty
  public String getDocTypeIDScheme ()
  {
    return m_sDocTypeIDScheme;
  }

  @Nonnull
  @Nonempty
  public String getDocTypeIDValue ()
  {
    return m_sDocTypeIDValue;
  }

  @Nonnull
  @Nonempty
  public String getEndUserCountryCode ()
  {
    return m_sEndUserCC;
  }

  public int compareTo (@Nonnull final SubsetKeyDT_EUC rhs)
  {
    int ret = m_sDocTypeIDScheme.compareTo (rhs.m_sDocTypeIDScheme);
    if (ret == 0)
    {
      ret = m_sDocTypeIDValue.compareTo (rhs.m_sDocTypeIDValue);
      if (ret == 0)
        ret = m_sEndUserCC.compareTo (rhs.m_sEndUserCC);
    }
    return ret;
  }

  @Override
  public boolean equals (final Object o)
  {
    if (o == this)
      return true;
    if (o == null || !getClass ().equals (o.getClass ()))
      return false;
    final SubsetKeyDT_EUC rhs = (SubsetKeyDT_EUC) o;
    return m_sDocTypeIDScheme.equals (rhs.m_sDocTypeIDScheme) &&
           m_sDocTypeIDValue.equals (rhs.m_sDocTypeIDValue) &&
           m_sEndUserCC.equals (rhs.m_sEndUserCC);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_sDocTypeIDScheme)
                                       .append (m_sDocTypeIDValue)
                                       .append (m_sEndUserCC)
                                       .getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("DocTypeIDScheme", m_sDocTypeIDScheme)
                                       .append ("DocTypeIDValue", m_sDocTypeIDValue)
                                       .append ("EndUserCC", m_sEndUserCC)
                                       .getToString ();
  }
}
