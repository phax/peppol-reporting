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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.annotation.Nonnegative;
import com.helger.annotation.concurrent.GuardedBy;
import com.helger.annotation.concurrent.Immutable;
import com.helger.annotation.style.IsSPIImplementation;
import com.helger.annotation.style.VisibleForTesting;
import com.helger.base.concurrent.SimpleReadWriteLock;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.state.ESuccess;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsHashSet;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsSet;
import com.helger.config.IConfig;
import com.helger.config.fallback.IConfigWithFallback;
import com.helger.httpclient.HttpClientHelper;
import com.helger.httpclient.HttpClientManager;
import com.helger.httpclient.HttpClientSettings;
import com.helger.httpclient.HttpClientSettingsConfig;
import com.helger.peppol.reporting.api.PeppolReportingHelper;
import com.helger.peppol.reporting.api.PeppolReportingItem;
import com.helger.peppol.reporting.api.PeppolReportingItemWithID;
import com.helger.peppol.reporting.api.backend.IPeppolReportingBackendSPI;
import com.helger.peppol.reporting.api.backend.PeppolReportingBackendException;
import com.helger.peppol.reporting.api.envelope.PeppolReportingEnvelope;
import com.helger.peppol.reporting.api.envelope.PeppolReportingEnvelopeJsonHelper;
import com.helger.peppol.reporting.api.envelope.PeppolReportingSubmissionResponse;
import com.helger.peppol.reporting.api.envelope.PeppolReportingSubmissionResponseJsonHelper;
import com.helger.peppolid.CIdentifier;

/**
 * SPI implementation of {@link IPeppolReportingBackendSPI} that does not store the reporting items
 * locally, but submits them via HTTP POST to a remote reporting collector, which creates and sends
 * the reports. The items are transferred as {@link PeppolReportingEnvelope} in batches of at most
 * {@link PeppolReportingEnvelope#MAX_ITEMS} items. The request is authenticated with the HTTP
 * header {@value #HTTP_HEADER_TOKEN}.<br>
 * Each reporting item is submitted with its item ID, so that the collector can ignore items it
 * already stored. Callers should use {@link #storeReportingItem(String, PeppolReportingItem)} or
 * {@link #storeReportingItems(List)} with stable item IDs, because
 * {@link #storeReportingItem(PeppolReportingItem)} uses a new random UUID on every call. Reading
 * reporting items is not supported.<br>
 * Failed submissions (network errors, HTTP status codes 408, 429 and 5xx) are retried with
 * exponential back-off and jitter. If all attempts fail, a {@link PeppolReportingBackendException}
 * is thrown, so that the caller can keep the items and submit them again later. If the collector
 * rejects items, a {@link PeppolReportingSubmissionRejectedException} is thrown.
 *
 * @author Philip Helger
 * @since 4.6.0
 */
@IsSPIImplementation
public class PeppolReportingBackendHttpClientSPI implements IPeppolReportingBackendSPI
{
  public static final String CONFIG_PREFIX = "peppol.reporting.httpclient.";
  public static final String CONFIG_PEPPOL_REPORTING_HTTPCLIENT_URL = CONFIG_PREFIX + "url";
  public static final String CONFIG_PEPPOL_REPORTING_HTTPCLIENT_TOKEN = CONFIG_PREFIX + "token";
  public static final String CONFIG_PEPPOL_REPORTING_HTTPCLIENT_SEATID = CONFIG_PREFIX + "seatid";
  public static final String CONFIG_PEPPOL_REPORTING_HTTPCLIENT_SOURCEID = CONFIG_PREFIX + "sourceid";
  public static final String CONFIG_PEPPOL_REPORTING_HTTPCLIENT_MAXBATCHSIZE = CONFIG_PREFIX + "maxbatchsize";
  public static final String CONFIG_PEPPOL_REPORTING_HTTPCLIENT_RETRY_MAXATTEMPTS = CONFIG_PREFIX + "retry.maxattempts";
  public static final String CONFIG_PEPPOL_REPORTING_HTTPCLIENT_RETRY_INITIALDELAY = CONFIG_PREFIX + "retry.initialdelay";
  public static final String CONFIG_PEPPOL_REPORTING_HTTPCLIENT_RETRY_MAXDELAY = CONFIG_PREFIX + "retry.maxdelay";

  /** The HTTP header containing the authentication token */
  public static final String HTTP_HEADER_TOKEN = "X-Token";

  public static final int DEFAULT_MAX_BATCH_SIZE = PeppolReportingEnvelope.MAX_ITEMS;
  public static final int DEFAULT_RETRY_MAX_ATTEMPTS = 5;
  public static final Duration DEFAULT_RETRY_INITIAL_DELAY = Duration.ofSeconds (1);
  public static final Duration DEFAULT_RETRY_MAX_DELAY = Duration.ofMinutes (1);

  private static final Logger LOGGER = LoggerFactory.getLogger (PeppolReportingBackendHttpClientSPI.class);
  private static final int MAX_BODY_LEN_IN_MESSAGE = 1000;

  /**
   * The settings and the HTTP client of an initialised backend.
   */
  @Immutable
  private static final class RemoteState
  {
    private final HttpClientManager m_aHttpClientMgr;
    private final String m_sURL;
    private final String m_sToken;
    private final String m_sSeatID;
    private final String m_sSourceID;
    private final int m_nMaxBatchSize;
    private final int m_nRetryMaxAttempts;
    private final Duration m_aRetryInitialDelay;
    private final Duration m_aRetryMaxDelay;

    RemoteState (@NonNull final HttpClientManager aHttpClientMgr,
                 @NonNull final String sURL,
                 @NonNull final String sToken,
                 @NonNull final String sSeatID,
                 @NonNull final String sSourceID,
                 final int nMaxBatchSize,
                 final int nRetryMaxAttempts,
                 @NonNull final Duration aRetryInitialDelay,
                 @NonNull final Duration aRetryMaxDelay)
    {
      m_aHttpClientMgr = aHttpClientMgr;
      m_sURL = sURL;
      m_sToken = sToken;
      m_sSeatID = sSeatID;
      m_sSourceID = sSourceID;
      m_nMaxBatchSize = nMaxBatchSize;
      m_nRetryMaxAttempts = nRetryMaxAttempts;
      m_aRetryInitialDelay = aRetryInitialDelay;
      m_aRetryMaxDelay = aRetryMaxDelay;
    }
  }

  /**
   * A failed submission attempt that may be retried.
   */
  @Immutable
  private static final class RetryableFailure
  {
    private final String m_sMessage;
    private final Exception m_aCause;

    RetryableFailure (@NonNull final String sMessage, @Nullable final Exception aCause)
    {
      m_sMessage = sMessage;
      m_aCause = aCause;
    }
  }

  /**
   * The status code and the body of an HTTP response.
   */
  @Immutable
  private static final class HttpResult
  {
    private final int m_nStatusCode;
    private final String m_sBody;

    HttpResult (final int nStatusCode, @Nullable final String sBody)
    {
      m_nStatusCode = nStatusCode;
      m_sBody = sBody;
    }
  }

  private final SimpleReadWriteLock m_aRWLock = new SimpleReadWriteLock ();
  @GuardedBy ("m_aRWLock")
  private RemoteState m_aState;

  @NonNull
  @Nonempty
  public String getDisplayName ()
  {
    return "HttpClient";
  }

  @Nullable
  private static String _getMandatoryString (@NonNull final IConfig aConfig,
                                             @NonNull final String sKey,
                                             final int nMaxLength)
  {
    final String ret = aConfig.getAsString (sKey);
    if (StringHelper.isEmpty (ret))
    {
      LOGGER.error ("The configuration property '" + sKey + "' is missing");
      return null;
    }
    if (nMaxLength > 0 && ret.length () > nMaxLength)
    {
      LOGGER.error ("The value of the configuration property '" +
                    sKey +
                    "' exceeds the maximum length of " +
                    nMaxLength);
      return null;
    }
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
  public ESuccess initBackend (@NonNull final IConfig aConfig)
  {
    ValueEnforcer.notNull (aConfig, "Config");

    final String sURL = _getMandatoryString (aConfig, CONFIG_PEPPOL_REPORTING_HTTPCLIENT_URL, 0);
    if (sURL == null)
      return ESuccess.FAILURE;
    final String sLowerURL = sURL.toLowerCase (Locale.ROOT);
    if (!sLowerURL.startsWith ("https://"))
    {
      if (!sLowerURL.startsWith ("http://"))
      {
        LOGGER.error ("The configuration property '" +
                      CONFIG_PEPPOL_REPORTING_HTTPCLIENT_URL +
                      "' must contain an http or https URL, but is '" +
                      sURL +
                      "'");
        return ESuccess.FAILURE;
      }
      LOGGER.warn ("The remote Peppol Reporting collector URL '" +
                   sURL +
                   "' does not use https - the authentication token is transmitted unencrypted");
    }

    final String sToken = _getMandatoryString (aConfig, CONFIG_PEPPOL_REPORTING_HTTPCLIENT_TOKEN, 0);
    if (sToken == null)
      return ESuccess.FAILURE;

    final String sSeatID = _getMandatoryString (aConfig,
                                                CONFIG_PEPPOL_REPORTING_HTTPCLIENT_SEATID,
                                                PeppolReportingEnvelope.MAX_LEN_SEAT_ID);
    if (sSeatID == null)
      return ESuccess.FAILURE;

    final String sSourceID = _getMandatoryString (aConfig,
                                                  CONFIG_PEPPOL_REPORTING_HTTPCLIENT_SOURCEID,
                                                  PeppolReportingEnvelope.MAX_LEN_SOURCE_ID);
    if (sSourceID == null)
      return ESuccess.FAILURE;

    final int nMaxBatchSize = aConfig.getAsInt (CONFIG_PEPPOL_REPORTING_HTTPCLIENT_MAXBATCHSIZE, DEFAULT_MAX_BATCH_SIZE);
    if (nMaxBatchSize < 1 || nMaxBatchSize > PeppolReportingEnvelope.MAX_ITEMS)
    {
      LOGGER.error ("The configuration property '" +
                    CONFIG_PEPPOL_REPORTING_HTTPCLIENT_MAXBATCHSIZE +
                    "' must be between 1 and " +
                    PeppolReportingEnvelope.MAX_ITEMS +
                    ", but is " +
                    nMaxBatchSize);
      return ESuccess.FAILURE;
    }

    final int nRetryMaxAttempts = aConfig.getAsInt (CONFIG_PEPPOL_REPORTING_HTTPCLIENT_RETRY_MAXATTEMPTS,
                                                    DEFAULT_RETRY_MAX_ATTEMPTS);
    if (nRetryMaxAttempts < 1)
    {
      LOGGER.error ("The configuration property '" +
                    CONFIG_PEPPOL_REPORTING_HTTPCLIENT_RETRY_MAXATTEMPTS +
                    "' must be at least 1, but is " +
                    nRetryMaxAttempts);
      return ESuccess.FAILURE;
    }

    final Duration aRetryInitialDelay = _getDuration (aConfig,
                                                      CONFIG_PEPPOL_REPORTING_HTTPCLIENT_RETRY_INITIALDELAY,
                                                      DEFAULT_RETRY_INITIAL_DELAY);
    final Duration aRetryMaxDelay = _getDuration (aConfig,
                                                  CONFIG_PEPPOL_REPORTING_HTTPCLIENT_RETRY_MAXDELAY,
                                                  DEFAULT_RETRY_MAX_DELAY);
    if (aRetryMaxDelay.compareTo (aRetryInitialDelay) < 0)
    {
      LOGGER.error ("The configuration property '" +
                    CONFIG_PEPPOL_REPORTING_HTTPCLIENT_RETRY_MAXDELAY +
                    "' (" +
                    aRetryMaxDelay +
                    ") must not be smaller than '" +
                    CONFIG_PEPPOL_REPORTING_HTTPCLIENT_RETRY_INITIALDELAY +
                    "' (" +
                    aRetryInitialDelay +
                    ")");
      return ESuccess.FAILURE;
    }

    // Proxy, timeouts, TLS etc. use the same prefix
    final HttpClientSettings aHCS = new HttpClientSettings ();
    if (aConfig instanceof final IConfigWithFallback aConfigWithFallback)
      HttpClientSettingsConfig.assignConfigValues (aHCS, aConfigWithFallback, CONFIG_PREFIX);
    else
      LOGGER.info ("The provided configuration does not support fallbacks - the HTTP client settings are not read from it");

    final RemoteState aNewState = new RemoteState (HttpClientManager.create (aHCS),
                                                   sURL,
                                                   sToken,
                                                   sSeatID,
                                                   sSourceID,
                                                   nMaxBatchSize,
                                                   nRetryMaxAttempts,
                                                   aRetryInitialDelay,
                                                   aRetryMaxDelay);
    final RemoteState aOldState = m_aRWLock.writeLockedGet (() -> {
      final RemoteState ret = m_aState;
      m_aState = aNewState;
      return ret;
    });
    if (aOldState != null)
    {
      LOGGER.warn ("The Peppol Reporting HttpClient backend was already initialized - replacing the previous settings");
      aOldState.m_aHttpClientMgr.close ();
    }

    LOGGER.info ("Peppol Reporting HttpClient backend submits reporting items for Seat ID '" +
                 sSeatID +
                 "' and source ID '" +
                 sSourceID +
                 "' to '" +
                 sURL +
                 "'");
    return ESuccess.SUCCESS;
  }

  public boolean isInitialized ()
  {
    return m_aRWLock.readLockedBoolean (() -> m_aState != null);
  }

  public void shutdownBackend ()
  {
    final RemoteState aOldState = m_aRWLock.writeLockedGet (() -> {
      final RemoteState ret = m_aState;
      m_aState = null;
      return ret;
    });
    if (aOldState != null)
      aOldState.m_aHttpClientMgr.close ();
  }

  @NonNull
  private RemoteState _getState ()
  {
    final RemoteState ret = m_aRWLock.readLockedGet (() -> m_aState);
    if (ret == null)
      throw new IllegalStateException ("The Peppol Reporting HttpClient backend is not initialized");
    return ret;
  }

  /**
   * Get the delay before the next retry, using exponential back-off with "equal jitter": the delay
   * is doubled for every retry, starting at the initial delay and capped at the maximum delay, and
   * a random value between half and the full delay is used.
   *
   * @param nRetryIndex
   *        The 0-based index of the retry. Must be &ge; 0.
   * @param aInitialDelay
   *        The delay of the first retry. May not be <code>null</code>.
   * @param aMaxDelay
   *        The maximum delay. May not be <code>null</code>.
   * @return The delay to wait. Never <code>null</code>.
   */
  @VisibleForTesting
  @NonNull
  static Duration getBackOffDelay (@Nonnegative final int nRetryIndex,
                                   @NonNull final Duration aInitialDelay,
                                   @NonNull final Duration aMaxDelay)
  {
    final long nMaxMillis = aMaxDelay.toMillis ();
    long nCapMillis = aInitialDelay.toMillis ();
    for (int i = 0; i < nRetryIndex && nCapMillis > 0 && nCapMillis < nMaxMillis; ++i)
      nCapMillis *= 2;
    nCapMillis = Math.min (nCapMillis, nMaxMillis);

    final long nHalfMillis = nCapMillis / 2;
    return Duration.ofMillis (nHalfMillis + ThreadLocalRandom.current ().nextLong (nCapMillis - nHalfMillis + 1));
  }

  @NonNull
  private static String _getBodyForMessage (@Nullable final String sBody)
  {
    if (StringHelper.isEmpty (sBody))
      return "<no body>";
    if (sBody.length () > MAX_BODY_LEN_IN_MESSAGE)
      return sBody.substring (0, MAX_BODY_LEN_IN_MESSAGE) + "...";
    return sBody;
  }

  @Nullable
  private static PeppolReportingSubmissionResponse _parseResponse (@Nullable final String sBody)
  {
    if (StringHelper.isEmpty (sBody))
      return null;
    try
    {
      final PeppolReportingSubmissionResponse ret = PeppolReportingSubmissionResponseJsonHelper.toDomain (sBody);
      if (ret == null)
        LOGGER.warn ("The response of the remote Peppol Reporting collector is not a JSON object");
      return ret;
    }
    catch (final IllegalArgumentException ex)
    {
      LOGGER.warn ("Failed to interpret the response of the remote Peppol Reporting collector: " + ex.getMessage ());
      return null;
    }
  }

  @NonNull
  private static PeppolReportingSubmissionRejectedException _createRejectedException (@NonNull final PeppolReportingEnvelope aEnvelope,
                                                                                      @NonNull final PeppolReportingSubmissionResponse aResponse)
  {
    return new PeppolReportingSubmissionRejectedException ("The remote Peppol Reporting collector rejected " +
                                                           aResponse.getAllRejectedItems ().size () +
                                                           " of " +
                                                           aEnvelope.getItemCount () +
                                                           " reporting items - none of them were stored",
                                                           aResponse.getAllRejectedItems ());
  }

  /**
   * Submit an envelope once.
   *
   * @return <code>null</code> if the submission was successful, a non-<code>null</code> failure if
   *         the submission may be retried.
   * @throws PeppolReportingBackendException
   *         if the submission failed and must not be retried
   */
  @Nullable
  private static RetryableFailure _sendOnce (@NonNull final RemoteState aState,
                                             @NonNull final PeppolReportingEnvelope aEnvelope,
                                             @NonNull final String sPayload) throws PeppolReportingBackendException
  {
    final HttpPost aPost = new HttpPost (aState.m_sURL);
    aPost.setHeader (HTTP_HEADER_TOKEN, aState.m_sToken);
    aPost.setEntity (new StringEntity (sPayload, ContentType.APPLICATION_JSON));

    final HttpResult aResult;
    try
    {
      aResult = aState.m_aHttpClientMgr.execute (aPost, aHttpResponse -> {
        final HttpEntity aEntity = aHttpResponse.getEntity ();
        String sBody = null;
        if (aEntity != null)
        {
          final ContentType aContentType = HttpClientHelper.getContentTypeOrDefault (aEntity,
                                                                                     ContentType.APPLICATION_JSON);
          sBody = HttpClientHelper.entityToString (aEntity,
                                                   HttpClientHelper.getCharset (aContentType, StandardCharsets.UTF_8));
        }
        return new HttpResult (aHttpResponse.getCode (), sBody);
      });
    }
    catch (final IOException ex)
    {
      return new RetryableFailure ("Failed to submit " +
                                   aEnvelope.getItemCount () +
                                   " reporting items to the remote Peppol Reporting collector '" +
                                   aState.m_sURL +
                                   "'",
                                   ex);
    }

    final int nStatusCode = aResult.m_nStatusCode;
    if (nStatusCode >= 200 && nStatusCode < 300)
    {
      final PeppolReportingSubmissionResponse aResponse = _parseResponse (aResult.m_sBody);
      if (aResponse != null && !aResponse.isAccepted ())
        throw _createRejectedException (aEnvelope, aResponse);

      if (aResponse != null)
        LOGGER.info ("The remote Peppol Reporting collector accepted " +
                     aEnvelope.getItemCount () +
                     " reporting items (" +
                     aResponse.getAcceptedCount () +
                     " new, " +
                     aResponse.getDuplicateCount () +
                     " duplicates)");
      else
        LOGGER.info ("The remote Peppol Reporting collector accepted " +
                     aEnvelope.getItemCount () +
                     " reporting items");
      return null;
    }

    if (nStatusCode == 400)
    {
      final PeppolReportingSubmissionResponse aResponse = _parseResponse (aResult.m_sBody);
      if (aResponse != null && !aResponse.isAccepted ())
        throw _createRejectedException (aEnvelope, aResponse);

      throw new PeppolReportingBackendException ("The remote Peppol Reporting collector rejected the submission with HTTP status code 400: " +
                                                 _getBodyForMessage (aResult.m_sBody));
    }

    if (nStatusCode == 408 || nStatusCode == 429 || nStatusCode >= 500)
      return new RetryableFailure ("The remote Peppol Reporting collector '" +
                                   aState.m_sURL +
                                   "' responded with HTTP status code " +
                                   nStatusCode,
                                   null);

    throw new PeppolReportingBackendException ("The remote Peppol Reporting collector '" +
                                               aState.m_sURL +
                                               "' responded with the unexpected HTTP status code " +
                                               nStatusCode +
                                               ": " +
                                               _getBodyForMessage (aResult.m_sBody));
  }

  private static void _sendWithRetry (@NonNull final RemoteState aState,
                                      @NonNull final PeppolReportingEnvelope aEnvelope) throws PeppolReportingBackendException
  {
    final String sPayload = PeppolReportingEnvelopeJsonHelper.toJsonString (aEnvelope);

    RetryableFailure aLastFailure = null;
    for (int nAttempt = 1; nAttempt <= aState.m_nRetryMaxAttempts; ++nAttempt)
    {
      if (aLastFailure != null)
      {
        final Duration aDelay = getBackOffDelay (nAttempt - 2, aState.m_aRetryInitialDelay, aState.m_aRetryMaxDelay);
        LOGGER.warn (aLastFailure.m_sMessage +
                     " - retrying in " +
                     aDelay.toMillis () +
                     " ms (attempt " +
                     nAttempt +
                     " of " +
                     aState.m_nRetryMaxAttempts +
                     ")");
        try
        {
          Thread.sleep (aDelay.toMillis ());
        }
        catch (final InterruptedException ex)
        {
          Thread.currentThread ().interrupt ();
          throw new PeppolReportingBackendException ("Interrupted while waiting to retry the submission to the remote Peppol Reporting collector",
                                                     ex);
        }
      }

      aLastFailure = _sendOnce (aState, aEnvelope, sPayload);
      if (aLastFailure == null)
        return;
    }

    final String sMsg = aLastFailure.m_sMessage + " - giving up after " + aState.m_nRetryMaxAttempts + " attempts";
    if (aLastFailure.m_aCause != null)
      throw new PeppolReportingBackendException (sMsg, aLastFailure.m_aCause);
    throw new PeppolReportingBackendException (sMsg);
  }

  public void storeReportingItem (@NonNull @Nonempty final String sItemID,
                                  @NonNull final PeppolReportingItem aReportingItem) throws PeppolReportingBackendException
  {
    storeReportingItems (new CommonsArrayList <> (new PeppolReportingItemWithID (sItemID, aReportingItem)));
  }

  /**
   * Submit the reporting items to the remote collector. Items with document types that are not
   * eligible for reporting are skipped. All other items must belong to the configured Seat ID
   * (compared case-insensitively). The items are submitted in batches of the configured maximum
   * batch size. If an exception is thrown, the batches submitted before were already stored by the
   * collector - as the collector ignores duplicates, all items can be submitted again.
   *
   * @throws PeppolReportingSubmissionRejectedException
   *         if the collector rejected invalid items
   * @throws PeppolReportingBackendException
   *         if the submission failed
   */
  @Override
  public void storeReportingItems (@NonNull final List <PeppolReportingItemWithID> aReportingItems) throws PeppolReportingBackendException
  {
    ValueEnforcer.notNullNoNullValue (aReportingItems, "ReportingItems");

    final RemoteState aState = _getState ();

    // First check if all items are eligible before processing any of them
    // Either all or none
    final ICommonsList <PeppolReportingItemWithID> aEligibleItems = new CommonsArrayList <> ();
    final ICommonsSet <String> aUniqueID = new CommonsHashSet <> ();
    for (final PeppolReportingItemWithID aItemWithID : aReportingItems)
    {
      final PeppolReportingItem aItem = aItemWithID.getReportingItem ();
      if (!PeppolReportingHelper.isDocumentTypeEligableForReporting (aItem.getDocTypeIDScheme (),
                                                                     aItem.getDocTypeIDValue ()))
      {
        LOGGER.info ("Not submitting Peppol Reporting Item '" +
                     aItemWithID.getItemID () +
                     "', as the document type is not eligible for reporting (" +
                     CIdentifier.getURIEncoded (aItem.getDocTypeIDScheme (), aItem.getDocTypeIDValue ()) +
                     ")");
        continue;
      }

      if (!aItem.getReportingServiceProviderID ().equalsIgnoreCase (aState.m_sSeatID))
        throw new PeppolReportingBackendException ("The Peppol Reporting Item '" +
                                                   aItemWithID.getItemID () +
                                                   "' belongs to the reporting service provider '" +
                                                   aItem.getReportingServiceProviderID () +
                                                   "' but the backend is configured for the Seat ID '" +
                                                   aState.m_sSeatID +
                                                   "'");

      if (!aUniqueID.add (aItemWithID.getItemID ()))
        throw new PeppolReportingBackendException ("The Peppol Reporting Item '" +
                                                   aItemWithID.getItemID () +
                                                   "' is used more then once in the provided batch");

      aEligibleItems.add (aItemWithID);
    }

    // Process them
    final int nCount = aEligibleItems.size ();
    for (int nStart = 0; nStart < nCount; nStart += aState.m_nMaxBatchSize)
    {
      final List <PeppolReportingItemWithID> aBatch = aEligibleItems.subList (nStart,
                                                                              Math.min (nStart + aState.m_nMaxBatchSize,
                                                                                        nCount));
      final PeppolReportingEnvelope aEnvelope;
      try
      {
        aEnvelope = new PeppolReportingEnvelope (aState.m_sSeatID, aState.m_sSourceID, aBatch);
      }
      catch (final IllegalArgumentException ex)
      {
        throw new PeppolReportingBackendException ("Failed to create the Peppol Reporting envelope", ex);
      }

      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Submitting " + aEnvelope.getItemCount () + " Peppol Reporting Items to '" + aState.m_sURL + "'");

      _sendWithRetry (aState, aEnvelope);
    }
  }

  /**
   * Always throws an exception, because the reporting items are stored by the remote collector,
   * which also creates the reports.
   *
   * @throws PeppolReportingBackendException
   *         always
   */
  @NonNull
  public Iterable <PeppolReportingItem> iterateReportingItems (@NonNull final LocalDate aStartDateIncl,
                                                               @NonNull final LocalDate aEndDateIncl) throws PeppolReportingBackendException
  {
    throw new PeppolReportingBackendException ("The Peppol Reporting HttpClient backend does not support reading reporting items - the reports are created by the remote collector");
  }
}
