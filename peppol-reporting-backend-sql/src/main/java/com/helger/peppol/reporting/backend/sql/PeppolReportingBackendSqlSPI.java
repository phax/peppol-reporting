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
package com.helger.peppol.reporting.backend.sql;

import java.io.IOException;
import java.time.LocalDate;
import java.util.EnumSet;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.IsSPIImplementation;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.OverrideOnDemand;
import com.helger.commons.annotation.UsedViaReflection;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.concurrent.SimpleReadWriteLock;
import com.helger.commons.state.ESuccess;
import com.helger.commons.string.StringHelper;
import com.helger.config.IConfig;
import com.helger.db.api.EDatabaseSystemType;
import com.helger.db.api.config.IJdbcConfiguration;
import com.helger.db.api.flyway.FlywayConfiguration;
import com.helger.db.api.helper.DBValueHelper;
import com.helger.db.jdbc.DataSourceProviderFromJdbcConfiguration;
import com.helger.db.jdbc.callback.ConstantPreparedStatementDataProvider;
import com.helger.db.jdbc.executor.DBExecutor;
import com.helger.db.jdbc.executor.DBResultRow;
import com.helger.peppol.reporting.api.EReportingDirection;
import com.helger.peppol.reporting.api.PeppolReportingHelper;
import com.helger.peppol.reporting.api.PeppolReportingItem;
import com.helger.peppol.reporting.api.backend.IPeppolReportingBackendSPI;
import com.helger.peppol.reporting.api.backend.PeppolReportingBackendException;
import com.helger.peppolid.CIdentifier;

/**
 * SPI implementation of {@link IPeppolReportingBackendSPI} for SQL. This backend supports the lazy
 * gathering of report items through an Iterator.
 *
 * @author Philip Helger
 */
@IsSPIImplementation
public class PeppolReportingBackendSqlSPI implements IPeppolReportingBackendSPI
{
  private static final Logger LOGGER = LoggerFactory.getLogger (PeppolReportingBackendSqlSPI.class);

  private final SimpleReadWriteLock m_aRWLock = new SimpleReadWriteLock ();
  @GuardedBy ("m_aRWLock")
  private IConfig m_aConfig;
  private ReportingJdbcConfiguration m_aJdbcConfig;
  @GuardedBy ("m_aRWLock")
  private DataSourceProviderFromJdbcConfiguration m_aDSP;
  private String m_sTableNamePrefix;

  @UsedViaReflection
  public PeppolReportingBackendSqlSPI ()
  {}

  @Nonnull
  @Nonempty
  public String getDisplayName ()
  {
    return "SQL";
  }

  @Nonnull
  public static String getTableNamePrefix (@Nonnull final String sJdbcSchema)
  {
    final String sSchemaName = StringHelper.trim (sJdbcSchema);
    if (StringHelper.hasText (sSchemaName))
    {
      // Quotes are required for PostgreSQL when schema contains a dash
      return "\"" + sSchemaName + "\".";
    }
    // May not be null
    return "";
  }

  @Nullable
  @OverrideOnDemand
  protected DataSourceProviderFromJdbcConfiguration createReportingDataSourceProvider (@Nonnull final IJdbcConfiguration aJdbcConfig)
  {
    return new DataSourceProviderFromJdbcConfiguration (aJdbcConfig);
  }

  @Nonnull
  public ESuccess initBackend (@Nonnull final IConfig aConfig)
  {
    m_aRWLock.writeLocked ( () -> {
      if (m_aDSP != null)
        throw new IllegalStateException ("The Peppol Reporting SQL DB backend was already initialized");

      // Init JDBC configuration
      final ReportingJdbcConfiguration aJdbcConfig = new ReportingJdbcConfiguration (aConfig);

      // Resolve database type
      final EDatabaseSystemType eDBType = aJdbcConfig.getJdbcDatabaseSystemType ();
      final EnumSet <EDatabaseSystemType> aAllowedDBTypes = EnumSet.of (EDatabaseSystemType.MYSQL,
                                                                        EDatabaseSystemType.POSTGRESQL);
      if (eDBType == null || !aAllowedDBTypes.contains (eDBType))
        throw new IllegalStateException ("The database type MUST be provided and MUST be one of " +
                                         StringHelper.imploder ()
                                                     .source (aAllowedDBTypes, EDatabaseSystemType::getID)
                                                     .separator (", ")
                                                     .build () +
                                         " - provided value is '" +
                                         aJdbcConfig.getJdbcDatabaseType () +
                                         "'");

      // Build Flyway configuration
      final ReportingFlywayConfigurationBuilder aBuilder = new ReportingFlywayConfigurationBuilder (aConfig,
                                                                                                    aJdbcConfig);
      final FlywayConfiguration aFlywayConfig = aBuilder.build ();

      // Run Flyway
      if (aFlywayConfig.isFlywayEnabled ())
        ReportingFlywayMigrator.Singleton.INSTANCE.runFlyway (eDBType, aJdbcConfig, aFlywayConfig);
      else
        LOGGER.warn ("Peppol Reporting Flyway Migration is disabled according to the configuration key '" +
                     aBuilder.getConfigKeyEnabled () +
                     "'");

      // Remember stuff
      m_aConfig = aConfig;
      m_aJdbcConfig = aJdbcConfig;
      m_aDSP = createReportingDataSourceProvider (aJdbcConfig);
      if (m_aDSP == null)
        throw new IllegalStateException ("Failed to create Peppol Reporting SQL DB DataSource provider");
      m_sTableNamePrefix = getTableNamePrefix (aJdbcConfig.getJdbcSchema ());
    });

    if (!isInitialized ())
    {
      // Error was already logged
      return ESuccess.FAILURE;
    }

    return ESuccess.SUCCESS;
  }

  public boolean isInitialized ()
  {
    return m_aRWLock.readLockedBoolean ( () -> m_aConfig != null && m_aDSP != null && m_sTableNamePrefix != null);
  }

  public void shutdownBackend ()
  {
    if (isInitialized ())
    {
      m_aRWLock.writeLocked ( () -> {
        LOGGER.info ("Shutting down Peppol Reporting SQL DB client");
        m_aConfig = null;
        if (m_aDSP != null)
          try
          {
            m_aDSP.close ();
          }
          catch (final IOException ex)
          {
            LOGGER.error ("Failed to close Peppol Reporting DataSource provider", ex);
          }
        m_aDSP = null;
      });
    }
    else
      LOGGER.warn ("The Peppol Reporting SQL DB backend cannot be shutdown, because it was never properly initialized");
  }

  @Nonnull
  private DBExecutor _newExecutor ()
  {
    return new ReportingDBExecutor (m_aDSP, m_aJdbcConfig);
  }

  public void storeReportingItem (@Nonnull final PeppolReportingItem aReportingItem) throws PeppolReportingBackendException
  {
    ValueEnforcer.notNull (aReportingItem, "ReportingItem");

    if (PeppolReportingHelper.isDocumentTypeEligableForReporting (aReportingItem.getDocTypeIDScheme (),
                                                                  aReportingItem.getDocTypeIDValue ()))
    {
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Trying to store Peppol Reporting Item in SQL DB");

      if (!isInitialized ())
        throw new IllegalStateException ("The Peppol Reporting SQL DB backend is not initialized");

      final DBExecutor aExecutor = _newExecutor ();
      final ESuccess eSuccess = aExecutor.performInTransaction ( () -> {
        // Create new
        final long nCreated = aExecutor.insertOrUpdateOrDelete ("INSERT INTO " +
                                                                m_sTableNamePrefix +
                                                                "peppol_reporting_item (exchangedt, sending, c2id, c3id, dtscheme, dtvalue, procscheme, procvalue, tp, c1cc, c4cc, enduserid)" +
                                                                " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                                                                new ConstantPreparedStatementDataProvider (DBValueHelper.toTimestamp (aReportingItem.getExchangeDTUTC ()
                                                                                                                                                    .toLocalDateTime ()),
                                                                                                           Boolean.valueOf (aReportingItem.isSending ()),
                                                                                                           DBValueHelper.getTrimmedToLength (aReportingItem.getC2ID (),
                                                                                                                                             PeppolReportingItem.MAX_LEN_C2_ID),
                                                                                                           DBValueHelper.getTrimmedToLength (aReportingItem.getC3ID (),
                                                                                                                                             PeppolReportingItem.MAX_LEN_C3_ID),
                                                                                                           DBValueHelper.getTrimmedToLength (aReportingItem.getDocTypeIDScheme (),
                                                                                                                                             PeppolReportingItem.MAX_LEN_DOCTYPE_SCHEME),
                                                                                                           DBValueHelper.getTrimmedToLength (aReportingItem.getDocTypeIDValue (),
                                                                                                                                             PeppolReportingItem.MAX_LEN_DOCTYPE_VALUE),
                                                                                                           DBValueHelper.getTrimmedToLength (aReportingItem.getProcessIDScheme (),
                                                                                                                                             PeppolReportingItem.MAX_LEN_PROCESS_SCHEME),
                                                                                                           DBValueHelper.getTrimmedToLength (aReportingItem.getProcessIDValue (),
                                                                                                                                             PeppolReportingItem.MAX_LEN_PROCESS_VALUE),
                                                                                                           DBValueHelper.getTrimmedToLength (aReportingItem.getTransportProtocol (),
                                                                                                                                             PeppolReportingItem.MAX_LEN_TRANSPORT_PROTOCOL),
                                                                                                           DBValueHelper.getTrimmedToLength (aReportingItem.getC1CountryCode (),
                                                                                                                                             PeppolReportingItem.MAX_LEN_C1_COUNTRY_CODE),
                                                                                                           DBValueHelper.getTrimmedToLength (aReportingItem.getC4CountryCode (),
                                                                                                                                             PeppolReportingItem.MAX_LEN_C4_COUNTRY_CODE),
                                                                                                           DBValueHelper.getTrimmedToLength (aReportingItem.getEndUserID (),
                                                                                                                                             PeppolReportingItem.MAX_LEN_END_USER_ID)));
        if (nCreated != 1)
          throw new IllegalStateException ("Failed to create new SQL DB entry (" + nCreated + ")");
      });
      if (eSuccess.isFailure ())
        throw new IllegalStateException ("Failed to insert into Peppol Reporting into SQL DB");

      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Successfully stored Peppol Reporting Item in SQL DB");
    }
    else
    {
      LOGGER.info ("Not storing Peppol Reporting Item in SQL DB, as the document type is not eligable for reporting (" +
                   CIdentifier.getURIEncoded (aReportingItem.getDocTypeIDScheme (),
                                              aReportingItem.getDocTypeIDValue ()) +
                   ")");
    }
  }

  @Nonnull
  public Iterable <PeppolReportingItem> iterateReportingItems (@Nonnull final LocalDate aStartDateIncl,
                                                               @Nonnull final LocalDate aEndDateIncl) throws PeppolReportingBackendException
  {
    ValueEnforcer.notNull (aStartDateIncl, "StartDateIncl");
    ValueEnforcer.notNull (aEndDateIncl, "EndDateIncl");
    ValueEnforcer.isTrue ( () -> aEndDateIncl.compareTo (aStartDateIncl) >= 0, "EndDateIncl must be >= StartDateIncl");

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Querying Peppol Reporting Items from SQL DB between " + aStartDateIncl + " and " + aEndDateIncl);

    if (!isInitialized ())
      throw new IllegalStateException ("The Peppol Reporting SQL DB backend is not initialized");

    final ICommonsList <DBResultRow> aDBResult = _newExecutor ().queryAll ("SELECT exchangedt, sending, c2id, c3id, dtscheme, dtvalue, procscheme, procvalue, tp, c1cc, c4cc, enduserid" +
                                                                           " FROM " +
                                                                           m_sTableNamePrefix +
                                                                           "peppol_reporting_item" +
                                                                           " WHERE exchangedt >= ? AND exchangedt < ?",
                                                                           new ConstantPreparedStatementDataProvider (DBValueHelper.toTimestamp (aStartDateIncl.atStartOfDay ()),
                                                                                                                      DBValueHelper.toTimestamp (aEndDateIncl.plusDays (1)
                                                                                                                                                             .atStartOfDay ())));

    final ICommonsList <PeppolReportingItem> ret = new CommonsArrayList <> ();
    if (aDBResult != null)
      for (final DBResultRow aRow : aDBResult)
      {
        ret.add (PeppolReportingItem.builder ()
                                    .exchangeDateTimeInUTC (aRow.getAsLocalDateTime (0))
                                    .direction (aRow.getAsBoolean (1) ? EReportingDirection.SENDING
                                                                      : EReportingDirection.RECEIVING)
                                    .c2ID (aRow.getAsString (2))
                                    .c3ID (aRow.getAsString (3))
                                    .docTypeIDScheme (aRow.getAsString (4))
                                    .docTypeIDValue (aRow.getAsString (5))
                                    .processIDScheme (aRow.getAsString (6))
                                    .processIDValue (aRow.getAsString (7))
                                    .transportProtocol (aRow.getAsString (8))
                                    .c1CountryCode (aRow.getAsString (9))
                                    .c4CountryCode (aRow.getAsString (10))
                                    .endUserID (aRow.getAsString (11))
                                    .build ());
      }

    return ret;
  }
}
