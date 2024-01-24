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
package com.helper.peppol.reporting.eusr.model;

import javax.annotation.Nonnull;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.MustImplementEqualsAndHashcode;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.string.ToStringGenerator;

/**
 * Single Peppol EUSR Key for "Document Type ID, Process ID and End User
 * Country".
 *
 * @author Philip Helger
 * @since 1.2.0
 */
@MustImplementEqualsAndHashcode
public final class SubsetKeyDT_PR_EUC implements IEUSRSubsetKey <SubsetKeyDT_PR_EUC>
{
  public static final String TYPE = "PerDT-PR-EUC";

  private final String m_sDocTypeIDScheme;
  private final String m_sDocTypeIDValue;
  private final String m_sProcessIDScheme;
  private final String m_sProcessIDValue;
  private final String m_sEndUserCC;

  public SubsetKeyDT_PR_EUC (@Nonnull @Nonempty final String sDocTypeIDScheme,
                             @Nonnull @Nonempty final String sDocTypeIDValue,
                             @Nonnull @Nonempty final String sProcessIDScheme,
                             @Nonnull @Nonempty final String sProcessIDValue,
                             @Nonnull @Nonempty final String sEndUserCC)
  {
    ValueEnforcer.notEmpty (sDocTypeIDScheme, "DocTypeIDScheme");
    ValueEnforcer.notEmpty (sDocTypeIDValue, "DocTypeIDValue");
    ValueEnforcer.notEmpty (sProcessIDScheme, "ProcessIDScheme");
    ValueEnforcer.notEmpty (sProcessIDValue, "ProcessIDValue");
    ValueEnforcer.notEmpty (sEndUserCC, "EndUserCC");
    m_sDocTypeIDScheme = sDocTypeIDScheme;
    m_sDocTypeIDValue = sDocTypeIDValue;
    m_sProcessIDScheme = sProcessIDScheme;
    m_sProcessIDValue = sProcessIDValue;
    m_sEndUserCC = sEndUserCC;
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

  @Nonnull
  @Nonempty
  public String getEndUserCountryCode ()
  {
    return m_sEndUserCC;
  }

  public int compareTo (@Nonnull final SubsetKeyDT_PR_EUC rhs)
  {
    int ret = m_sDocTypeIDScheme.compareTo (rhs.m_sDocTypeIDScheme);
    if (ret == 0)
    {
      ret = m_sDocTypeIDValue.compareTo (rhs.m_sDocTypeIDValue);
      if (ret == 0)
      {
        ret = m_sProcessIDScheme.compareTo (rhs.m_sProcessIDScheme);
        if (ret == 0)
        {
          ret = m_sProcessIDValue.compareTo (rhs.m_sProcessIDValue);
          if (ret == 0)
          {
            ret = m_sEndUserCC.compareTo (rhs.m_sEndUserCC);
          }
        }
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
    final SubsetKeyDT_PR_EUC rhs = (SubsetKeyDT_PR_EUC) o;
    return m_sDocTypeIDScheme.equals (rhs.m_sDocTypeIDScheme) &&
           m_sDocTypeIDValue.equals (rhs.m_sDocTypeIDValue) &&
           m_sProcessIDScheme.equals (rhs.m_sProcessIDScheme) &&
           m_sProcessIDValue.equals (rhs.m_sProcessIDValue) &&
           m_sEndUserCC.equals (rhs.m_sEndUserCC);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_sDocTypeIDScheme)
                                       .append (m_sDocTypeIDValue)
                                       .append (m_sProcessIDScheme)
                                       .append (m_sProcessIDValue)
                                       .append (m_sEndUserCC)
                                       .getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("DocTypeIDScheme", m_sDocTypeIDScheme)
                                       .append ("DocTypeIDValue", m_sDocTypeIDValue)
                                       .append ("ProcessIDScheme", m_sProcessIDScheme)
                                       .append ("ProcessIDValue", m_sProcessIDValue)
                                       .append ("EndUserCC", m_sEndUserCC)
                                       .getToString ();
  }
}
