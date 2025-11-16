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
package com.helger.peppol.reporting.backend.mongodb;

import java.time.LocalDate;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.GuardedBy;
import com.helger.annotation.style.IsSPIImplementation;
import com.helger.annotation.style.OverrideOnDemand;
import com.helger.annotation.style.UsedViaReflection;
import com.helger.base.concurrent.SimpleReadWriteLock;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.state.ESuccess;
import com.helger.base.string.StringHelper;
import com.helger.config.IConfig;
import com.helger.peppol.reporting.api.PeppolReportingHelper;
import com.helger.peppol.reporting.api.PeppolReportingItem;
import com.helger.peppol.reporting.api.backend.IPeppolReportingBackendSPI;
import com.helger.peppol.reporting.api.backend.PeppolReportingBackendException;
import com.helger.peppolid.CIdentifier;
import com.mongodb.MongoClientException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Sorts;

/**
 * SPI implementation of {@link IPeppolReportingBackendSPI} for MongoDB. This backend supports the
 * lazy gathering of report items through an Iterator.
 *
 * @author Philip Helger
 */
@IsSPIImplementation
public class PeppolReportingBackendMongoDBSPI implements IPeppolReportingBackendSPI
{
  public static final String DEFAULT_COLLECTION = "reporting-items";
  private static final String CONFIG_PREFIX = "peppol.reporting.mongodb.";
  public static final String CONFIG_PEPPOL_REPORTING_MONGODB_CONNECTIONSTRING = CONFIG_PREFIX + "connectionstring";
  public static final String CONFIG_PEPPOL_REPORTING_MONGODB_DBNAME = CONFIG_PREFIX + "dbname";
  public static final String CONFIG_PEPPOL_REPORTING_MONGODB_COLLECTION = CONFIG_PREFIX + "collection";

  private static final Logger LOGGER = LoggerFactory.getLogger (PeppolReportingBackendMongoDBSPI.class);

  private final SimpleReadWriteLock m_aRWLock = new SimpleReadWriteLock ();
  @GuardedBy ("m_aRWLock")
  private MongoClientWrapper m_aClientWrapper;
  private String m_sCollection;

  @UsedViaReflection
  public PeppolReportingBackendMongoDBSPI ()
  {}

  @NonNull
  @Nonempty
  public String getDisplayName ()
  {
    return "MongoDB";
  }

  @Nullable
  public static MongoClientWrapper createDefaultClientWrapper (@NonNull final IConfig aConfig)
  {
    // Get connection string from configuration
    final String sConnectionString = aConfig.getAsString (CONFIG_PEPPOL_REPORTING_MONGODB_CONNECTIONSTRING);
    if (StringHelper.isEmpty (sConnectionString))
    {
      LOGGER.error ("The MongoDB connection string is missing in the configuration. See property '" +
                    CONFIG_PEPPOL_REPORTING_MONGODB_CONNECTIONSTRING +
                    "'");
      return null;
    }

    // Get database name from configuration
    final String sDBName = aConfig.getAsString (CONFIG_PEPPOL_REPORTING_MONGODB_DBNAME);
    if (StringHelper.isEmpty (sDBName))
    {
      LOGGER.error ("The MongoDB database name is missing in the configuration. See property '" +
                    CONFIG_PEPPOL_REPORTING_MONGODB_DBNAME +
                    "'");
      return null;
    }

    LOGGER.info ("Using Peppol Reporting MongoDB database name '" + sDBName + "'");
    return new MongoClientWrapper (sConnectionString, sDBName);
  }

  @Nullable
  @OverrideOnDemand
  protected MongoClientWrapper createClientWrapper (@NonNull final IConfig aConfig)
  {
    return createDefaultClientWrapper (aConfig);
  }

  /**
   * Get the MongoDB collection name to use.
   *
   * @param aConfig
   *        The configuration object to use. Never <code>null</code>.
   * @return The DB collection name to use. Any <code>null</code> or empty value will lead to an
   *         error.
   */
  @OverrideOnDemand
  @Nullable
  protected String getMongoCollectionName (@NonNull final IConfig aConfig)
  {
    // Configured collection introduced in 2.2.1
    final String sConfiguredCollection = aConfig.getAsString (CONFIG_PEPPOL_REPORTING_MONGODB_COLLECTION);
    return StringHelper.getNotEmpty (sConfiguredCollection, DEFAULT_COLLECTION);
  }

  @NonNull
  public ESuccess initBackend (@NonNull final IConfig aConfig)
  {
    m_aRWLock.writeLocked ( () -> {
      if (m_aClientWrapper != null)
        throw new IllegalStateException ("The Peppol Reporting MongoDB backend was already initialized");

      m_aClientWrapper = createClientWrapper (aConfig);

      // Configured collection introduced in 2.2.1
      m_sCollection = getMongoCollectionName (aConfig);
      if (StringHelper.isEmpty (m_sCollection))
        throw new IllegalStateException ("The Peppol Reporting MongoDB backend collection name may not be empty");

      // It may take some time, until the "DB writable" field returns true
    });

    if (!isInitialized ())
    {
      // Error was already logged
      return ESuccess.FAILURE;
    }

    try
    {
      // Make sure indexes are present
      _getCollection ().createIndex (Indexes.ascending (PeppolReportingMongoDBHelper.BSON_EXCHANGEDATE,
                                                        PeppolReportingMongoDBHelper.BSON_EXCHANGEDT));
      _getCollection ().createIndex (Indexes.ascending (PeppolReportingMongoDBHelper.BSON_EXCHANGEDATE));
    }
    catch (final MongoClientException ex)
    {
      // E.g. MongoTimeoutException if MongoDB server is not reachable
      LOGGER.error ("Failed to create indeces in Peppol Reporting MongoDB", ex);
      return ESuccess.FAILURE;
    }

    return ESuccess.SUCCESS;
  }

  /**
   * @return The internal MongoDB client wrapper. May be <code>null</code>.
   * @since 2.2.2
   */
  @Nullable
  public final MongoClientWrapper getClientWrapper ()
  {
    return m_aRWLock.readLockedGet ( () -> m_aClientWrapper);
  }

  public boolean isInitialized ()
  {
    return m_aRWLock.readLockedBoolean ( () -> m_aClientWrapper != null);
  }

  public void shutdownBackend ()
  {
    if (isInitialized ())
    {
      m_aRWLock.writeLocked ( () -> {
        LOGGER.info ("Shutting down Peppol Reporting MongoDB client");
        m_aClientWrapper.close ();
        m_aClientWrapper = null;
        m_sCollection = null;
      });
    }
    else
      LOGGER.warn ("The Peppol Reporting MongoDB backend cannot be shutdown, because it was never properly initialized");
  }

  @NonNull
  private MongoCollection <Document> _getCollection ()
  {
    return m_aRWLock.readLockedGet ( () -> m_aClientWrapper.getCollection (m_sCollection));
  }

  private boolean _isDBWritable ()
  {
    return m_aRWLock.readLockedBoolean ( () -> m_aClientWrapper.isDBWritable ());
  }

  public void storeReportingItem (@NonNull final PeppolReportingItem aReportingItem) throws PeppolReportingBackendException
  {
    ValueEnforcer.notNull (aReportingItem, "ReportingItem");

    if (PeppolReportingHelper.isDocumentTypeEligableForReporting (aReportingItem.getDocTypeIDScheme (),
                                                                  aReportingItem.getDocTypeIDValue ()))
    {
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Trying to store Peppol Reporting Item in MongoDB");

      if (!isInitialized ())
        throw new IllegalStateException ("The Peppol Reporting MongoDB backend is not initialized");

      if (!_isDBWritable ())
        throw new IllegalStateException ("The Peppol Reporting MongoDB is not writable");

      // Write to collection
      if (!_getCollection ().insertOne (PeppolReportingMongoDBHelper.toBson (aReportingItem)).wasAcknowledged ())
        throw new IllegalStateException ("Failed to insert into Peppol Reporting MongoDB Collection");

      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Successfully stored Peppol Reporting Item in MongoDB");
    }
    else
    {
      LOGGER.info ("Not storing Peppol Reporting Item in MongoDB, as the document type is not eligable for reporting (" +
                   CIdentifier.getURIEncoded (aReportingItem.getDocTypeIDScheme (),
                                              aReportingItem.getDocTypeIDValue ()) +
                   ")");
    }
  }

  @NonNull
  public Iterable <PeppolReportingItem> iterateReportingItems (@NonNull final LocalDate aStartDateIncl,
                                                               @NonNull final LocalDate aEndDateIncl) throws PeppolReportingBackendException
  {
    ValueEnforcer.notNull (aStartDateIncl, "StartDateIncl");
    ValueEnforcer.notNull (aEndDateIncl, "EndDateIncl");
    ValueEnforcer.isTrue ( () -> aEndDateIncl.compareTo (aStartDateIncl) >= 0, "EndDateIncl must be >= StartDateIncl");

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Querying Peppol Reporting Items from MongoDB between " + aStartDateIncl + " and " + aEndDateIncl);

    if (!isInitialized ())
      throw new IllegalStateException ("The Peppol Reporting MongoDB backend is not initialized");

    // Find between date, but order by exchange date and time
    final Bson aFilter = Filters.and (Filters.gte (PeppolReportingMongoDBHelper.BSON_EXCHANGEDATE, aStartDateIncl),
                                      Filters.lte (PeppolReportingMongoDBHelper.BSON_EXCHANGEDATE, aEndDateIncl));

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Using MongoDB filter '" + aFilter.toBsonDocument ().toJson () + "'");

    return _getCollection ().find (aFilter)
                            .sort (Sorts.ascending (PeppolReportingMongoDBHelper.BSON_EXCHANGEDT))
                            .map (PeppolReportingMongoDBHelper::toDomain);
  }
}
