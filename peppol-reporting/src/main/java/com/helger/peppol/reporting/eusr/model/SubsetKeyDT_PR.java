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

import com.helger.annotation.Nonempty;
import com.helger.annotation.style.MustImplementEqualsAndHashcode;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.hashcode.HashCodeGenerator;
import com.helger.base.tostring.ToStringGenerator;

import jakarta.annotation.Nonnull;

/**
 * Single Peppol EUSR Key for "Document Type ID and Process ID".
 *
 * @author Philip Helger
 * @since 1.2.0
 */
@MustImplementEqualsAndHashcode
public final class SubsetKeyDT_PR implements IEUSRSubsetKey <SubsetKeyDT_PR>
{
  public static final String TYPE = "PerDT-PR";

  private final String m_sDocTypeIDScheme;
  private final String m_sDocTypeIDValue;
  private final String m_sProcessIDScheme;
  private final String m_sProcessIDValue;

  public SubsetKeyDT_PR (@Nonnull @Nonempty final String sDocTypeIDScheme,
                         @Nonnull @Nonempty final String sDocTypeIDValue,
                         @Nonnull @Nonempty final String sProcessIDScheme,
                         @Nonnull @Nonempty final String sProcessIDValue)
  {
    ValueEnforcer.notEmpty (sDocTypeIDScheme, "DocTypeIDScheme");
    ValueEnforcer.notEmpty (sDocTypeIDValue, "DocTypeIDValue");
    ValueEnforcer.notEmpty (sProcessIDScheme, "ProcessIDScheme");
    ValueEnforcer.notEmpty (sProcessIDValue, "ProcessIDValue");
    m_sDocTypeIDScheme = sDocTypeIDScheme;
    m_sDocTypeIDValue = sDocTypeIDValue;
    m_sProcessIDScheme = sProcessIDScheme;
    m_sProcessIDValue = sProcessIDValue;
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
  public String getProcessIDScheme ()
  {
    return m_sProcessIDScheme;
  }

  @Nonnull
  @Nonempty
  public String getProcessIDValue ()
  {
    return m_sProcessIDValue;
  }

  public int compareTo (@Nonnull final SubsetKeyDT_PR rhs)
  {
    int ret = m_sDocTypeIDScheme.compareTo (rhs.m_sDocTypeIDScheme);
    if (ret == 0)
    {
      ret = m_sDocTypeIDValue.compareTo (rhs.m_sDocTypeIDValue);
      if (ret == 0)
      {
        ret = m_sProcessIDScheme.compareTo (rhs.m_sProcessIDScheme);
        if (ret == 0)
          ret = m_sProcessIDValue.compareTo (rhs.m_sProcessIDValue);
      }
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
    final SubsetKeyDT_PR rhs = (SubsetKeyDT_PR) o;
    return m_sDocTypeIDScheme.equals (rhs.m_sDocTypeIDScheme) &&
           m_sDocTypeIDValue.equals (rhs.m_sDocTypeIDValue) &&
           m_sProcessIDScheme.equals (rhs.m_sProcessIDScheme) &&
           m_sProcessIDValue.equals (rhs.m_sProcessIDValue);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_sDocTypeIDScheme)
                                       .append (m_sDocTypeIDValue)
                                       .append (m_sProcessIDScheme)
                                       .append (m_sProcessIDValue)
                                       .getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("DocTypeIDScheme", m_sDocTypeIDScheme)
                                       .append ("DocTypeIDValue", m_sDocTypeIDValue)
                                       .append ("ProcessIDScheme", m_sProcessIDScheme)
                                       .append ("ProcessIDValue", m_sProcessIDValue)
                                       .getToString ();
  }
}
