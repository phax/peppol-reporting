/*
 * Copyright (C) 2022-2023 Philip Helger
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
package com.helper.peppol.reporting.model;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.builder.IBuilder;
import com.helger.commons.log.ConditionalLogger;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.ToStringGenerator;
import com.helger.peppol.smp.ESMPTransportProfile;
import com.helger.peppol.smp.ISMPTransportProfile;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IProcessIdentifier;

/**
 * This class represents a single Peppol Reporting raw data item. It contains
 * the raw data for Peppol TSR and EUSR together.
 *
 * @author Philip Helger
 */
@Immutable
public final class PeppolReportingItem
{
  // TSR, EUSR
  private final OffsetDateTime m_aExchangeDTUTC;
  // TSR, EUSR
  private final EReportingDirection m_eDirection;
  // TSR
  private final String m_sC2ID;
  // TSR
  private final String m_sC3ID;
  // TSR, EUSR
  private final String m_sDocTypeIDScheme;
  private final String m_sDocTypeIDValue;
  // TSR, EUSR
  private final String m_sProcessIDScheme;
  private final String m_sProcessIDValue;
  // TSR
  private final String m_sTransportProtocol;
  // TSR, EUSR
  private final String m_sC1CountryCode;
  // TSR, EUSR
  private final String m_sC4CountryCode;
  // EUSR
  private final String m_sEndUserID;

  public PeppolReportingItem (@Nonnull final OffsetDateTime aExchangeDT,
                              @Nonnull final EReportingDirection eDirection,
                              @Nonnull @Nonempty final String sC2ID,
                              @Nonnull @Nonempty final String sC3ID,
                              @Nonnull @Nonempty final String sDocTypeIDScheme,
                              @Nonnull @Nonempty final String sDocTypeIDValue,
                              @Nonnull @Nonempty final String sProcessIDScheme,
                              @Nonnull @Nonempty final String sProcessIDValue,
                              @Nonnull @Nonempty final String sTransportProtocol,
                              @Nonnull @Nonempty final String sC1CC,
                              @Nullable final String sC4CC,
                              @Nonnull @Nonempty final String sEndUserID)
  {
    ValueEnforcer.notNull (aExchangeDT, "ExchangeDT");
    ValueEnforcer.notNull (eDirection, "Direction");
    ValueEnforcer.notEmpty (sC2ID, "C2ID");
    ValueEnforcer.notEmpty (sC3ID, "C3ID");
    ValueEnforcer.notEmpty (sDocTypeIDScheme, "DocTypeIDScheme");
    ValueEnforcer.notEmpty (sDocTypeIDValue, "DocTypeIDValue");
    ValueEnforcer.notEmpty (sProcessIDScheme, "ProcessIDScheme");
    ValueEnforcer.notEmpty (sProcessIDValue, "ProcessIDValue");
    ValueEnforcer.notEmpty (sTransportProtocol, "TransportProtocol");
    ValueEnforcer.notEmpty (sC1CC, "C1CountryCode");
    if (eDirection.isReceiving ())
      ValueEnforcer.notEmpty (sC4CC, "C4CountryCode");
    ValueEnforcer.notEmpty (sEndUserID, "EndUserID");

    // Make sure it is UTC
    m_aExchangeDTUTC = aExchangeDT.atZoneSameInstant (ZoneOffset.UTC).toOffsetDateTime ();
    m_eDirection = eDirection;
    m_sC2ID = sC2ID;
    m_sC3ID = sC3ID;
    m_sDocTypeIDScheme = sDocTypeIDScheme;
    m_sDocTypeIDValue = sDocTypeIDValue;
    m_sProcessIDScheme = sProcessIDScheme;
    m_sProcessIDValue = sProcessIDValue;
    m_sTransportProtocol = sTransportProtocol;
    m_sC1CountryCode = sC1CC;
    m_sC4CountryCode = sC4CC;
    m_sEndUserID = sEndUserID;
  }

  /**
   * @return The exchange date time in UTC.
   */
  @Nonnull
  public OffsetDateTime getExchangeDTUTC ()
  {
    return m_aExchangeDTUTC;
  }

  /**
   * @return <code>true</code> if this reporting item is for sending.
   *         <code>false</code> if it is for receiving.
   * @see #isReceiving()
   */
  public boolean isSending ()
  {
    return m_eDirection.isSending ();
  }

  /**
   * @return <code>true</code> if this reporting item is for receiving.
   *         <code>false</code> if it is for sending.
   * @see #isSending()
   */
  public boolean isReceiving ()
  {
    return m_eDirection.isReceiving ();
  }

  /**
   * @return The sending Service Provider (C2) ID. Neither <code>null</code> nor
   *         empty.
   */
  @Nonnull
  @Nonempty
  public String getC2ID ()
  {
    return m_sC2ID;
  }

  /**
   * @return The receiving Service Provider (C2) ID. Neither <code>null</code>
   *         nor empty.
   */
  @Nonnull
  @Nonempty
  public String getC3ID ()
  {
    return m_sC3ID;
  }

  /**
   * @return The ID of the other service provider. So if this item is for a sent
   *         message, this is the C3-ID otherwise it is the C2 ID. Neither
   *         <code>null</code> nor empty.
   * @see #isSending()
   * @see #isReceiving()
   * @see #getC2ID()
   * @see #getC3ID()
   */
  @Nonnull
  @Nonempty
  public String getOtherServiceProviderID ()
  {
    return m_eDirection.isSending () ? m_sC3ID : m_sC2ID;
  }

  /**
   * @return The document type identifier scheme used. Neither <code>null</code>
   *         nor empty.
   * @see #getDocTypeIDValue()
   */
  @Nonnull
  @Nonempty
  public String getDocTypeIDScheme ()
  {
    return m_sDocTypeIDScheme;
  }

  /**
   * @return The document type identifier value used. Neither <code>null</code>
   *         nor empty.
   * @see #getDocTypeIDScheme()
   */
  @Nonnull
  @Nonempty
  public String getDocTypeIDValue ()
  {
    return m_sDocTypeIDValue;
  }

  /**
   * @return The process identifier scheme used. Neither <code>null</code> nor
   *         empty.
   * @see #getProcessIDValue()
   */
  @Nonnull
  @Nonempty
  public String getProcessIDScheme ()
  {
    return m_sProcessIDScheme;
  }

  /**
   * @return The process identifier scheme value. Neither <code>null</code> nor
   *         empty.
   * @see #getProcessIDScheme()
   */
  @Nonnull
  @Nonempty
  public String getProcessIDValue ()
  {
    return m_sProcessIDValue;
  }

  /**
   * @return The country code of C1. Neither <code>null</code> nor empty. C2
   *         knows it via KYC, C3 knows it, because it is transferred in the
   *         SBDH (since v2.0.0).
   */
  @Nonnull
  @Nonempty
  public String getC1CountryCode ()
  {
    return m_sC1CountryCode;
  }

  /**
   * @return The country code of C4. May be <code>null</code> for sent messages,
   *         because C1/C2 cannot necessarily know the country code of C4 in all
   *         cases.
   */
  @Nullable
  public String getC4CountryCode ()
  {
    return m_sC4CountryCode;
  }

  /**
   * @return The transport protocol used. Neither <code>null</code> nor empty.
   */
  @Nonnull
  @Nonempty
  public String getTransportProtocol ()
  {
    return m_sTransportProtocol;
  }

  /**
   * @return The End User Country for EUSR. It's either C1 country code or C4
   *         country code, depending on the message direction.
   * @see #isSending()
   * @see #isReceiving()
   * @see #getC1CountryCode()
   * @see #getC4CountryCode()
   */
  @Nonnull
  @Nonempty
  public String getEndUserCountryCode ()
  {
    return m_eDirection.isSending () ? m_sC1CountryCode : m_sC4CountryCode;
  }

  /**
   * @return The end user ID. The exact layout is implementation specific and
   *         varies from Service Provider to Service Provider.
   */
  @Nonnull
  @Nonempty
  public String getEndUserID ()
  {
    return m_sEndUserID;
  }

  @Override
  public String toString ()
  {
    return new ToStringGenerator (null).append ("ExchangeDTUTC", m_aExchangeDTUTC)
                                       .append ("Direction", m_eDirection)
                                       .append ("C2ID", m_sC2ID)
                                       .append ("C3ID", m_sC3ID)
                                       .append ("DocTypeIDScheme", m_sDocTypeIDScheme)
                                       .append ("DocTypeIDValue", m_sDocTypeIDValue)
                                       .append ("ProcessIDScheme", m_sProcessIDScheme)
                                       .append ("ProcessIDValue", m_sProcessIDValue)
                                       .append ("TransportProtocol", m_sTransportProtocol)
                                       .append ("C1CountryCode", m_sC1CountryCode)
                                       .append ("C4CountryCode", m_sC4CountryCode)
                                       .append ("EndUserID", m_sEndUserID)
                                       .getToString ();
  }

  @Nonnull
  public static Builder builder ()
  {
    return new Builder ();
  }

  /**
   * Builder class for {@link PeppolReportingItem} objects.
   *
   * @author Philip Helger
   */
  public static class Builder implements IBuilder <PeppolReportingItem>
  {
    private static final Logger LOGGER = LoggerFactory.getLogger (PeppolReportingItem.Builder.class);

    private OffsetDateTime m_aExchangeDT;
    private EReportingDirection m_eDirection;
    private String m_sC2ID;
    private String m_sC3ID;
    private String m_sDocTypeIDScheme;
    private String m_sDocTypeIDValue;
    private String m_sProcessIDScheme;
    private String m_sProcessIDValue;
    private String m_sTransportProtocol;
    private String m_sC1CountryCode;
    private String m_sC4CountryCode;
    private String m_sEndUserID;

    public Builder ()
    {}

    @Nonnull
    public Builder exchangeDateTime (@Nullable final OffsetDateTime a)
    {
      m_aExchangeDT = a;
      return this;
    }

    @Nonnull
    public Builder exchangeDateTime (@Nullable final ZonedDateTime a)
    {
      return exchangeDateTime (a == null ? null : a.toOffsetDateTime ());
    }

    @Nonnull
    public Builder exchangeDateTimeInUTC (@Nullable final LocalDateTime a)
    {
      return exchangeDateTime (a == null ? null : a.atOffset (ZoneOffset.UTC));
    }

    @Nonnull
    public Builder direction (@Nullable final EReportingDirection e)
    {
      m_eDirection = e;
      return this;
    }

    @Nonnull
    public Builder directionSending ()
    {
      return direction (EReportingDirection.SENDING);
    }

    @Nonnull
    public Builder directionReceiving ()
    {
      return direction (EReportingDirection.RECEIVING);
    }

    @Nonnull
    public Builder c2ID (@Nullable final String s)
    {
      m_sC2ID = s;
      return this;
    }

    @Nonnull
    public Builder c3ID (@Nullable final String s)
    {
      m_sC3ID = s;
      return this;
    }

    @Nonnull
    public Builder docTypeIDScheme (@Nullable final String s)
    {
      m_sDocTypeIDScheme = s;
      return this;
    }

    @Nonnull
    public Builder docTypeIDValue (@Nullable final String s)
    {
      m_sDocTypeIDValue = s;
      return this;
    }

    @Nonnull
    public Builder docTypeID (@Nullable final IDocumentTypeIdentifier a)
    {
      return docTypeIDScheme (a == null ? null : a.getScheme ()).docTypeIDValue (a == null ? null : a.getValue ());
    }

    @Nonnull
    public Builder processIDScheme (@Nullable final String s)
    {
      m_sProcessIDScheme = s;
      return this;
    }

    @Nonnull
    public Builder processIDValue (@Nullable final String s)
    {
      m_sProcessIDValue = s;
      return this;
    }

    @Nonnull
    public Builder processID (@Nullable final IProcessIdentifier a)
    {
      return processIDScheme (a == null ? null : a.getScheme ()).processIDValue (a == null ? null : a.getValue ());
    }

    @Nonnull
    public Builder transportProtocol (@Nullable final String s)
    {
      m_sTransportProtocol = s;
      return this;
    }

    @Nonnull
    public Builder transportProtocol (@Nullable final ISMPTransportProfile a)
    {
      return transportProtocol (a == null ? null : a.getID ());
    }

    @Nonnull
    public Builder transportProtocolPeppolAS4v2 ()
    {
      return transportProtocol (ESMPTransportProfile.TRANSPORT_PROFILE_PEPPOL_AS4_V2);
    }

    @Nonnull
    public Builder c1CountryCode (@Nullable final String s)
    {
      m_sC1CountryCode = s;
      return this;
    }

    @Nonnull
    public Builder c4CountryCode (@Nullable final String s)
    {
      m_sC4CountryCode = s;
      return this;
    }

    @Nonnull
    public Builder endUserID (@Nullable final String s)
    {
      m_sEndUserID = s;
      return this;
    }

    /**
     * Check if all mandatory fields are set or not.
     *
     * @param bLogFailures
     *        <code>true</code> if missing fields should be logged,
     *        <code>false</code> if not.
     * @return <code>true</code> if all mandatory fields are set,
     *         <code>false</code> if not.
     */
    public boolean isComplete (final boolean bLogFailures)
    {
      final ConditionalLogger aCondLogger = new ConditionalLogger (LOGGER, bLogFailures);

      if (m_aExchangeDT == null)
      {
        aCondLogger.warn ("ExchangeDateTime is missing");
        return false;
      }

      if (m_eDirection == null)
      {
        aCondLogger.warn ("Direction is missing");
        return false;
      }

      if (StringHelper.hasNoText (m_sC2ID))
      {
        aCondLogger.warn ("C2 ID is missing");
        return false;
      }

      if (StringHelper.hasNoText (m_sC3ID))
      {
        aCondLogger.warn ("C3 ID is missing");
        return false;
      }

      if (StringHelper.hasNoText (m_sDocTypeIDScheme))
      {
        aCondLogger.warn ("Document Type ID Scheme is missing");
        return false;
      }

      if (StringHelper.hasNoText (m_sDocTypeIDValue))
      {
        aCondLogger.warn ("Document Type ID Value is missing");
        return false;
      }

      if (StringHelper.hasNoText (m_sProcessIDScheme))
      {
        aCondLogger.warn ("Process ID Scheme is missing");
        return false;
      }

      if (StringHelper.hasNoText (m_sProcessIDValue))
      {
        aCondLogger.warn ("Process ID Value is missing");
        return false;
      }

      if (StringHelper.hasNoText (m_sTransportProtocol))
      {
        aCondLogger.warn ("Transport Protocol is missing");
        return false;
      }

      if (StringHelper.hasNoText (m_sC1CountryCode))
      {
        aCondLogger.warn ("C1 Country Code is missing");
        return false;
      }

      // C4 is only mandatory for receivers
      if (StringHelper.hasNoText (m_sC4CountryCode))
      {
        if (m_eDirection.isReceiving ())
        {
          aCondLogger.warn ("C4 Country Code is missing");
          return false;
        }
      }
      else
      {
        if (m_eDirection.isSending ())
        {
          aCondLogger.warn ("C4 Country Code cannot be provided for outgoing/sent messages");
          return false;
        }
      }

      if (StringHelper.hasNoText (m_sEndUserID))
      {
        aCondLogger.warn ("End User ID is missing");
        return false;
      }

      return true;
    }

    @Nonnull
    public PeppolReportingItem build ()
    {
      if (!isComplete (true))
        throw new IllegalStateException ("The builder was not filled completely");

      return new PeppolReportingItem (m_aExchangeDT,
                                      m_eDirection,
                                      m_sC2ID,
                                      m_sC3ID,
                                      m_sDocTypeIDScheme,
                                      m_sDocTypeIDValue,
                                      m_sProcessIDScheme,
                                      m_sProcessIDValue,
                                      m_sTransportProtocol,
                                      m_sC1CountryCode,
                                      m_sC4CountryCode,
                                      m_sEndUserID);
    }
  }
}
