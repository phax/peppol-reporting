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
package com.helger.peppol.reporting.backend.mongodb;

import org.bson.Document;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.annotation.style.IsSPIImplementation;
import com.helger.annotation.style.OverrideOnDemand;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.string.StringHelper;
import com.helger.config.IConfig;
import com.helger.peppol.reporting.api.backend.IPeppolReportingBackendProviderSPI;
import com.helger.peppol.reporting.api.backend.PeppolReportingBackendException;
import com.mongodb.MongoClientException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Indexes;

/**
 * SPI implementation of {@link IPeppolReportingBackendProviderSPI} that creates
 * {@link PeppolReportingBackendMongoDB} instances.
 *
 * @author Philip Helger
 */
@IsSPIImplementation
public class PeppolReportingBackendMongoDBSPI implements IPeppolReportingBackendProviderSPI
{
  public static final String BACKEND_ID = "mongodb";

  public static final String DEFAULT_COLLECTION = "reporting-items";
  private static final String CONFIG_PREFIX = "peppol.reporting.mongodb.";
  public static final String CONFIG_PEPPOL_REPORTING_MONGODB_CONNECTIONSTRING = CONFIG_PREFIX + "connectionstring";
  public static final String CONFIG_PEPPOL_REPORTING_MONGODB_DBNAME = CONFIG_PREFIX + "dbname";
  public static final String CONFIG_PEPPOL_REPORTING_MONGODB_COLLECTION = CONFIG_PREFIX + "collection";

  private static final Logger LOGGER = LoggerFactory.getLogger (PeppolReportingBackendMongoDBSPI.class);

  @NonNull
  @Nonempty
  public String getBackendID ()
  {
    return BACKEND_ID;
  }

  @NonNull
  @Nonempty
  public String getDisplayName ()
  {
    return PeppolReportingBackendMongoDB.DISPLAY_NAME;
  }

  /**
   * Create the default MongoDB client wrapper based on the configuration.
   *
   * @param aConfig
   *        The configuration to use. May not be <code>null</code>.
   * @return The client wrapper and never <code>null</code>.
   * @throws PeppolReportingBackendException
   *         If the configuration is invalid
   */
  @NonNull
  public static MongoClientWrapper createDefaultClientWrapper (@NonNull final IConfig aConfig) throws PeppolReportingBackendException
  {
    // Get connection string from configuration
    final String sConnectionString = aConfig.getAsString (CONFIG_PEPPOL_REPORTING_MONGODB_CONNECTIONSTRING);
    if (StringHelper.isEmpty (sConnectionString))
      throw new PeppolReportingBackendException ("The MongoDB connection string is missing in the configuration. See property '" +
                                                 CONFIG_PEPPOL_REPORTING_MONGODB_CONNECTIONSTRING +
                                                 "'");

    // Get database name from configuration
    final String sDBName = aConfig.getAsString (CONFIG_PEPPOL_REPORTING_MONGODB_DBNAME);
    if (StringHelper.isEmpty (sDBName))
      throw new PeppolReportingBackendException ("The MongoDB database name is missing in the configuration. See property '" +
                                                 CONFIG_PEPPOL_REPORTING_MONGODB_DBNAME +
                                                 "'");

    LOGGER.info ("Using Peppol Reporting MongoDB database name '" + sDBName + "'");
    return new MongoClientWrapper (sConnectionString, sDBName);
  }

  /**
   * Create the MongoDB client wrapper to use.
   *
   * @param aConfig
   *        The configuration to use. Never <code>null</code>.
   * @return The client wrapper and never <code>null</code>.
   * @throws PeppolReportingBackendException
   *         If the configuration is invalid
   */
  @NonNull
  @OverrideOnDemand
  protected MongoClientWrapper createClientWrapper (@NonNull final IConfig aConfig) throws PeppolReportingBackendException
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
  public PeppolReportingBackendMongoDB createBackend (@NonNull final IConfig aConfig) throws PeppolReportingBackendException
  {
    ValueEnforcer.notNull (aConfig, "Config");

    final String sCollection = getMongoCollectionName (aConfig);
    if (StringHelper.isEmpty (sCollection))
      throw new PeppolReportingBackendException ("The Peppol Reporting MongoDB backend collection name may not be empty");

    final MongoClientWrapper aClientWrapper = createClientWrapper (aConfig);
    try
    {
      // Make sure indexes are present
      final MongoCollection <Document> aCollection = aClientWrapper.getCollection (sCollection);
      aCollection.createIndex (Indexes.ascending (PeppolReportingMongoDBHelper.BSON_EXCHANGEDATE,
                                                  PeppolReportingMongoDBHelper.BSON_EXCHANGEDT));
      aCollection.createIndex (Indexes.ascending (PeppolReportingMongoDBHelper.BSON_EXCHANGEDATE));
    }
    catch (final MongoClientException ex)
    {
      // E.g. MongoTimeoutException if MongoDB server is not reachable
      aClientWrapper.close ();
      throw new PeppolReportingBackendException ("Failed to create indices in Peppol Reporting MongoDB", ex);
    }

    return new PeppolReportingBackendMongoDB (aClientWrapper, sCollection);
  }
}
