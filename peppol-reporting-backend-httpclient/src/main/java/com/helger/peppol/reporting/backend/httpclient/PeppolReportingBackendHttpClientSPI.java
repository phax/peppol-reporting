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
package com.helger.peppol.reporting.backend.httpclient;

import java.time.Duration;
import java.util.Locale;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.annotation.style.IsSPIImplementation;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.string.StringHelper;
import com.helger.config.IConfig;
import com.helger.config.fallback.IConfigWithFallback;
import com.helger.httpclient.HttpClientManager;
import com.helger.httpclient.HttpClientSettings;
import com.helger.httpclient.HttpClientSettingsConfig;
import com.helger.peppol.reporting.api.backend.IPeppolReportingBackendProviderSPI;
import com.helger.peppol.reporting.api.backend.PeppolReportingBackendException;
import com.helger.peppol.reporting.api.envelope.PeppolReportingEnvelope;

/**
 * SPI implementation of {@link IPeppolReportingBackendProviderSPI} that creates
 * {@link PeppolReportingBackendHttpClient} instances, which submit the reporting items to a remote
 * reporting collector.
 *
 * @author Philip Helger
 * @since 5.0.0
 */
@IsSPIImplementation
public class PeppolReportingBackendHttpClientSPI implements IPeppolReportingBackendProviderSPI
{
  public static final String BACKEND_ID = "httpclient";

  public static final String CONFIG_PREFIX = "peppol.reporting.httpclient.";
  public static final String CONFIG_PEPPOL_REPORTING_HTTPCLIENT_URL = CONFIG_PREFIX + "url";
  public static final String CONFIG_PEPPOL_REPORTING_HTTPCLIENT_TOKEN = CONFIG_PREFIX + "token";
  public static final String CONFIG_PEPPOL_REPORTING_HTTPCLIENT_SEATID = CONFIG_PREFIX + "seatid";
  public static final String CONFIG_PEPPOL_REPORTING_HTTPCLIENT_SOURCEID = CONFIG_PREFIX + "sourceid";
  public static final String CONFIG_PEPPOL_REPORTING_HTTPCLIENT_MAXBATCHSIZE = CONFIG_PREFIX + "maxbatchsize";
  public static final String CONFIG_PEPPOL_REPORTING_HTTPCLIENT_RETRY_MAXATTEMPTS = CONFIG_PREFIX + "retry.maxattempts";
  public static final String CONFIG_PEPPOL_REPORTING_HTTPCLIENT_RETRY_INITIALDELAY = CONFIG_PREFIX + "retry.initialdelay";
  public static final String CONFIG_PEPPOL_REPORTING_HTTPCLIENT_RETRY_MAXDELAY = CONFIG_PREFIX + "retry.maxdelay";

  public static final int DEFAULT_MAX_BATCH_SIZE = PeppolReportingEnvelope.MAX_ITEMS;
  public static final int DEFAULT_RETRY_MAX_ATTEMPTS = 5;
  public static final Duration DEFAULT_RETRY_INITIAL_DELAY = Duration.ofSeconds (1);
  public static final Duration DEFAULT_RETRY_MAX_DELAY = Duration.ofMinutes (1);

  private static final Logger LOGGER = LoggerFactory.getLogger (PeppolReportingBackendHttpClientSPI.class);

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
    return PeppolReportingBackendHttpClient.DISPLAY_NAME;
  }

  @NonNull
  @Nonempty
  private static String _getMandatoryString (@NonNull final IConfig aConfig,
                                             @NonNull final String sKey,
                                             final int nMaxLength) throws PeppolReportingBackendException
  {
    final String ret = aConfig.getAsString (sKey);
    if (StringHelper.isEmpty (ret))
      throw new PeppolReportingBackendException ("The configuration property '" + sKey + "' is missing");
    if (nMaxLength > 0 && ret.length () > nMaxLength)
      throw new PeppolReportingBackendException ("The value of the configuration property '" +
                                                 sKey +
                                                 "' exceeds the maximum length of " +
                                                 nMaxLength);
    return ret;
  }

  @NonNull
  private static Duration _getDuration (@NonNull final IConfig aConfig,
                                        @NonNull final String sKey,
                                        @NonNull final Duration aDefault)
  {
    final Duration ret = aConfig.getAsConfigDuration (sKey,
                                                      sErr -> LOGGER.warn ("Invalid duration value for configuration property '" +
                                                                           sKey +
                                                                           "': " +
                                                                           sErr));
    return ret != null ? ret : aDefault;
  }

  @NonNull
  public PeppolReportingBackendHttpClient createBackend (@NonNull final IConfig aConfig) throws PeppolReportingBackendException
  {
    ValueEnforcer.notNull (aConfig, "Config");

    final String sURL = _getMandatoryString (aConfig, CONFIG_PEPPOL_REPORTING_HTTPCLIENT_URL, 0);
    final String sLowerURL = sURL.toLowerCase (Locale.ROOT);
    if (!sLowerURL.startsWith ("https://"))
    {
      if (!sLowerURL.startsWith ("http://"))
        throw new PeppolReportingBackendException ("The configuration property '" +
                                                   CONFIG_PEPPOL_REPORTING_HTTPCLIENT_URL +
                                                   "' must contain an http or https URL, but is '" +
                                                   sURL +
                                                   "'");
      LOGGER.warn ("The remote Peppol Reporting collector URL '" +
                   sURL +
                   "' does not use https - the authentication token is transmitted unencrypted");
    }

    final String sToken = _getMandatoryString (aConfig, CONFIG_PEPPOL_REPORTING_HTTPCLIENT_TOKEN, 0);
    final String sSeatID = _getMandatoryString (aConfig,
                                                CONFIG_PEPPOL_REPORTING_HTTPCLIENT_SEATID,
                                                PeppolReportingEnvelope.MAX_LEN_SEAT_ID);
    final String sSourceID = _getMandatoryString (aConfig,
                                                  CONFIG_PEPPOL_REPORTING_HTTPCLIENT_SOURCEID,
                                                  PeppolReportingEnvelope.MAX_LEN_SOURCE_ID);

    final int nMaxBatchSize = aConfig.getAsInt (CONFIG_PEPPOL_REPORTING_HTTPCLIENT_MAXBATCHSIZE, DEFAULT_MAX_BATCH_SIZE);
    if (nMaxBatchSize < 1 || nMaxBatchSize > PeppolReportingEnvelope.MAX_ITEMS)
      throw new PeppolReportingBackendException ("The configuration property '" +
                                                 CONFIG_PEPPOL_REPORTING_HTTPCLIENT_MAXBATCHSIZE +
                                                 "' must be between 1 and " +
                                                 PeppolReportingEnvelope.MAX_ITEMS +
                                                 ", but is " +
                                                 nMaxBatchSize);

    final int nRetryMaxAttempts = aConfig.getAsInt (CONFIG_PEPPOL_REPORTING_HTTPCLIENT_RETRY_MAXATTEMPTS,
                                                    DEFAULT_RETRY_MAX_ATTEMPTS);
    if (nRetryMaxAttempts < 1)
      throw new PeppolReportingBackendException ("The configuration property '" +
                                                 CONFIG_PEPPOL_REPORTING_HTTPCLIENT_RETRY_MAXATTEMPTS +
                                                 "' must be at least 1, but is " +
                                                 nRetryMaxAttempts);

    final Duration aRetryInitialDelay = _getDuration (aConfig,
                                                      CONFIG_PEPPOL_REPORTING_HTTPCLIENT_RETRY_INITIALDELAY,
                                                      DEFAULT_RETRY_INITIAL_DELAY);
    final Duration aRetryMaxDelay = _getDuration (aConfig,
                                                  CONFIG_PEPPOL_REPORTING_HTTPCLIENT_RETRY_MAXDELAY,
                                                  DEFAULT_RETRY_MAX_DELAY);
    if (aRetryMaxDelay.compareTo (aRetryInitialDelay) < 0)
      throw new PeppolReportingBackendException ("The configuration property '" +
                                                 CONFIG_PEPPOL_REPORTING_HTTPCLIENT_RETRY_MAXDELAY +
                                                 "' (" +
                                                 aRetryMaxDelay +
                                                 ") must not be smaller than '" +
                                                 CONFIG_PEPPOL_REPORTING_HTTPCLIENT_RETRY_INITIALDELAY +
                                                 "' (" +
                                                 aRetryInitialDelay +
                                                 ")");

    // Proxy, timeouts, TLS etc. use the same prefix
    final HttpClientSettings aHCS = new HttpClientSettings ();
    if (aConfig instanceof final IConfigWithFallback aConfigWithFallback)
      HttpClientSettingsConfig.assignConfigValues (aHCS, aConfigWithFallback, CONFIG_PREFIX);
    else
      LOGGER.info ("The provided configuration does not support fallbacks - the HTTP client settings are not read from it");

    LOGGER.info ("Peppol Reporting HttpClient backend submits reporting items for Seat ID '" +
                 sSeatID +
                 "' and source ID '" +
                 sSourceID +
                 "' to '" +
                 sURL +
                 "'");
    return new PeppolReportingBackendHttpClient (HttpClientManager.create (aHCS),
                                                 sURL,
                                                 sToken,
                                                 sSeatID,
                                                 sSourceID,
                                                 nMaxBatchSize,
                                                 nRetryMaxAttempts,
                                                 aRetryInitialDelay,
                                                 aRetryMaxDelay);
  }
}
