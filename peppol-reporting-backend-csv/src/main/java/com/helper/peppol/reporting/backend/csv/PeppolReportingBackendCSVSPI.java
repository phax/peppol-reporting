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
package com.helper.peppol.reporting.backend.csv;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ELockType;
import com.helger.commons.annotation.IsSPIImplementation;
import com.helger.commons.annotation.MustBeLocked;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.VisibleForTesting;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.concurrent.SimpleReadWriteLock;
import com.helger.commons.csv.CCSV;
import com.helger.commons.csv.CSVReader;
import com.helger.commons.csv.CSVWriter;
import com.helger.commons.datetime.PDTFromString;
import com.helger.commons.io.file.FileHelper;
import com.helger.commons.state.ESuccess;
import com.helger.commons.string.StringHelper;
import com.helger.config.IConfig;
import com.helper.peppol.reporting.api.EReportingDirection;
import com.helper.peppol.reporting.api.PeppolReportingItem;
import com.helper.peppol.reporting.api.backend.IPeppolReportingBackendSPI;
import com.helper.peppol.reporting.api.backend.PeppolReportingBackendException;

/**
 * SPI implementation of {@link IPeppolReportingBackendSPI} for CSV.
 *
 * @author Philip Helger
 */
@IsSPIImplementation
public class PeppolReportingBackendCSVSPI implements IPeppolReportingBackendSPI
{
  public static final String CONFIG_PEPPOL_REPORTING_CSV_FILENAME = "peppol.reporting.csv.filename";
  public static final String CONFIG_PEPPOL_REPORTING_CSV_SEPARATOR_CHAR = "peppol.reporting.csv.separator-char";
  public static final String CONFIG_PEPPOL_REPORTING_CSV_QUOTE_CHAR = "peppol.reporting.csv.quote-char";
  public static final String CONFIG_PEPPOL_REPORTING_CSV_ESCAPE_CHAR = "peppol.reporting.csv.escape-char";

  private static final Logger LOGGER = LoggerFactory.getLogger (PeppolReportingBackendCSVSPI.class);

  private final SimpleReadWriteLock m_aRWLock = new SimpleReadWriteLock ();
  private File m_aCSVFile;
  private char m_cSeparatorChar;
  private char m_cQuoteChar;
  private char m_cEscapeChar;
  private CSVWriter m_aCSVWriter;

  @Nonnull
  @Nonempty
  public String getDisplayName ()
  {
    return "CSV";
  }

  private static char _asChar (@Nonnull final IConfig aConfig, final String sProperty, final char cDefault)
  {
    final String sValue = aConfig.getAsString (sProperty);
    if (StringHelper.hasNoText (sValue))
      return cDefault;
    if (sValue.length () > 1)
      LOGGER.warn ("The configuration property '" +
                   sProperty +
                   "' value length of " +
                   sValue.length () +
                   " is stripped to the first character");
    return sValue.charAt (0);
  }

  @Nonnull
  public ESuccess initBackend (@Nonnull final IConfig aConfig)
  {
    final String sFilename = aConfig.getAsString (CONFIG_PEPPOL_REPORTING_CSV_FILENAME);
    if (StringHelper.hasNoText (sFilename))
    {
      LOGGER.error ("The CSV filename is missing in the configuration. See property '" +
                    CONFIG_PEPPOL_REPORTING_CSV_FILENAME +
                    "'");
      return ESuccess.FAILURE;
    }
    final File aFile = new File (sFilename);
    if (!FileHelper.canReadAndWriteFile (aFile))
    {
      LOGGER.error ("The CSV filename '" + sFilename + "' is missing the necessary access rights to read and write");
      return ESuccess.FAILURE;
    }

    m_cSeparatorChar = _asChar (aConfig, CONFIG_PEPPOL_REPORTING_CSV_SEPARATOR_CHAR, CCSV.DEFAULT_SEPARATOR);
    m_cQuoteChar = _asChar (aConfig, CONFIG_PEPPOL_REPORTING_CSV_QUOTE_CHAR, CCSV.DEFAULT_QUOTE_CHARACTER);
    m_cEscapeChar = _asChar (aConfig, CONFIG_PEPPOL_REPORTING_CSV_ESCAPE_CHAR, CCSV.DEFAULT_ESCAPE_CHARACTER);

    m_aRWLock.writeLocked ( () -> {
      m_aCSVFile = aFile;
      try
      {
        m_aCSVWriter = new CSVWriter (new FileWriter (aFile, StandardCharsets.UTF_8, true));
        m_aCSVWriter.setSeparatorChar (m_cSeparatorChar).setQuoteChar (m_cQuoteChar).setEscapeChar (m_cEscapeChar);
      }
      catch (final IOException ex)
      {
        throw new IllegalStateException ("Failed to create CSV Writer", ex);
      }
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
    return m_aRWLock.readLockedBoolean ( () -> m_aCSVWriter != null);
  }

  @MustBeLocked (ELockType.WRITE)
  private void _shutdown ()
  {
    m_aCSVFile = null;
    try
    {
      m_aCSVWriter.close ();
    }
    catch (final IOException ex)
    {
      LOGGER.error ("Failed to close the CSV Writer", ex);
    }
    m_aCSVWriter = null;
  }

  public void shutdownBackend ()
  {
    if (isInitialized ())
    {
      m_aRWLock.writeLocked ( () -> {
        LOGGER.info ("Shutting down Peppol Reporting CSV client");
        _shutdown ();
      });
    }
    else
      LOGGER.warn ("The Peppol Reporting CSV backend cannot be shutdown, because it was never properly initialized");
  }

  @Nonnull
  @VisibleForTesting
  static ICommonsList <String> asCSV (@Nonnull final PeppolReportingItem aValue)
  {
    ValueEnforcer.notNull (aValue, "Value");

    final ICommonsList <String> ret = new CommonsArrayList <> ();
    ret.add (DateTimeFormatter.ISO_DATE_TIME.format (aValue.getExchangeDTUTC ()));
    ret.add (DateTimeFormatter.ISO_LOCAL_DATE.format (aValue.getExchangeDTUTC ().toLocalDate ()));
    ret.add (aValue.getDirection ().getID ());
    ret.add (aValue.getC2ID ());
    ret.add (aValue.getC3ID ());
    ret.add (aValue.getDocTypeIDScheme ());
    ret.add (aValue.getDocTypeIDValue ());
    ret.add (aValue.getProcessIDScheme ());
    ret.add (aValue.getProcessIDValue ());
    ret.add (aValue.getTransportProtocol ());
    ret.add (aValue.getC1CountryCode ());
    if (aValue.hasC4CountryCode ())
      ret.add (aValue.getC4CountryCode ());
    else
      ret.add (null);
    ret.add (aValue.getEndUserID ());
    return ret;
  }

  public void storeReportingItem (@Nonnull final PeppolReportingItem aReportingItem) throws PeppolReportingBackendException
  {
    ValueEnforcer.notNull (aReportingItem, "ReportingItem");

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Trying to store Peppol Reporting Item in CSV");

    if (!isInitialized ())
      throw new IllegalStateException ("The Peppol Reporting CSV backend is not initialized");

    m_aCSVWriter.writeNext (asCSV (aReportingItem));
    try
    {
      m_aCSVWriter.flush ();
    }
    catch (final IOException ex)
    {
      throw new PeppolReportingBackendException ("Failed to flush CSV file", ex);
    }

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Successfully stored Peppol Reporting Item in CSV");
  }

  @Nonnull
  @VisibleForTesting
  static PeppolReportingItem asItem (@Nonnull final ICommonsList <String> aValue)
  {
    ValueEnforcer.notNull (aValue, "Value");

    return PeppolReportingItem.builder ()
                              .exchangeDateTime (PDTFromString.getOffsetDateTimeFromString (aValue.get (0),
                                                                                            DateTimeFormatter.ISO_DATE_TIME))
                              .direction (EReportingDirection.getFromIDOrThrow (aValue.get (2)))
                              .c2ID (aValue.get (3))
                              .c3ID (aValue.get (4))
                              .docTypeIDScheme (aValue.get (5))
                              .docTypeIDValue (aValue.get (6))
                              .processIDScheme (aValue.get (7))
                              .processIDValue (aValue.get (8))
                              .transportProtocol (aValue.get (9))
                              .c1CountryCode (aValue.get (10))
                              .c4CountryCode (StringHelper.getNotEmpty (aValue.get (11), (String) null))
                              .endUserID (aValue.get (12))
                              .build ();

  }

  public void forEachReportingItem (@Nonnull final LocalDate aStartDateIncl,
                                    @Nonnull final LocalDate aEndDateIncl,
                                    @Nonnull final Consumer <? super PeppolReportingItem> aConsumer) throws PeppolReportingBackendException
  {
    ValueEnforcer.notNull (aStartDateIncl, "StartDateIncl");
    ValueEnforcer.notNull (aEndDateIncl, "EndDateIncl");
    ValueEnforcer.isTrue ( () -> aEndDateIncl.compareTo (aStartDateIncl) >= 0, "EndDateIncl must be >= StartDateIncl");
    ValueEnforcer.notNull (aConsumer, "Consumer");

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Querying Peppol Reporting Items from CSV between " + aStartDateIncl + " and " + aEndDateIncl);

    if (!isInitialized ())
      throw new IllegalStateException ("The Peppol Reporting CSV backend is not initialized");

    final int nCounter = 0;
    try (final CSVReader aReader = new CSVReader (new FileReader (m_aCSVFile, StandardCharsets.UTF_8)))
    {
      aReader.setSeparatorChar (m_cSeparatorChar).setQuoteChar (m_cQuoteChar).setEscapeChar (m_cEscapeChar);

      ICommonsList <String> aLine;
      while ((aLine = aReader.readNext ()) != null)
      {
        // First check the date
        final LocalDate aExchangeDate = PDTFromString.getLocalDateFromString (aLine.get (1),
                                                                              DateTimeFormatter.ISO_LOCAL_DATE);
        if (aExchangeDate.compareTo (aStartDateIncl) >= 0 && aExchangeDate.compareTo (aEndDateIncl) <= 0)
        {
          // Build only on match
          final PeppolReportingItem aReportingItem = asItem (aLine);
          aConsumer.accept (aReportingItem);
        }
      }
    }
    catch (final IOException ex)
    {
      throw new PeppolReportingBackendException ("IO error in reading CSV", ex);
    }

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Found a total of " + nCounter + " matching documents in CSV");
  }

  @Nonnull
  public Iterable <PeppolReportingItem> iterateReportingItems (@Nonnull final LocalDate aStartDateIncl,
                                                               @Nonnull final LocalDate aEndDateIncl) throws PeppolReportingBackendException
  {
    final ICommonsList <PeppolReportingItem> ret = new CommonsArrayList <> ();
    forEachReportingItem (aStartDateIncl, aEndDateIncl, ret::add);
    return ret;
  }
}
