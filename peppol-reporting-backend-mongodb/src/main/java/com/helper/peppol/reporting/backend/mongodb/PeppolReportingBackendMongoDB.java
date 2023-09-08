package com.helper.peppol.reporting.backend.mongodb;

import java.time.LocalDate;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.IsSPIImplementation;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.state.ESuccess;
import com.helger.commons.string.StringHelper;
import com.helger.config.IConfig;
import com.helper.peppol.reporting.api.PeppolReportingItem;
import com.helper.peppol.reporting.api.backend.IPeppolReportingBackendSPI;
import com.helper.peppol.reporting.api.backend.PeppolReportingBackendException;
import com.mongodb.client.MongoCollection;

/**
 * SPI implementation of {@link IPeppolReportingBackendSPI} for MongoDB.
 *
 * @author Philip Helger
 */
@IsSPIImplementation
public class PeppolReportingBackendMongoDB implements IPeppolReportingBackendSPI
{
  public static final String CONFIG_PEPPOL_REPORTING_MONGODB_CONNECTIONSTRING = "peppol.reporting.mongodb.connectionstring";
  public static final String CONFIG_PEPPOL_REPORTING_MONGODB_DBNAME = "peppol.reporting.mongodb.dbname";

  private static final Logger LOGGER = LoggerFactory.getLogger (PeppolReportingBackendMongoDB.class);

  private MongoClientWrapper m_aClientWrapper;

  @Nonnull
  @Nonempty
  public String getDisplayName ()
  {
    return "MongoDB";
  }

  @Nonnull
  public ESuccess initBackend (@Nonnull final IConfig aConfig)
  {
    if (m_aClientWrapper != null)
      throw new IllegalStateException ("The Peppol Reporting MongoDB backend was already initialized");

    final String sConnectionString = aConfig.getAsString (CONFIG_PEPPOL_REPORTING_MONGODB_CONNECTIONSTRING);
    if (StringHelper.hasNoText (sConnectionString))
    {
      LOGGER.error ("The MongoDB connection string is missing in the configuration. See property '" +
                    CONFIG_PEPPOL_REPORTING_MONGODB_CONNECTIONSTRING +
                    "'");
      return ESuccess.FAILURE;
    }
    final String sDBName = aConfig.getAsString (CONFIG_PEPPOL_REPORTING_MONGODB_DBNAME);
    if (StringHelper.hasNoText (sDBName))
    {
      LOGGER.error ("The MongoDB database name is missing in the configuration. See property '" +
                    CONFIG_PEPPOL_REPORTING_MONGODB_DBNAME +
                    "'");
      return ESuccess.FAILURE;
    }

    LOGGER.info ("Using Peppol Reporting Mongo DB database name '" + sDBName + "'");
    m_aClientWrapper = new MongoClientWrapper (sConnectionString, sDBName);

    return ESuccess.SUCCESS;
  }

  public void shutdownBackend ()
  {
    if (m_aClientWrapper != null)
    {
      LOGGER.info ("Shutting down Peppol Reporting Mongo DB client");
      m_aClientWrapper.close ();
      m_aClientWrapper = null;
    }
    else
      LOGGER.warn ("The Peppol Reporting Mongo DB backend cannot be shutdown, because it was never properly initialized");
  }

  @Nonnull
  private MongoCollection <Document> _getCollection ()
  {
    return m_aClientWrapper.getCollection ("peppol-reporting");
  }

  public void storeReportingItem (@Nonnull final PeppolReportingItem aReportingItem) throws PeppolReportingBackendException
  {
    // TODO
    _getCollection ();
  }

  public void forEachReportingItem (@Nonnull final LocalDate aStartDateIncl,
                                    @Nonnull final LocalDate aEndDateIncl,
                                    @Nonnull final Consumer <? super PeppolReportingItem> aConsumer) throws PeppolReportingBackendException
  {
    // TODO

  }
}
