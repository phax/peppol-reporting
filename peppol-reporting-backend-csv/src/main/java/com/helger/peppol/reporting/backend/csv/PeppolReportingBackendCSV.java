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
package com.helger.peppol.reporting.backend.csv;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.annotation.concurrent.GuardedBy;
import com.helger.annotation.concurrent.ThreadSafe;
import com.helger.annotation.style.VisibleForTesting;
import com.helger.base.concurrent.SimpleReadWriteLock;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.csv.CSVReader;
import com.helger.csv.CSVWriter;
import com.helger.datetime.format.PDTFromString;
import com.helger.peppol.reporting.api.EReportingDirection;
import com.helger.peppol.reporting.api.PeppolReportingItem;
import com.helger.peppol.reporting.api.backend.AbstractPeppolReportingBackendWithReader;
import com.helger.peppol.reporting.api.backend.PeppolReportingBackendException;

/**
 * Peppol Reporting backend that stores the reporting items in a CSV file.
 *
 * @author Philip Helger
 * @since 5.0.0
 */
@ThreadSafe
public class PeppolReportingBackendCSV extends AbstractPeppolReportingBackendWithReader
{
  public static final String DISPLAY_NAME = "CSV";

  private static final Logger LOGGER = LoggerFactory.getLogger (PeppolReportingBackendCSV.class);

  private final File m_aCSVFile;
  private final char m_cSeparatorChar;
  private final char m_cQuoteChar;
  private final char m_cEscapeChar;
  private final SimpleReadWriteLock m_aRWLock = new SimpleReadWriteLock ();
  @GuardedBy ("m_aRWLock")
  private final CSVWriter m_aCSVWriter;

  /**
   * Constructor. Opens the CSV file for appending.
   *
   * @param aCSVFile
   *        The CSV file to use. May not be <code>null</code>.
   * @param cSeparatorChar
   *        The CSV separator character.
   * @param cQuoteChar
   *        The CSV quote character.
   * @param cEscapeChar
   *        The CSV escape character.
   * @throws PeppolReportingBackendException
   *         If the CSV file cannot be opened
   */
  public PeppolReportingBackendCSV (@NonNull final File aCSVFile,
                                    final char cSeparatorChar,
                                    final char cQuoteChar,
                                    final char cEscapeChar) throws PeppolReportingBackendException
  {
    super (DISPLAY_NAME);
    ValueEnforcer.notNull (aCSVFile, "CSVFile");
    m_aCSVFile = aCSVFile;
    m_cSeparatorChar = cSeparatorChar;
    m_cQuoteChar = cQuoteChar;
    m_cEscapeChar = cEscapeChar;
    try
    {
      m_aCSVWriter = new CSVWriter (new FileWriter (aCSVFile, StandardCharsets.UTF_8, true));
    }
    catch (final IOException ex)
    {
      throw new PeppolReportingBackendException ("Failed to open CSV file '" + aCSVFile.getAbsolutePath () + "'", ex);
    }
    m_aCSVWriter.setSeparatorChar (cSeparatorChar).setQuoteChar (cQuoteChar).setEscapeChar (cEscapeChar);
  }

  /**
   * @return The CSV file in use. Never <code>null</code>.
   */
  @NonNull
  public final File getCSVFile ()
  {
    return m_aCSVFile;
  }

  @NonNull
  @VisibleForTesting
  static ICommonsList <String> asCSV (@NonNull final PeppolReportingItem aValue)
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

  @NonNull
  @VisibleForTesting
  static PeppolReportingItem asItem (@NonNull final ICommonsList <String> aValue)
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

  @Override
  protected void storeEligibleReportingItem (@NonNull @Nonempty final String sItemID,
                                             @NonNull final PeppolReportingItem aReportingItem) throws PeppolReportingBackendException
  {
    m_aRWLock.writeLockedThrowing ( () -> {
      m_aCSVWriter.writeNext (asCSV (aReportingItem));
      try
      {
        m_aCSVWriter.flush ();
      }
      catch (final IOException ex)
      {
        throw new PeppolReportingBackendException ("Failed to flush CSV file '" + m_aCSVFile.getAbsolutePath () + "'",
                                                   ex);
      }
    });
  }

  @Override
  protected void forEachReportingItemInRange (@NonNull final LocalDate aStartDateIncl,
                                              @NonNull final LocalDate aEndDateIncl,
                                              @NonNull final Consumer <? super PeppolReportingItem> aConsumer) throws PeppolReportingBackendException
  {
    int nCounter = 0;
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
          aConsumer.accept (asItem (aLine));
          ++nCounter;
        }
      }
    }
    catch (final IOException ex)
    {
      throw new PeppolReportingBackendException ("IO error in reading CSV '" + m_aCSVFile.getAbsolutePath () + "'",
                                                 ex);
    }

    if (LOGGER.isDebugEnabled ())
      LOGGER.debug ("Found a total of " +
                    nCounter +
                    " matching documents in CSV '" +
                    m_aCSVFile.getAbsolutePath () +
                    "'");
  }

  @Override
  protected void onClose ()
  {
    m_aRWLock.writeLocked ( () -> {
      try
      {
        m_aCSVWriter.close ();
      }
      catch (final IOException ex)
      {
        LOGGER.error ("Failed to close the CSV Writer", ex);
      }
    });
  }
}
