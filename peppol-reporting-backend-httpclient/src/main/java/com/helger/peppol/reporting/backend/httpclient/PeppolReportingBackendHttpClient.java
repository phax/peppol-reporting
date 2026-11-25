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
import java.util.List;
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
import com.helger.annotation.concurrent.Immutable;
import com.helger.annotation.concurrent.ThreadSafe;
import com.helger.annotation.style.VisibleForTesting;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsHashSet;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsSet;
import com.helger.httpclient.HttpClientHelper;
import com.helger.httpclient.HttpClientManager;
import com.helger.peppol.reporting.api.PeppolReportingHelper;
import com.helger.peppol.reporting.api.PeppolReportingItem;
import com.helger.peppol.reporting.api.PeppolReportingItemWithID;
import com.helger.peppol.reporting.api.backend.AbstractPeppolReportingBackend;
import com.helger.peppol.reporting.api.backend.PeppolReportingBackendException;
import com.helger.peppol.reporting.api.envelope.PeppolReportingEnvelope;
import com.helger.peppol.reporting.api.envelope.PeppolReportingEnvelopeJsonHelper;
import com.helger.peppol.reporting.api.envelope.PeppolReportingSubmissionResponse;
import com.helger.peppol.reporting.api.envelope.PeppolReportingSubmissionResponseJsonHelper;
import com.helger.peppolid.CIdentifier;

/**
 * Peppol Reporting backend that does not store the reporting items locally, but submits them via
 * HTTP POST to a remote reporting collector, which creates and sends the reports. The items are
 * transferred as {@link PeppolReportingEnvelope} in batches of at most
 * {@link PeppolReportingEnvelope#MAX_ITEMS} items. The request is authenticated with the HTTP
 * header {@value #HTTP_HEADER_TOKEN}.<br>
 * Each reporting item is submitted with its item ID, so that the collector can ignore items it
 * already stored. This backend does not implement
 * {@link com.helger.peppol.reporting.api.backend.IPeppolReportingItemReader}, because the reports
 * are created by the collector.<br>
 * Failed submissions (network errors, HTTP status codes 408, 429 and 5xx) are retried with
 * exponential back-off and jitter. If all attempts fail, a {@link PeppolReportingBackendException}
 * is thrown, so that the caller can keep the items and submit them again later. If the collector
 * rejects items, a {@link PeppolReportingSubmissionRejectedException} is thrown.
 *
 * @author Philip Helger
 * @since 5.0.0
 */
@ThreadSafe
public class PeppolReportingBackendHttpClient extends AbstractPeppolReportingBackend
{
  public static final String DISPLAY_NAME = "HttpClient";

  /** The HTTP header containing the authentication token */
  public static final String HTTP_HEADER_TOKEN = "X-Token";

  private static final Logger LOGGER = LoggerFactory.getLogger (PeppolReportingBackendHttpClient.class);
  private static final int MAX_BODY_LEN_IN_MESSAGE = 1000;

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

  private final HttpClientManager m_aHttpClientMgr;
  private final String m_sURL;
  private final String m_sToken;
  private final String m_sSeatID;
  private final String m_sSourceID;
  private final int m_nMaxBatchSize;
  private final int m_nRetryMaxAttempts;
  private final Duration m_aRetryInitialDelay;
  private final Duration m_aRetryMaxDelay;

  /**
   * Constructor. All values must already be validated - use
   * {@link PeppolReportingBackendHttpClientSPI#createBackend(com.helger.config.IConfig)} to create
   * an instance from the configuration.
   */
  PeppolReportingBackendHttpClient (@NonNull final HttpClientManager aHttpClientMgr,
                                    @NonNull @Nonempty final String sURL,
                                    @NonNull @Nonempty final String sToken,
                                    @NonNull @Nonempty final String sSeatID,
                                    @NonNull @Nonempty final String sSourceID,
                                    final int nMaxBatchSize,
                                    final int nRetryMaxAttempts,
                                    @NonNull final Duration aRetryInitialDelay,
                                    @NonNull final Duration aRetryMaxDelay)
  {
    super (DISPLAY_NAME);
    ValueEnforcer.notNull (aHttpClientMgr, "HttpClientMgr");
    ValueEnforcer.notEmpty (sURL, "URL");
    ValueEnforcer.notEmpty (sToken, "Token");
    ValueEnforcer.notEmpty (sSeatID, "SeatID");
    ValueEnforcer.notEmpty (sSourceID, "SourceID");
    ValueEnforcer.isBetweenInclusive (nMaxBatchSize, "MaxBatchSize", 1, PeppolReportingEnvelope.MAX_ITEMS);
    ValueEnforcer.isGT0 (nRetryMaxAttempts, "RetryMaxAttempts");
    ValueEnforcer.notNull (aRetryInitialDelay, "RetryInitialDelay");
    ValueEnforcer.notNull (aRetryMaxDelay, "RetryMaxDelay");
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

  /**
   * @return The URL of the collector endpoint. Neither <code>null</code> nor empty.
   */
  @NonNull
  @Nonempty
  public final String getURL ()
  {
    return m_sURL;
  }

  /**
   * @return The Seat ID of the reporting service provider. Neither <code>null</code> nor empty.
   */
  @NonNull
  @Nonempty
  public final String getSeatID ()
  {
    return m_sSeatID;
  }

  /**
   * @return The ID of this Access Point deployment. Neither <code>null</code> nor empty.
   */
  @NonNull
  @Nonempty
  public final String getSourceID ()
  {
    return m_sSourceID;
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
  private RetryableFailure _sendOnce (@NonNull final PeppolReportingEnvelope aEnvelope,
                                      @NonNull final String sPayload) throws PeppolReportingBackendException
  {
    final HttpPost aPost = new HttpPost (m_sURL);
    aPost.setHeader (HTTP_HEADER_TOKEN, m_sToken);
    aPost.setEntity (new StringEntity (sPayload, ContentType.APPLICATION_JSON));

    final HttpResult aResult;
    try
    {
      aResult = m_aHttpClientMgr.execute (aPost, aHttpResponse -> {
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
                                   m_sURL +
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
                                   m_sURL +
                                   "' responded with HTTP status code " +
                                   nStatusCode,
                                   null);

    throw new PeppolReportingBackendException ("The remote Peppol Reporting collector '" +
                                               m_sURL +
                                               "' responded with the unexpected HTTP status code " +
                                               nStatusCode +
                                               ": " +
                                               _getBodyForMessage (aResult.m_sBody));
  }

  private void _sendWithRetry (@NonNull final PeppolReportingEnvelope aEnvelope) throws PeppolReportingBackendException
  {
    final String sPayload = PeppolReportingEnvelopeJsonHelper.toJsonString (aEnvelope);

    RetryableFailure aLastFailure = null;
    for (int nAttempt = 1; nAttempt <= m_nRetryMaxAttempts; ++nAttempt)
    {
      if (aLastFailure != null)
      {
        final Duration aDelay = getBackOffDelay (nAttempt - 2, m_aRetryInitialDelay, m_aRetryMaxDelay);
        LOGGER.warn (aLastFailure.m_sMessage +
                     " - retrying in " +
                     aDelay.toMillis () +
                     " ms (attempt " +
                     nAttempt +
                     " of " +
                     m_nRetryMaxAttempts +
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

      aLastFailure = _sendOnce (aEnvelope, sPayload);
      if (aLastFailure == null)
        return;
    }

    final String sMsg = aLastFailure.m_sMessage + " - giving up after " + m_nRetryMaxAttempts + " attempts";
    if (aLastFailure.m_aCause != null)
      throw new PeppolReportingBackendException (sMsg, aLastFailure.m_aCause);
    throw new PeppolReportingBackendException (sMsg);
  }

  @Override
  protected void storeEligibleReportingItem (@NonNull @Nonempty final String sItemID,
                                             @NonNull final PeppolReportingItem aReportingItem) throws PeppolReportingBackendException
  {
    storeReportingItems (new CommonsArrayList <> (new PeppolReportingItemWithID (sItemID, aReportingItem)));
  }

  /**
   * Submit the reporting items to the remote collector. Items with document types that are not
   * eligible for reporting are skipped. All other items must belong to the configured Seat ID
   * (compared case-insensitively) and the item IDs must be unique - otherwise nothing is submitted.
   * The items are submitted in batches of the configured maximum batch size. If an exception is
   * thrown, the batches submitted before were already stored by the collector - as the collector
   * ignores duplicates, all items can be submitted again.
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
    checkNotClosed ();

    // First check all items before submitting any of them - either all or none
    final ICommonsList <PeppolReportingItemWithID> aEligibleItems = new CommonsArrayList <> ();
    final ICommonsSet <String> aUniqueIDs = new CommonsHashSet <> ();
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

      if (!aItem.getReportingServiceProviderID ().equalsIgnoreCase (m_sSeatID))
        throw new PeppolReportingBackendException ("The Peppol Reporting Item '" +
                                                   aItemWithID.getItemID () +
                                                   "' belongs to the reporting service provider '" +
                                                   aItem.getReportingServiceProviderID () +
                                                   "' but the backend is configured for the Seat ID '" +
                                                   m_sSeatID +
                                                   "'");

      if (!aUniqueIDs.add (aItemWithID.getItemID ()))
        throw new PeppolReportingBackendException ("The Peppol Reporting Item '" +
                                                   aItemWithID.getItemID () +
                                                   "' is used more than once in the provided batch");

      aEligibleItems.add (aItemWithID);
    }

    final int nCount = aEligibleItems.size ();
    for (int nStart = 0; nStart < nCount; nStart += m_nMaxBatchSize)
    {
      final List <PeppolReportingItemWithID> aBatch = aEligibleItems.subList (nStart,
                                                                              Math.min (nStart + m_nMaxBatchSize,
                                                                                        nCount));
      final PeppolReportingEnvelope aEnvelope;
      try
      {
        aEnvelope = new PeppolReportingEnvelope (m_sSeatID, m_sSourceID, aBatch);
      }
      catch (final IllegalArgumentException ex)
      {
        throw new PeppolReportingBackendException ("Failed to create the Peppol Reporting envelope", ex);
      }

      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Submitting " + aEnvelope.getItemCount () + " Peppol Reporting Items to '" + m_sURL + "'");

      _sendWithRetry (aEnvelope);
    }
  }

  @Override
  protected void onClose ()
  {
    m_aHttpClientMgr.close ();
  }
}
