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
package com.helper.peppol.reporting.backend.mongodb;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nonnull;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.impl.CommonsHashMap;
import com.helger.commons.collection.impl.ICommonsMap;
import com.helger.commons.debug.GlobalDebug;
import com.helger.commons.io.stream.StreamHelper;
import com.helger.commons.mutable.MutableInt;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.event.ClusterDescriptionChangedEvent;
import com.mongodb.event.ClusterListener;
import com.mongodb.event.CommandFailedEvent;
import com.mongodb.event.CommandListener;
import com.mongodb.event.CommandSucceededEvent;

/**
 * A provider for MongoDB collection.
 *
 * @author Philip Helger
 */
public class MongoClientWrapper implements AutoCloseable
{
  private static final class LoggingCommandListener implements CommandListener
  {
    private static final Logger LOGGER = LoggerFactory.getLogger (MongoClientWrapper.LoggingCommandListener.class);
    private final ICommonsMap <String, MutableInt> m_aCommands = new CommonsHashMap <> ();

    @Override
    public synchronized void commandSucceeded (final CommandSucceededEvent event)
    {
      final String sCommandName = event.getCommandName ();
      final int nCount = m_aCommands.computeIfAbsent (sCommandName, k -> new MutableInt (0)).inc ();

      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Successfully executed '" + sCommandName + "' [" + nCount + "]");
    }

    @Override
    public void commandFailed (final CommandFailedEvent event)
    {
      LOGGER.error ("Failed execution of command '" + event.getCommandName () + "' with id " + event.getRequestId ());
    }
  }

  private static class IsWriteable implements ClusterListener
  {
    private static final Logger LOGGER = LoggerFactory.getLogger (MongoClientWrapper.IsWriteable.class);
    private final AtomicBoolean m_aIsWritable = new AtomicBoolean (false);

    @Override
    public void clusterDescriptionChanged (final ClusterDescriptionChangedEvent aEvent)
    {
      if (!m_aIsWritable.get ())
      {
        if (aEvent.getNewDescription ().hasWritableServer ())
        {
          m_aIsWritable.set (true);
          LOGGER.info ("Able to write to server");
        }
      }
      else
      {
        if (!aEvent.getNewDescription ().hasWritableServer ())
        {
          m_aIsWritable.set (false);
          LOGGER.error ("Unable to write to server");
        }
      }
    }

    public boolean isWritable ()
    {
      return m_aIsWritable.get ();
    }
  }

  private final MongoClient m_aMongoClient;
  private final MongoDatabase m_aDatabase;
  private final IsWriteable m_aClusterListener = new IsWriteable ();

  public MongoClientWrapper (@Nonnull @Nonempty final String sConnectionString, @Nonnull @Nonempty final String sDBName)
  {
    ValueEnforcer.notEmpty (sConnectionString, "ConnectionString");
    ValueEnforcer.notEmpty (sDBName, "DBName");

    final MongoClientSettings aClientSettings = MongoClientSettings.builder ()
                                                                   .applicationName ("Peppol Reporting Backend")
                                                                   .applyConnectionString (new ConnectionString (sConnectionString))
                                                                   .addCommandListener (new LoggingCommandListener ())
                                                                   .applyToClusterSettings (x -> {
                                                                     x.addClusterListener (m_aClusterListener);
                                                                     if (GlobalDebug.isDebugMode ())
                                                                     {
                                                                       /*
                                                                        * Less
                                                                        * waiting
                                                                        * time
                                                                        */
                                                                       x.serverSelectionTimeout (10, TimeUnit.SECONDS);
                                                                     }
                                                                   })
                                                                   .build ();
    m_aMongoClient = MongoClients.create (aClientSettings);
    m_aDatabase = m_aMongoClient.getDatabase (sDBName);
  }

  public void close ()
  {
    StreamHelper.close (m_aMongoClient);
  }

  public boolean isDBWritable ()
  {
    return m_aClusterListener.isWritable ();
  }

  /**
   * Get the accessor to the MongoDB collection with the specified name
   *
   * @param sName
   *        Collection name. May neither be <code>null</code> nor empty.
   * @return The collection with the specified name.
   */
  @Nonnull
  public MongoCollection <Document> getCollection (@Nonnull @Nonempty final String sName)
  {
    ValueEnforcer.notEmpty (sName, "Name");

    return m_aDatabase.getCollection (sName);
  }
}
