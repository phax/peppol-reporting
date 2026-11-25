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

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.annotation.style.IsSPIImplementation;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.string.StringHelper;
import com.helger.config.IConfig;
import com.helger.csv.CCSV;
import com.helger.io.file.FileHelper;
import com.helger.peppol.reporting.api.backend.IPeppolReportingBackendProviderSPI;
import com.helger.peppol.reporting.api.backend.PeppolReportingBackendException;

/**
 * SPI implementation of {@link IPeppolReportingBackendProviderSPI} that creates
 * {@link PeppolReportingBackendCSV} instances.
 *
 * @author Philip Helger
 */
@IsSPIImplementation
public class PeppolReportingBackendCSVSPI implements IPeppolReportingBackendProviderSPI
{
  public static final String BACKEND_ID = "csv";

  public static final String CONFIG_PEPPOL_REPORTING_CSV_FILENAME = "peppol.reporting.csv.filename";
  public static final String CONFIG_PEPPOL_REPORTING_CSV_SEPARATOR_CHAR = "peppol.reporting.csv.separator-char";
  public static final String CONFIG_PEPPOL_REPORTING_CSV_QUOTE_CHAR = "peppol.reporting.csv.quote-char";
  public static final String CONFIG_PEPPOL_REPORTING_CSV_ESCAPE_CHAR = "peppol.reporting.csv.escape-char";

  private static final Logger LOGGER = LoggerFactory.getLogger (PeppolReportingBackendCSVSPI.class);

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
    return PeppolReportingBackendCSV.DISPLAY_NAME;
  }

  private static char _asChar (@NonNull final IConfig aConfig, @NonNull final String sProperty, final char cDefault)
  {
    final String sValue = aConfig.getAsString (sProperty);
    if (StringHelper.isEmpty (sValue))
      return cDefault;
    if (sValue.length () > 1)
      LOGGER.warn ("The configuration property '" +
                   sProperty +
                   "' value length of " +
                   sValue.length () +
                   " is stripped to the first character");
    return sValue.charAt (0);
  }

  @NonNull
  public PeppolReportingBackendCSV createBackend (@NonNull final IConfig aConfig) throws PeppolReportingBackendException
  {
    ValueEnforcer.notNull (aConfig, "Config");

    final String sFilename = aConfig.getAsString (CONFIG_PEPPOL_REPORTING_CSV_FILENAME);
    if (StringHelper.isEmpty (sFilename))
      throw new PeppolReportingBackendException ("The CSV filename is missing in the configuration. See property '" +
                                                 CONFIG_PEPPOL_REPORTING_CSV_FILENAME +
                                                 "'");
    final File aFile = new File (sFilename);
    if (!FileHelper.canReadAndWriteFile (aFile))
      throw new PeppolReportingBackendException ("The CSV filename '" +
                                                 sFilename +
                                                 "' is missing the necessary access rights to read and write");

    return new PeppolReportingBackendCSV (aFile,
                                          _asChar (aConfig, CONFIG_PEPPOL_REPORTING_CSV_SEPARATOR_CHAR, CCSV.DEFAULT_SEPARATOR),
                                          _asChar (aConfig, CONFIG_PEPPOL_REPORTING_CSV_QUOTE_CHAR, CCSV.DEFAULT_QUOTE_CHARACTER),
                                          _asChar (aConfig, CONFIG_PEPPOL_REPORTING_CSV_ESCAPE_CHAR, CCSV.DEFAULT_ESCAPE_CHARACTER));
  }
}
