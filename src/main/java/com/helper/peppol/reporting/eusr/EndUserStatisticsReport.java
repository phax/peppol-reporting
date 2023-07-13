package com.helper.peppol.reporting.eusr;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.builder.IBuilder;
import com.helger.commons.datetime.OffsetDate;
import com.helger.commons.datetime.XMLOffsetDate;
import com.helger.commons.math.MathHelper;
import com.helger.commons.string.StringHelper;
import com.helger.peppol.reporting.jaxb.eusr.v110.EndUserStatisticsReportType;
import com.helger.peppol.reporting.jaxb.eusr.v110.FullSetType;
import com.helger.peppol.reporting.jaxb.eusr.v110.HeaderType;
import com.helger.peppol.reporting.jaxb.eusr.v110.ReportPeriodType;
import com.helper.peppol.reporting.eusr.model.EUSRReportingItemList;
import com.helper.peppol.reporting.model.PeppolReportingItem;

@Immutable
public final class EndUserStatisticsReport
{
  public static final String DEFAULT_REPORTER_ID_SCHEME = "CertSubjectCN";

  private EndUserStatisticsReport ()
  {}

  @Nonnull
  public static Builder builder ()
  {
    return new Builder ();
  }

  public static final class Builder implements IBuilder <EndUserStatisticsReportType>
  {
    private String m_sCustomizationID;
    private String m_sProfileID;
    private LocalDate m_aStartDate;
    private LocalDate m_aEndDate;
    private String m_sReportingServiceProviderIDScheme;
    private String m_sReportingServiceProviderID;
    private EUSRReportingItemList m_aList;

    public Builder ()
    {
      customizationID (CEUSR.CUSTOMIZATION_ID_V11);
      profileID (CEUSR.PROFILE_ID_V10);
      reportingServiceProviderIDScheme (DEFAULT_REPORTER_ID_SCHEME);
    }

    @Nonnull
    public Builder customizationID (@Nullable final String s)
    {
      m_sCustomizationID = s;
      return this;
    }

    @Nonnull
    public Builder profileID (@Nullable final String s)
    {
      m_sProfileID = s;
      return this;
    }

    @Nonnull
    public Builder startDate (@Nullable final LocalDate a)
    {
      m_aStartDate = a;
      return this;
    }

    @Nonnull
    public Builder endDate (@Nullable final LocalDate a)
    {
      m_aEndDate = a;
      return this;
    }

    @Nonnull
    public Builder monthOf (@Nullable final LocalDate a)
    {
      return startDate (a == null ? null : a.withDayOfMonth (1)).endDate (a == null ? null : a.plusMonths (1)
                                                                                              .withDayOfMonth (1)
                                                                                              .minusDays (1));
    }

    @Nonnull
    public Builder monthOf (@Nullable final OffsetDate a)
    {
      return monthOf (a == null ? null : a.toLocalDate ());
    }

    @Nonnull
    public Builder monthOf (@Nullable final OffsetDateTime a)
    {
      return monthOf (a == null ? null : a.toLocalDate ());
    }

    @Nonnull
    public Builder monthOf (@Nullable final XMLOffsetDate a)
    {
      return monthOf (a == null ? null : a.toLocalDate ());
    }

    @Nonnull
    public Builder reportingServiceProviderIDScheme (@Nullable final String s)
    {
      m_sReportingServiceProviderIDScheme = s;
      return this;
    }

    @Nonnull
    public Builder reportingServiceProviderID (@Nullable final String s)
    {
      m_sReportingServiceProviderID = s;
      return this;
    }

    @Nonnull
    public Builder reportingItemList (@Nullable final EUSRReportingItemList a)
    {
      m_aList = a;
      return this;
    }

    @Nonnull
    public Builder reportingItemList (@Nullable final PeppolReportingItem... aItems)
    {
      return reportingItemList (aItems == null ? null : new EUSRReportingItemList (aItems));
    }

    @Nonnull
    public Builder reportingItemList (@Nullable final Iterable <? extends PeppolReportingItem> aItems)
    {
      return reportingItemList (aItems == null ? null : new EUSRReportingItemList (aItems));
    }

    public boolean isComplete ()
    {
      if (StringHelper.hasNoText (m_sCustomizationID))
        return false;

      if (StringHelper.hasNoText (m_sProfileID))
        return false;

      if (m_aStartDate == null)
        return false;

      if (m_aEndDate == null)
        return false;

      if (StringHelper.hasNoText (m_sReportingServiceProviderIDScheme))
        return false;

      if (StringHelper.hasNoText (m_sReportingServiceProviderID))
        return false;

      if (m_aList == null)
        return false;

      return true;
    }

    @Nonnull
    public EndUserStatisticsReportType build ()
    {
      if (!isComplete ())
        throw new IllegalStateException ("The builder was not filled completely");

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

      {
        final FullSetType aFullSet = new FullSetType ();
        aFullSet.setSendingEndUsers (MathHelper.toBigInteger (m_aList.getSendingEndUserCount ()));
        aFullSet.setReceivingEndUsers (MathHelper.toBigInteger (m_aList.getReceivingEndUserCount ()));
        aFullSet.setSendingOrReceivingEndUsers (MathHelper.toBigInteger (m_aList.getSendingOrReceivingEndUserCount ()));
        aReport.setFullSet (aFullSet);
      }

      // Add all subsets
      m_aList.fillReportSubsets (aReport);
      return aReport;
    }
  }
}
