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

import java.time.LocalDate;
import java.util.function.Consumer;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.ThreadSafe;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.peppol.reporting.api.PeppolReportingItem;
import com.helger.peppol.reporting.api.backend.AbstractPeppolReportingBackendWithReader;
import com.helger.peppol.reporting.api.backend.PeppolReportingBackendException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;

/**
 * Peppol Reporting backend that stores the reporting items in MongoDB. This backend supports the
 * lazy gathering of report items: both iterating the result of
 * {@link #iterateReportingItems(LocalDate, LocalDate)} and using
 * {@link #forEachReportingItem(LocalDate, LocalDate, Consumer)} stream the items via a MongoDB
 * cursor.
 *
 * @author Philip Helger
 * @since 5.0.0
 */
@ThreadSafe
public class PeppolReportingBackendMongoDB extends AbstractPeppolReportingBackendWithReader
{
  public static final String DISPLAY_NAME = "MongoDB";

  private static final Logger LOGGER = LoggerFactory.getLogger (PeppolReportingBackendMongoDB.class);

  private final MongoClientWrapper m_aClientWrapper;
  private final String m_sCollection;

  /**
   * Constructor.
   *
   * @param aClientWrapper
   *        The MongoDB client wrapper to use. May not be <code>null</code>. It is closed together
   *        with this backend.
   * @param sCollection
   *        The name of the collection to store the reporting items in. May neither be
   *        <code>null</code> nor empty.
   */
  public PeppolReportingBackendMongoDB (@NonNull final MongoClientWrapper aClientWrapper,
                                        @NonNull @Nonempty final String sCollection)
  {
    super (DISPLAY_NAME);
    ValueEnforcer.notNull (aClientWrapper, "ClientWrapper");
    ValueEnforcer.notEmpty (sCollection, "Collection");
    m_aClientWrapper = aClientWrapper;
    m_sCollection = sCollection;
  }

  /**
   * @return The internal MongoDB client wrapper. Never <code>null</code>.
   */
  @NonNull
  public final MongoClientWrapper getClientWrapper ()
  {
    return m_aClientWrapper;
  }

  @NonNull
  private MongoCollection <Document> _getCollection ()
  {
    return m_aClientWrapper.getCollection (m_sCollection);
  }

  @Override
  protected void storeEligibleReportingItem (@NonNull @Nonempty final String sItemID,
                                             @NonNull final PeppolReportingItem aReportingItem) throws PeppolReportingBackendException
  {
    if (!m_aClientWrapper.isDBWritable ())
      throw new PeppolReportingBackendException ("The Peppol Reporting MongoDB is not writable");

    if (!_getCollection ().insertOne (PeppolReportingMongoDBHelper.toBson (aReportingItem)).wasAcknowledged ())
      throw new PeppolReportingBackendException ("Failed to insert into Peppol Reporting MongoDB Collection");
  }

  @NonNull
  private MongoIterable <PeppolReportingItem> _findReportingItems (@NonNull final LocalDate aStartDateIncl,
                                                                   @NonNull final LocalDate aEndDateIncl)
  {
    // Find between date, but order by exchange date and time
    final Bson aFilter = Filters.and (Filters.gte (PeppolReportingMongoDBHelper.BSON_EXCHANGEDATE, aStartDateIncl),
                                      Filters.lte (PeppolReportingMongoDBHelper.BSON_EXCHANGEDATE, aEndDateIncl));

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Using MongoDB filter '" + aFilter.toBsonDocument ().toJson () + "'");

    return _getCollection ().find (aFilter)
                            .sort (Sorts.ascending (PeppolReportingMongoDBHelper.BSON_EXCHANGEDT))
                            .map (PeppolReportingMongoDBHelper::toDomain);
  }

  @Override
  protected void forEachReportingItemInRange (@NonNull final LocalDate aStartDateIncl,
                                              @NonNull final LocalDate aEndDateIncl,
                                              @NonNull final Consumer <? super PeppolReportingItem> aConsumer)
  {
    // The cursor is closed, even if the consumer throws an exception
    try (final MongoCursor <PeppolReportingItem> aCursor = _findReportingItems (aStartDateIncl, aEndDateIncl).iterator ())
    {
      while (aCursor.hasNext ())
        aConsumer.accept (aCursor.next ());
    }
  }

  @Override
  @NonNull
  public Iterable <PeppolReportingItem> iterateReportingItems (@NonNull final LocalDate aStartDateIncl,
                                                               @NonNull final LocalDate aEndDateIncl)
  {
    validateDateRange (aStartDateIncl, aEndDateIncl);
    checkNotClosed ();
    return _findReportingItems (aStartDateIncl, aEndDateIncl);
  }

  @Override
  protected void onClose ()
  {
    m_aClientWrapper.close ();
  }
}
