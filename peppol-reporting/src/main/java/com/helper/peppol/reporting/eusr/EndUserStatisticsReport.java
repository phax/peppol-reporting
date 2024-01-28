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
package com.helper.peppol.reporting.eusr;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.builder.IBuilder;
import com.helger.commons.datetime.OffsetDate;
import com.helger.commons.datetime.XMLOffsetDate;
import com.helger.commons.log.ConditionalLogger;
import com.helger.commons.string.StringHelper;
import com.helger.peppol.reporting.jaxb.eusr.v110.EndUserStatisticsReportType;
import com.helger.peppol.reporting.jaxb.eusr.v110.HeaderType;
import com.helger.peppol.reporting.jaxb.eusr.v110.ReportPeriodType;
import com.helper.peppol.reporting.api.CPeppolReporting;
import com.helper.peppol.reporting.api.PeppolReportingItem;
import com.helper.peppol.reporting.eusr.model.EUSRReportingItemList;

/**
 * Builder for Peppol End User Statistics Report objects.
 *
 * @author Philip Helger
 * @since 1.2.0
 */
@Immutable
public final class EndUserStatisticsReport
{
  private EndUserStatisticsReport ()
  {}

  /**
   * @return A new builder for EUSR 1.1 reports and never <code>null</code>.
   */
  @Nonnull
  public static Builder11 builder ()
  {
    return new Builder11 ();
  }

  /**
   * The main builder class for Peppol End User Statistics Reports v1.1.
   *
   * @author Philip Helger
   * @since 1.2.0
   */
  public static class Builder11 implements IBuilder <EndUserStatisticsReportType>
  {
    private static final Logger LOGGER = LoggerFactory.getLogger (EndUserStatisticsReport.Builder11.class);

    private String m_sCustomizationID;
    private String m_sProfileID;
    private LocalDate m_aStartDate;
    private LocalDate m_aEndDate;
    private String m_sReportingServiceProviderIDScheme;
    private String m_sReportingServiceProviderID;
    private Iterable <? extends PeppolReportingItem> m_aReportingItems;

    /**
     * Constructor. Sets default values for: {@link #customizationID(String)},
     * {@link #profileID(String)} and
     * {@link #reportingServiceProviderIDScheme(String)}
     */
    public Builder11 ()
    {
      customizationID (CPeppolReporting.EUSR_CUSTOMIZATION_ID_V11);
      profileID (CPeppolReporting.EUSR_PROFILE_ID_V10);
      reportingServiceProviderIDScheme (CPeppolReporting.SERVICE_PROVIDER_ID_SCHEME);
    }

    /**
     * Set the customization ID to be used. Defaulted in the constructor.
     *
     * @param s
     *        New value. May be <code>null</code>.
     * @return this for chaining
     */
    @Nonnull
    public Builder11 customizationID (@Nullable final String s)
    {
      m_sCustomizationID = s;
      return this;
    }

    /**
     * Set the profile ID to be used. Defaulted in the constructor.
     *
     * @param s
     *        New value. May be <code>null</code>.
     * @return this for chaining
     */
    @Nonnull
    public Builder11 profileID (@Nullable final String s)
    {
      m_sProfileID = s;
      return this;
    }

    /**
     * Set the reporting start date.
     *
     * @param a
     *        New value. May be <code>null</code>.
     * @return this for chaining
     */
    @Nonnull
    public Builder11 startDate (@Nullable final LocalDate a)
    {
      m_aStartDate = a;
      return this;
    }

    /**
     * Set the reporting end date.
     *
     * @param a
     *        New value. May be <code>null</code>.
     * @return this for chaining
     */
    @Nonnull
    public Builder11 endDate (@Nullable final LocalDate a)
    {
      m_aEndDate = a;
      return this;
    }

    /**
     * Set the reporting start and end date.
     *
     * @param a
     *        The date from which the first day of the month and the last day of
     *        the month are taken. May be <code>null</code>.
     * @return this for chaining
     * @see #startDate(LocalDate)
     * @see #endDate(LocalDate)
     */
    @Nonnull
    public Builder11 monthOf (@Nullable final LocalDate a)
    {
      return startDate (a == null ? null : a.withDayOfMonth (1)).endDate (a == null ? null
                                                                                    : a.plusMonths (1)
                                                                                       .withDayOfMonth (1)
                                                                                       .minusDays (1));
    }

    /**
     * Set the reporting start and end date.
     *
     * @param a
     *        The date from which the first day of the month and the last day of
     *        the month are taken. May be <code>null</code>.
     * @return this for chaining
     * @see #startDate(LocalDate)
     * @see #endDate(LocalDate)
     */
    @Nonnull
    public Builder11 monthOf (@Nullable final OffsetDate a)
    {
      return monthOf (a == null ? null : a.toLocalDate ());
    }

    /**
     * Set the reporting start and end date.
     *
     * @param a
     *        The date from which the first day of the month and the last day of
     *        the month are taken. May be <code>null</code>.
     * @return this for chaining
     * @see #startDate(LocalDate)
     * @see #endDate(LocalDate)
     */
    @Nonnull
    public Builder11 monthOf (@Nullable final OffsetDateTime a)
    {
      return monthOf (a == null ? null : a.toLocalDate ());
    }

    /**
     * Set the reporting start and end date.
     *
     * @param a
     *        The date from which the first day of the month and the last day of
     *        the month are taken. May be <code>null</code>.
     * @return this for chaining
     * @see #startDate(LocalDate)
     * @see #endDate(LocalDate)
     */
    @Nonnull
    public Builder11 monthOf (@Nullable final XMLOffsetDate a)
    {
      return monthOf (a == null ? null : a.toLocalDate ());
    }

    /**
     * Set the reporting start and end date.
     *
     * @param a
     *        The date from which the first day of the month and the last day of
     *        the month are taken. May be <code>null</code>.
     * @return this for chaining
     * @see #startDate(LocalDate)
     * @see #endDate(LocalDate)
     */
    @Nonnull
    public Builder11 monthOf (@Nullable final YearMonth a)
    {
      return startDate (a == null ? null : a.atDay (1)).endDate (a == null ? null
                                                                           : a.plusMonths (1).atDay (1).minusDays (1));
    }

    /**
     * Set the reporting Service Provider ID scheme to be used. Defaulted in the
     * constructor.
     *
     * @param s
     *        New value. May be <code>null</code>.
     * @return this for chaining
     */
    @Nonnull
    public Builder11 reportingServiceProviderIDScheme (@Nullable final String s)
    {
      m_sReportingServiceProviderIDScheme = s;
      return this;
    }

    /**
     * Set the reporting Service Provider ID to be used. Usually has the layout
     * <code>P[A-Z]{2}[0-9]{6}</code>.
     *
     * @param s
     *        New value. May be <code>null</code>.
     * @return this for chaining
     */
    @Nonnull
    public Builder11 reportingServiceProviderID (@Nullable final String s)
    {
      m_sReportingServiceProviderID = s;
      return this;
    }

    /**
     * Set the EUSR reporting items based on which the report is to be created.
     *
     * @param aItems
     *        The items of which a new {@link EUSRReportingItemList} is created
     *        and used. May be <code>null</code>.
     * @return this for chaining
     */
    @Nonnull
    public Builder11 reportingItemList (@Nullable final Iterable <? extends PeppolReportingItem> aItems)
    {
      m_aReportingItems = aItems;
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
      if (StringHelper.hasNoText (m_sCustomizationID))
      {
        aCondLogger.warn ("CustomizationID is missing");
        return false;
      }

      if (StringHelper.hasNoText (m_sProfileID))
      {
        aCondLogger.warn ("ProfileID is missing");
        return false;
      }

      if (m_aStartDate == null)
      {
        aCondLogger.warn ("StartDate is missing");
        return false;
      }

      if (m_aEndDate == null)
      {
        aCondLogger.warn ("EndDate is missing");
        return false;
      }

      if (m_aEndDate.isBefore (m_aStartDate))
      {
        aCondLogger.warn ("StartDate must be before or equal to the EndDate");
        return false;
      }

      if (StringHelper.hasNoText (m_sReportingServiceProviderIDScheme))
      {
        aCondLogger.warn ("Reporting Service Provider ID Scheme is missing");
        return false;
      }

      if (StringHelper.hasNoText (m_sReportingServiceProviderID))
      {
        aCondLogger.warn ("Reporting Service Provider ID is missing");
        return false;
      }

      if (m_aReportingItems == null)
      {
        aCondLogger.warn ("Reporting Item list is missing");
        return false;
      }

      aCondLogger.trace ( () -> "Builder fields are complete");
      return true;
    }

    /**
     * Build the main EUSR report. Use {@link #isComplete(boolean)} to check if
     * all mandatory fields are set or not.
     *
     * @see #isComplete(boolean)
     */
    @Nonnull
    public EndUserStatisticsReportType build ()
    {
      if (!isComplete (true))
        throw new IllegalStateException ("The EUSR builder was not filled completely");

      final EndUserStatisticsReportType aReport = new EndUserStatisticsReportType ();
      aReport.setCustomizationID (m_sCustomizationID);
      aReport.setProfileID (m_sProfileID);

      {
        final HeaderType aHeader = new HeaderType ();
        final ReportPeriodType aPeriod = new ReportPeriodType ();
        aPeriod.setStartDate (XMLOffsetDate.of (m_aStartDate));
        aPeriod.setEndDate (XMLOffsetDate.of (m_aEndDate));
        aHeader.setReportPeriod (aPeriod);
        aHeader.setReporterID (m_sReportingServiceProviderID).setSchemeID (m_sReportingServiceProviderIDScheme);
        aReport.setHeader (aHeader);
      }

      // Add the Full set and all Subsets
      EUSRReportingItemList.fillReportSubsets (m_aReportingItems, aReport);
      return aReport;
    }
  }
}
