/*
 * Copyright (C) 2023-2026 Philip Helger
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
import java.util.function.Consumer;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.ThreadSafe;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.state.ESuccess;
import com.helger.base.wrapper.Wrapper;
import com.helger.db.api.config.IJdbcConfiguration;
import com.helger.db.api.helper.DBValueHelper;
import com.helger.db.jdbc.DataSourceProviderFromJdbcConfiguration;
import com.helger.db.jdbc.callback.ConstantPreparedStatementDataProvider;
import com.helger.db.jdbc.callback.IResultSetRowCallback;
import com.helger.db.jdbc.executor.DBExecutor;
import com.helger.peppol.reporting.api.EReportingDirection;
import com.helger.peppol.reporting.api.PeppolReportingItem;
import com.helger.peppol.reporting.api.backend.AbstractPeppolReportingBackendWithReader;
import com.helger.peppol.reporting.api.backend.PeppolReportingBackendException;

/**
 * Peppol Reporting backend that stores the reporting items in an SQL database. This backend
 * supports the streaming of report items via
 * {@link #forEachReportingItem(LocalDate, LocalDate, Consumer)}, where each item is passed to the
 * consumer while the result set is read. {@link #iterateReportingItems(LocalDate, LocalDate)}
 * collects all matching items in memory.
 *
 * @author Philip Helger
 * @since 5.0.0
 */
@ThreadSafe
public class PeppolReportingBackendSql extends AbstractPeppolReportingBackendWithReader
{
  public static final String DISPLAY_NAME = "SQL";

  private static final Logger LOGGER = LoggerFactory.getLogger (PeppolReportingBackendSql.class);

  private final IJdbcConfiguration m_aJdbcConfig;
  private final DataSourceProviderFromJdbcConfiguration m_aDSP;
  private final String m_sTableNamePrefix;

  /**
   * Constructor. The database schema must already exist.
   *
   * @param aJdbcConfig
   *        The JDBC configuration to use. May not be <code>null</code>.
   * @param aDSP
   *        The data source provider to use. May not be <code>null</code>. It is closed together with
   *        this backend.
   * @param sTableNamePrefix
   *        The table name prefix to use (e.g. the schema name with a trailing dot). May not be
   *        <code>null</code> but maybe empty.
   */
  public PeppolReportingBackendSql (@NonNull final IJdbcConfiguration aJdbcConfig,
                                    @NonNull final DataSourceProviderFromJdbcConfiguration aDSP,
                                    @NonNull final String sTableNamePrefix)
  {
    super (DISPLAY_NAME);
    ValueEnforcer.notNull (aJdbcConfig, "JdbcConfig");
    ValueEnforcer.notNull (aDSP, "DSP");
    ValueEnforcer.notNull (sTableNamePrefix, "TableNamePrefix");
    m_aJdbcConfig = aJdbcConfig;
    m_aDSP = aDSP;
    m_sTableNamePrefix = sTableNamePrefix;
  }

  @NonNull
  private DBExecutor _newExecutor ()
  {
    return new ReportingDBExecutor (m_aDSP, m_aJdbcConfig);
  }

  @Override
  protected void storeEligibleReportingItem (@NonNull @Nonempty final String sItemID,
                                             @NonNull final PeppolReportingItem aReportingItem) throws PeppolReportingBackendException
  {
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
      throw new PeppolReportingBackendException ("Failed to insert Peppol Reporting item into SQL DB");
  }

  @Override
  protected void forEachReportingItemInRange (@NonNull final LocalDate aStartDateIncl,
                                              @NonNull final LocalDate aEndDateIncl,
                                              @NonNull final Consumer <? super PeppolReportingItem> aConsumer) throws PeppolReportingBackendException
  {
    // Map each row while the ResultSet is open and pass it to the consumer
    // directly, so that no list of all reporting items is created. The
    // DBExecutor turns all exceptions into a failure, so remember the one of
    // the consumer to re-throw it afterwards.
    final Wrapper <RuntimeException> aConsumerEx = new Wrapper <> ();
    final IResultSetRowCallback aResultRowCallback = aRow -> {
      final PeppolReportingItem aItem = PeppolReportingItem.builder ()
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
                                                           .build ();
      try
      {
        aConsumer.accept (aItem);
      }
      catch (final RuntimeException ex)
      {
        aConsumerEx.set (ex);
        // Abort the iteration
        throw ex;
      }
    };
    final ESuccess eSuccess = _newExecutor ().queryAll ("SELECT exchangedt, sending, c2id, c3id, dtscheme, dtvalue, procscheme, procvalue, tp, c1cc, c4cc, enduserid" +
                                                        " FROM " +
                                                        m_sTableNamePrefix +
                                                        "peppol_reporting_item" +
                                                        " WHERE exchangedt >= ? AND exchangedt < ?",
                                                        new ConstantPreparedStatementDataProvider (DBValueHelper.toTimestamp (aStartDateIncl.atStartOfDay ()),
                                                                                                   DBValueHelper.toTimestamp (aEndDateIncl.plusDays (1)
                                                                                                                                          .atStartOfDay ())),
                                                        aResultRowCallback);
    if (aConsumerEx.get () != null)
      throw aConsumerEx.get ();
    if (eSuccess.isFailure ())
      throw new PeppolReportingBackendException ("Failed to query Peppol Reporting items from SQL DB");
  }

  @Override
  protected void onClose ()
  {
    try
    {
      m_aDSP.close ();
    }
    catch (final IOException ex)
    {
      LOGGER.error ("Failed to close Peppol Reporting DataSource provider", ex);
    }
  }
}
