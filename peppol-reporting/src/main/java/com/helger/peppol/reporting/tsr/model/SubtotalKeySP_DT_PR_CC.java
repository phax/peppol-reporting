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
package com.helger.peppol.reporting.tsr.model;

import javax.annotation.Nonnull;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.MustImplementEqualsAndHashcode;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.hashcode.HashCodeGenerator;
import com.helger.commons.string.ToStringGenerator;

/**
 * Single Peppol TSR Key for "Service Provider, Document Type ID, Process ID and
 * Country Codes".
 *
 * @author Philip Helger
 * @since 1.2.0
 */
@MustImplementEqualsAndHashcode
public final class SubtotalKeySP_DT_PR_CC implements ITSRSubtotalKey <SubtotalKeySP_DT_PR_CC>
{
  public static final String TYPE = "PerSP-DT-PR-CC";

  private final String m_sOtherSPID;
  private final String m_sDocTypeIDScheme;
  private final String m_sDocTypeIDValue;
  private final String m_sProcessIDScheme;
  private final String m_sProcessIDValue;
  private final String m_sC1CountryCode;
  private final String m_sC4CountryCode;

  public SubtotalKeySP_DT_PR_CC (@Nonnull @Nonempty final String sOtherSPID,
                                 @Nonnull @Nonempty final String sDocTypeIDScheme,
                                 @Nonnull @Nonempty final String sDocTypeIDValue,
                                 @Nonnull @Nonempty final String sProcessIDScheme,
                                 @Nonnull @Nonempty final String sProcessIDValue,
                                 @Nonnull @Nonempty final String sC1CountryCode,
                                 @Nonnull @Nonempty final String sC4CountryCode)
  {
    ValueEnforcer.notEmpty (sOtherSPID, "OtherSPID");
    ValueEnforcer.notEmpty (sDocTypeIDScheme, "DocTypeIDScheme");
    ValueEnforcer.notEmpty (sDocTypeIDValue, "DocTypeIDValue");
    ValueEnforcer.notEmpty (sProcessIDScheme, "ProcessIDScheme");
    ValueEnforcer.notEmpty (sProcessIDValue, "ProcessIDValue");
    ValueEnforcer.notEmpty (sC1CountryCode, "C1CountryCode");
    ValueEnforcer.notEmpty (sC4CountryCode, "C4CountryCode");
    m_sOtherSPID = sOtherSPID;
    m_sDocTypeIDScheme = sDocTypeIDScheme;
    m_sDocTypeIDValue = sDocTypeIDValue;
    m_sProcessIDScheme = sProcessIDScheme;
    m_sProcessIDValue = sProcessIDValue;
    m_sC1CountryCode = sC1CountryCode;
    m_sC4CountryCode = sC4CountryCode;
  }

  @Nonnull
  @Nonempty
  public String getServiceProviderID ()
  {
    return m_sOtherSPID;
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
  public String getC1CountryCode ()
  {
    return m_sC1CountryCode;
  }

  @Nonnull
  @Nonempty
  public String getC4CountryCode ()
  {
    return m_sC4CountryCode;
  }

  public int compareTo (@Nonnull final SubtotalKeySP_DT_PR_CC rhs)
  {
    int ret = m_sOtherSPID.compareTo (rhs.m_sOtherSPID);
    if (ret == 0)
    {
      ret = m_sDocTypeIDScheme.compareTo (rhs.m_sDocTypeIDScheme);
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
              ret = m_sC1CountryCode.compareTo (rhs.m_sC1CountryCode);
              if (ret == 0)
              {
                ret = m_sC4CountryCode.compareTo (rhs.m_sC4CountryCode);
              }
            }
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
    final SubtotalKeySP_DT_PR_CC rhs = (SubtotalKeySP_DT_PR_CC) o;
    return m_sOtherSPID.equals (rhs.m_sOtherSPID) &&
           m_sDocTypeIDScheme.equals (rhs.m_sDocTypeIDScheme) &&
           m_sDocTypeIDValue.equals (rhs.m_sDocTypeIDValue) &&
           m_sProcessIDScheme.equals (rhs.m_sProcessIDScheme) &&
           m_sProcessIDValue.equals (rhs.m_sProcessIDValue) &&
           m_sC1CountryCode.equals (rhs.m_sC1CountryCode) &&
           m_sC4CountryCode.equals (rhs.m_sC4CountryCode);
  }

  @Override
  public int hashCode ()
  {
    return new HashCodeGenerator (this).append (m_sOtherSPID)
                                       .append (m_sDocTypeIDScheme)
                                       .append (m_sDocTypeIDValue)
                                       .append (m_sProcessIDScheme)
                                       .append (m_sProcessIDValue)
                                       .append (m_sC1CountryCode)
                                       .append (m_sC4CountryCode)
                                       .getHashCode ();
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("OtherSPID", m_sOtherSPID)
                                       .append ("DocTypeIDScheme", m_sDocTypeIDScheme)
                                       .append ("DocTypeIDValue", m_sDocTypeIDValue)
                                       .append ("ProcessIDScheme", m_sProcessIDScheme)
                                       .append ("ProcessIDValue", m_sProcessIDValue)
                                       .append ("C1CountryCode", m_sC1CountryCode)
                                       .append ("C4CountryCode", m_sC4CountryCode)
                                       .getToString ();
  }
}
