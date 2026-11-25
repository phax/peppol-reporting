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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.helger.annotation.concurrent.GuardedBy;
import com.helger.base.concurrent.SimpleLock;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsHashMap;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsMap;
import com.helger.config.IConfig;
import com.helger.config.fallback.ConfigWithFallback;
import com.helger.config.source.appl.ConfigurationSourceFunction;
import com.helger.peppol.reporting.api.PeppolReportingItem;
import com.helger.peppol.reporting.api.PeppolReportingItemWithID;
import com.helger.peppol.reporting.api.backend.PeppolReportingBackendException;
import com.helger.peppol.reporting.api.envelope.PeppolReportingEnvelope;
import com.helger.peppol.reporting.api.envelope.PeppolReportingEnvelopeJsonHelper;
import com.helger.peppol.reporting.api.envelope.PeppolReportingRejectedItem;
import com.helger.peppol.reporting.api.envelope.PeppolReportingSubmissionResponse;
import com.helger.peppol.reporting.api.envelope.PeppolReportingSubmissionResponseJsonHelper;
import com.helger.peppol.reporting.testbackend.PeppolReportingTestItems;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * Test class for class {@link PeppolReportingBackendHttpClientSPI}, using a local HTTP server as mock
 * collector.
 *
 * @author Philip Helger
 */
public final class PeppolReportingBackendHttpClientSPITest
{
  private static final String TOKEN = "secret-token";
  // The test items use lower case Seat IDs
  private static final String SEAT_ID = "POP000001";
  private static final String SOURCE_ID = "5f0c6b1e-0a43-4d5c-9b0e-2c3e5c1f7a10";
  private static final LocalDate DATE = LocalDate.of (2026, 11, 3);

  /**
   * A single response of the mock collector.
   */
  private static final class MockResponse
  {
    private final int m_nStatusCode;
    private final String m_sBody;

    MockResponse (final int nStatusCode, @Nullable final String sBody)
    {
      m_nStatusCode = nStatusCode;
      m_sBody = sBody;
    }
  }

  /**
   * A single request received by the mock collector.
   */
  private static final class MockRequest
  {
    private final String m_sToken;
    private final String m_sContentType;
    private final PeppolReportingEnvelope m_aEnvelope;

    MockRequest (@Nullable final String sToken,
                 @Nullable final String sContentType,
                 @Nullable final PeppolReportingEnvelope aEnvelope)
    {
      m_sToken = sToken;
      m_sContentType = sContentType;
      m_aEnvelope = aEnvelope;
    }
  }

  private final SimpleLock m_aLock = new SimpleLock ();
  @GuardedBy ("m_aLock")
  private final ICommonsList <MockResponse> m_aResponses = new CommonsArrayList <> ();
  @GuardedBy ("m_aLock")
  private final ICommonsList <MockRequest> m_aRequests = new CommonsArrayList <> ();
  private HttpServer m_aServer;
  private final PeppolReportingBackendHttpClientSPI m_aBackend = new PeppolReportingBackendHttpClientSPI ();

  private void _handle (@NonNull final HttpExchange aExchange) throws IOException
  {
    final String sBody = new String (aExchange.getRequestBody ().readAllBytes (), StandardCharsets.UTF_8);
    final MockRequest aRequest = new MockRequest (aExchange.getRequestHeaders ()
                                                           .getFirst (PeppolReportingBackendHttpClientSPI.HTTP_HEADER_TOKEN),
                                                  aExchange.getRequestHeaders ().getFirst ("Content-Type"),
                                                  PeppolReportingEnvelopeJsonHelper.toDomain (sBody));
    final MockResponse aResponse = m_aLock.lockedGet ( () -> {
      m_aRequests.add (aRequest);
      // Use the last response for all further requests
      return m_aResponses.size () > 1 ? m_aResponses.remove (0) : m_aResponses.getFirstOrNull ();
    });

    final int nStatusCode = aResponse == null ? 200 : aResponse.m_nStatusCode;
    final byte [] aResponseBytes = aResponse == null || aResponse.m_sBody == null ? new byte [0]
                                                                                   : aResponse.m_sBody.getBytes (StandardCharsets.UTF_8);
    aExchange.getResponseHeaders ().add ("Content-Type", "application/json");
    aExchange.sendResponseHeaders (nStatusCode, aResponseBytes.length == 0 ? -1 : aResponseBytes.length);
    try (final OutputStream aOS = aExchange.getResponseBody ())
    {
      aOS.write (aResponseBytes);
    }
  }

  @Before
  public void before () throws IOException
  {
    m_aServer = HttpServer.create (new InetSocketAddress ("localhost", 0), 0);
    m_aServer.createContext ("/api/reporting/items", aExchange -> {
      try
      {
        _handle (aExchange);
      }
      finally
      {
        aExchange.close ();
      }
    });
    m_aServer.start ();
  }

  @After
  public void after ()
  {
    if (m_aBackend.isInitialized ())
      m_aBackend.shutdownBackend ();
    m_aServer.stop (0);
  }

  private void _addResponse (final int nStatusCode, @Nullable final String sBody)
  {
    m_aLock.locked ( () -> m_aResponses.add (new MockResponse (nStatusCode, sBody)));
  }

  @NonNull
  private ICommonsList <MockRequest> _getRequests ()
  {
    return m_aLock.lockedGet (m_aRequests::getClone);
  }

  @NonNull
  private ICommonsMap <String, String> _createSettings ()
  {
    final ICommonsMap <String, String> ret = new CommonsHashMap <> ();
    ret.put (PeppolReportingBackendHttpClientSPI.CONFIG_PEPPOL_REPORTING_HTTPCLIENT_URL,
             "http://localhost:" + m_aServer.getAddress ().getPort () + "/api/reporting/items");
    ret.put (PeppolReportingBackendHttpClientSPI.CONFIG_PEPPOL_REPORTING_HTTPCLIENT_TOKEN, TOKEN);
    ret.put (PeppolReportingBackendHttpClientSPI.CONFIG_PEPPOL_REPORTING_HTTPCLIENT_SEATID, SEAT_ID);
    ret.put (PeppolReportingBackendHttpClientSPI.CONFIG_PEPPOL_REPORTING_HTTPCLIENT_SOURCEID, SOURCE_ID);
    ret.put (PeppolReportingBackendHttpClientSPI.CONFIG_PEPPOL_REPORTING_HTTPCLIENT_RETRY_MAXATTEMPTS, "3");
    ret.put (PeppolReportingBackendHttpClientSPI.CONFIG_PEPPOL_REPORTING_HTTPCLIENT_RETRY_INITIALDELAY, "10ms");
    ret.put (PeppolReportingBackendHttpClientSPI.CONFIG_PEPPOL_REPORTING_HTTPCLIENT_RETRY_MAXDELAY, "40ms");
    return ret;
  }

  @NonNull
  private static IConfig _createConfig (@NonNull final Map <String, String> aSettings)
  {
    return new ConfigWithFallback (new ConfigurationSourceFunction (aSettings::get));
  }

  private void _init (@NonNull final Map <String, String> aSettings)
  {
    assertTrue (m_aBackend.initBackend (_createConfig (aSettings)).isSuccess ());
    assertTrue (m_aBackend.isInitialized ());
  }

  @NonNull
  private static ICommonsList <PeppolReportingItemWithID> _createSendingItems (final int nCount)
  {
    final ICommonsList <PeppolReportingItemWithID> ret = new CommonsArrayList <> ();
    for (int i = 0; i < nCount; ++i)
      ret.add (new PeppolReportingItemWithID ("id" + i, PeppolReportingTestItems.sendingItem (DATE, i)));
    return ret;
  }

  private void _expectInitFailure (@NonNull final String sKey, @Nullable final String sValue)
  {
    final ICommonsMap <String, String> aSettings = _createSettings ();
    if (sValue == null)
      aSettings.remove (sKey);
    else
      aSettings.put (sKey, sValue);
    assertTrue (sKey + "=" + sValue, m_aBackend.initBackend (_createConfig (aSettings)).isFailure ());
    assertFalse (m_aBackend.isInitialized ());
  }

  @Test
  public void testInitFailures ()
  {
    _expectInitFailure (PeppolReportingBackendHttpClientSPI.CONFIG_PEPPOL_REPORTING_HTTPCLIENT_URL, null);
    _expectInitFailure (PeppolReportingBackendHttpClientSPI.CONFIG_PEPPOL_REPORTING_HTTPCLIENT_URL, "ftp://localhost/x");
    _expectInitFailure (PeppolReportingBackendHttpClientSPI.CONFIG_PEPPOL_REPORTING_HTTPCLIENT_TOKEN, null);
    _expectInitFailure (PeppolReportingBackendHttpClientSPI.CONFIG_PEPPOL_REPORTING_HTTPCLIENT_SEATID, null);
    _expectInitFailure (PeppolReportingBackendHttpClientSPI.CONFIG_PEPPOL_REPORTING_HTTPCLIENT_SOURCEID, null);
    _expectInitFailure (PeppolReportingBackendHttpClientSPI.CONFIG_PEPPOL_REPORTING_HTTPCLIENT_SOURCEID,
                        "x".repeat (PeppolReportingEnvelope.MAX_LEN_SOURCE_ID + 1));
    _expectInitFailure (PeppolReportingBackendHttpClientSPI.CONFIG_PEPPOL_REPORTING_HTTPCLIENT_MAXBATCHSIZE, "0");
    _expectInitFailure (PeppolReportingBackendHttpClientSPI.CONFIG_PEPPOL_REPORTING_HTTPCLIENT_MAXBATCHSIZE,
                        Integer.toString (PeppolReportingEnvelope.MAX_ITEMS + 1));
    _expectInitFailure (PeppolReportingBackendHttpClientSPI.CONFIG_PEPPOL_REPORTING_HTTPCLIENT_RETRY_MAXATTEMPTS, "0");
    _expectInitFailure (PeppolReportingBackendHttpClientSPI.CONFIG_PEPPOL_REPORTING_HTTPCLIENT_RETRY_MAXDELAY, "5ms");
  }

  @Test
  public void testNotInitialized () throws Exception
  {
    try
    {
      m_aBackend.storeReportingItems (_createSendingItems (1));
      fail ();
    }
    catch (final IllegalStateException ex)
    {
      // expected
    }
  }

  @Test
  public void testSubmitInBatches () throws Exception
  {
    final ICommonsMap <String, String> aSettings = _createSettings ();
    aSettings.put (PeppolReportingBackendHttpClientSPI.CONFIG_PEPPOL_REPORTING_HTTPCLIENT_MAXBATCHSIZE, "2");
    _init (aSettings);

    _addResponse (200,
                  PeppolReportingSubmissionResponseJsonHelper.toJsonString (PeppolReportingSubmissionResponse.createAccepted (2,
                                                                                                                              0)));
    final ICommonsList <PeppolReportingItemWithID> aItems = _createSendingItems (5);
    m_aBackend.storeReportingItems (aItems);

    final ICommonsList <MockRequest> aRequests = _getRequests ();
    assertEquals (3, aRequests.size ());
    final ICommonsList <PeppolReportingItemWithID> aReceived = new CommonsArrayList <> ();
    for (final MockRequest aRequest : aRequests)
    {
      assertEquals (TOKEN, aRequest.m_sToken);
      assertTrue (aRequest.m_sContentType, aRequest.m_sContentType.startsWith ("application/json"));
      assertNotNull (aRequest.m_aEnvelope);
      assertEquals (SEAT_ID, aRequest.m_aEnvelope.getSeatID ());
      assertEquals (SOURCE_ID, aRequest.m_aEnvelope.getSourceID ());
      aReceived.addAll (aRequest.m_aEnvelope.getAllItems ());
    }
    assertEquals (2, aRequests.get (0).m_aEnvelope.getItemCount ());
    assertEquals (1, aRequests.get (2).m_aEnvelope.getItemCount ());
    assertEquals (aItems, aReceived);
  }

  @Test
  public void testSingleItemWithoutResponseBody () throws Exception
  {
    _init (_createSettings ());

    _addResponse (204, null);
    final PeppolReportingItemWithID aItem = _createSendingItems (1).getFirstOrNull ();
    m_aBackend.storeReportingItem (aItem.getItemID (), aItem.getReportingItem ());

    final ICommonsList <MockRequest> aRequests = _getRequests ();
    assertEquals (1, aRequests.size ());
    assertEquals (new CommonsArrayList <> (aItem), aRequests.getFirstOrNull ().m_aEnvelope.getAllItems ());
  }

  @Test
  public void testSingleItemWithRandomItemID () throws Exception
  {
    _init (_createSettings ());

    _addResponse (204, null);
    _addResponse (204, null);
    final PeppolReportingItem aItem = PeppolReportingTestItems.sendingItem (DATE, 1);
    m_aBackend.storeReportingItem (aItem);
    m_aBackend.storeReportingItem (aItem);

    final ICommonsList <MockRequest> aRequests = _getRequests ();
    assertEquals (2, aRequests.size ());
    final PeppolReportingItemWithID aSent0 = aRequests.get (0).m_aEnvelope.getAllItems ().getFirstOrNull ();
    final PeppolReportingItemWithID aSent1 = aRequests.get (1).m_aEnvelope.getAllItems ().getFirstOrNull ();
    assertEquals (aItem, aSent0.getReportingItem ());
    assertEquals (aItem, aSent1.getReportingItem ());

    // A new UUID is used for each call
    assertEquals (aSent0.getItemID (), UUID.fromString (aSent0.getItemID ()).toString ());
    assertEquals (aSent1.getItemID (), UUID.fromString (aSent1.getItemID ()).toString ());
    assertNotEquals (aSent0.getItemID (), aSent1.getItemID ());
  }

  @Test
  public void testNonEligibleItemsAreSkipped () throws Exception
  {
    _init (_createSettings ());

    m_aBackend.storeReportingItems (new CommonsArrayList <> (new PeppolReportingItemWithID ("n1",
                                                                                            PeppolReportingTestItems.nonEligibleItem (DATE))));
    assertTrue (_getRequests ().isEmpty ());

    // Empty list
    m_aBackend.storeReportingItems (new CommonsArrayList <> ());
    assertTrue (_getRequests ().isEmpty ());
  }

  @Test
  public void testSeatIDMismatch ()
  {
    _init (_createSettings ());

    // The receiving test items belong to C3 "pop000002"
    final ICommonsList <PeppolReportingItemWithID> aItems = _createSendingItems (2);
    aItems.add (new PeppolReportingItemWithID ("r1", PeppolReportingTestItems.receivingItem (DATE, 1)));
    try
    {
      m_aBackend.storeReportingItems (aItems);
      fail ();
    }
    catch (final PeppolReportingBackendException ex)
    {
      assertTrue (ex.getMessage (), ex.getMessage ().contains ("'r1'"));
    }
    // Nothing was submitted
    assertTrue (_getRequests ().isEmpty ());
  }

  @Test
  public void testDuplicateItemIDs ()
  {
    _init (_createSettings ());

    final PeppolReportingItemWithID aItem = _createSendingItems (1).getFirstOrNull ();
    try
    {
      m_aBackend.storeReportingItems (new CommonsArrayList <> (aItem, aItem));
      fail ();
    }
    catch (final PeppolReportingBackendException ex)
    {
      // expected
    }
    assertTrue (_getRequests ().isEmpty ());
  }

  @Test
  public void testRetrySucceeds () throws Exception
  {
    _init (_createSettings ());

    _addResponse (503, null);
    _addResponse (429, null);
    _addResponse (200, null);
    m_aBackend.storeReportingItems (_createSendingItems (3));

    final ICommonsList <MockRequest> aRequests = _getRequests ();
    assertEquals (3, aRequests.size ());
    // The same envelope is sent each time
    assertEquals (aRequests.get (0).m_aEnvelope, aRequests.get (2).m_aEnvelope);
  }

  @Test
  public void testRetryGivesUp ()
  {
    _init (_createSettings ());

    _addResponse (500, "Internal error");
    try
    {
      m_aBackend.storeReportingItems (_createSendingItems (1));
      fail ();
    }
    catch (final PeppolReportingBackendException ex)
    {
      assertFalse (ex instanceof PeppolReportingSubmissionRejectedException);
      assertTrue (ex.getMessage (), ex.getMessage ().contains ("3 attempts"));
    }
    assertEquals (3, _getRequests ().size ());
  }

  @Test
  public void testConnectionFailureIsRetried ()
  {
    final ICommonsMap <String, String> aSettings = _createSettings ();
    _init (aSettings);
    // Nobody is listening any more
    m_aServer.stop (0);

    try
    {
      m_aBackend.storeReportingItems (_createSendingItems (1));
      fail ();
    }
    catch (final PeppolReportingBackendException ex)
    {
      assertTrue (ex.getMessage (), ex.getMessage ().contains ("3 attempts"));
      assertTrue (ex.getCause () instanceof IOException);
    }
  }

  @Test
  public void testRejected ()
  {
    _init (_createSettings ());

    final List <PeppolReportingRejectedItem> aRejected = new CommonsArrayList <> (new PeppolReportingRejectedItem ("id1",
                                                                                                                  "C1 country code is invalid"));
    _addResponse (400,
                  PeppolReportingSubmissionResponseJsonHelper.toJsonString (PeppolReportingSubmissionResponse.createRejected (aRejected)));
    try
    {
      m_aBackend.storeReportingItems (_createSendingItems (3));
      fail ();
    }
    catch (final PeppolReportingSubmissionRejectedException ex)
    {
      assertEquals (aRejected, ex.getAllRejectedItems ());
    }
    catch (final PeppolReportingBackendException ex)
    {
      fail ("Wrong exception " + ex);
    }
    // Not retried
    assertEquals (1, _getRequests ().size ());
  }

  @Test
  public void testRejectedWithoutDetails ()
  {
    _init (_createSettings ());

    _addResponse (400, "Unsupported envelope version");
    try
    {
      m_aBackend.storeReportingItems (_createSendingItems (1));
      fail ();
    }
    catch (final PeppolReportingBackendException ex)
    {
      assertFalse (ex instanceof PeppolReportingSubmissionRejectedException);
      assertTrue (ex.getMessage (), ex.getMessage ().contains ("Unsupported envelope version"));
    }
    assertEquals (1, _getRequests ().size ());
  }

  @Test
  public void testUnauthorizedIsNotRetried ()
  {
    _init (_createSettings ());

    _addResponse (401, null);
    try
    {
      m_aBackend.storeReportingItems (_createSendingItems (1));
      fail ();
    }
    catch (final PeppolReportingBackendException ex)
    {
      assertTrue (ex.getMessage (), ex.getMessage ().contains ("401"));
    }
    assertEquals (1, _getRequests ().size ());
  }

  @Test
  public void testUnsupportedOperations ()
  {
    _init (_createSettings ());

    try
    {
      m_aBackend.iterateReportingItems (YearMonth.from (DATE));
      fail ();
    }
    catch (final PeppolReportingBackendException ex)
    {
      // expected
    }

    try
    {
      m_aBackend.forEachReportingItem (YearMonth.from (DATE), x -> {});
      fail ();
    }
    catch (final PeppolReportingBackendException ex)
    {
      // expected
    }
    assertTrue (_getRequests ().isEmpty ());
  }

  @Test
  public void testBackOffDelay ()
  {
    final Duration aInitial = Duration.ofMillis (100);
    final Duration aMax = Duration.ofMillis (1000);
    for (int i = 0; i < 100; ++i)
    {
      final long n0 = PeppolReportingBackendHttpClientSPI.getBackOffDelay (0, aInitial, aMax).toMillis ();
      assertTrue (Long.toString (n0), n0 >= 50 && n0 <= 100);

      final long n2 = PeppolReportingBackendHttpClientSPI.getBackOffDelay (2, aInitial, aMax).toMillis ();
      assertTrue (Long.toString (n2), n2 >= 200 && n2 <= 400);

      // Capped at the maximum
      final long n10 = PeppolReportingBackendHttpClientSPI.getBackOffDelay (10, aInitial, aMax).toMillis ();
      assertTrue (Long.toString (n10), n10 >= 500 && n10 <= 1000);

      // No overflow for large indices
      final long nLarge = PeppolReportingBackendHttpClientSPI.getBackOffDelay (Integer.MAX_VALUE, aInitial, aMax)
                                                         .toMillis ();
      assertTrue (Long.toString (nLarge), nLarge >= 500 && nLarge <= 1000);
    }
    assertEquals (0, PeppolReportingBackendHttpClientSPI.getBackOffDelay (3, Duration.ZERO, Duration.ZERO).toMillis ());
    assertEquals (0,
                  PeppolReportingBackendHttpClientSPI.getBackOffDelay (Integer.MAX_VALUE, Duration.ZERO, aMax).toMillis ());
  }
}
