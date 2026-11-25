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
package com.helger.peppol.reporting.api.backend;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.junit.Test;

import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.config.Config;
import com.helger.config.IConfig;
import com.helger.config.source.appl.ConfigurationSourceFunction;
import com.helger.peppol.reporting.api.PeppolReportingItem;
import com.helger.peppolid.peppol.doctype.EPredefinedDocumentTypeIdentifier;
import com.helger.peppolid.peppol.process.EPredefinedProcessIdentifier;

/**
 * Test class for class {@link PeppolReportingBackend}. Two mock providers are registered via
 * <code>src/test/resources</code>.
 *
 * @author Philip Helger
 */
public final class PeppolReportingBackendTest
{
  private static final OffsetDateTime EXCHANGE_DT = OffsetDateTime.parse ("2026-03-15T10:00:00Z");

  @NonNull
  private static IConfig _createConfig (@NonNull final Map <String, String> aSettings)
  {
    return new Config (new ConfigurationSourceFunction (aSettings::get));
  }

  @NonNull
  private static IConfig _createConfigForBackend (@NonNull final String sBackendID)
  {
    return _createConfig (Map.of (PeppolReportingBackend.CONFIG_PEPPOL_REPORTING_BACKEND, sBackendID));
  }

  @NonNull
  private static PeppolReportingItem _createItem ()
  {
    return PeppolReportingItem.builder ()
                              .exchangeDateTime (EXCHANGE_DT)
                              .directionSending ()
                              .c2ID ("PAT000001")
                              .c3ID ("POP000002")
                              .docTypeID (EPredefinedDocumentTypeIdentifier.INVOICE_EN16931_PEPPOL_V30)
                              .processID (EPredefinedProcessIdentifier.BIS3_BILLING)
                              .transportProtocolPeppolAS4v2 ()
                              .c1CountryCode ("FI")
                              .endUserID ("abc")
                              .build ();
  }

  private static void _expectSelectionFailure (@NonNull final IConfig aConfig, @NonNull final String sExpectedText)
  {
    try
    {
      PeppolReportingBackend.getBackendProvider (aConfig);
      fail ();
    }
    catch (final PeppolReportingBackendException ex)
    {
      assertTrue (ex.getMessage (), ex.getMessage ().contains (sExpectedText));
    }
  }

  @Test
  public void testGetAllBackendProviders ()
  {
    final ICommonsList <IPeppolReportingBackendProviderSPI> aProviders = PeppolReportingBackend.getAllBackendProviders ();
    assertEquals (2, aProviders.size ());

    // A copy is returned
    aProviders.clear ();
    assertEquals (2, PeppolReportingBackend.getAllBackendProviders ().size ());
  }

  @Test
  public void testSelection () throws Exception
  {
    assertSame (MockBackendProviderASPI.class,
                PeppolReportingBackend.getBackendProvider (_createConfigForBackend ("mock-a")).getClass ());
    // Case-insensitive
    assertSame (MockBackendProviderBSPI.class,
                PeppolReportingBackend.getBackendProvider (_createConfigForBackend ("MOCK-B")).getClass ());

    // No selection with two providers
    _expectSelectionFailure (_createConfig (Map.of ()), PeppolReportingBackend.CONFIG_PEPPOL_REPORTING_BACKEND);
    // Unknown backend - the available ones are listed
    _expectSelectionFailure (_createConfigForBackend ("sql"), "mock-a, mock-b");
  }

  @Test
  public void testCreateBackend () throws Exception
  {
    final PeppolReportingItem aItem = _createItem ();
    final LocalDate aDate = EXCHANGE_DT.toLocalDate ();

    final IPeppolReportingBackend aBackend = PeppolReportingBackend.createBackend (_createConfigForBackend ("mock-a"));
    try
    {
      assertEquals ("MockA", aBackend.getDisplayName ());
      aBackend.storeReportingItem ("id1", aItem);

      final IPeppolReportingItemReader aReader = (IPeppolReportingItemReader) aBackend;
      final ICommonsList <PeppolReportingItem> aFound = new CommonsArrayList <> ();
      aReader.forEachReportingItem (aDate, aDate, aFound::add);
      assertEquals (new CommonsArrayList <> (aItem), aFound);
      assertEquals (aFound, new CommonsArrayList <> (aReader.iterateReportingItems (aDate, aDate)));
    }
    finally
    {
      aBackend.close ();
    }

    // Closing twice has no effect
    aBackend.close ();
    try
    {
      aBackend.storeReportingItem ("id2", aItem);
      fail ();
    }
    catch (final IllegalStateException ex)
    {
      // expected
    }
  }

  @Test
  @SuppressWarnings ("removal")
  public void testDeprecatedBackendService () throws Exception
  {
    final PeppolReportingItem aItem = _createItem ();
    final LocalDate aDate = EXCHANGE_DT.toLocalDate ();

    final IPeppolReportingBackendSPI aBackendService = PeppolReportingBackend.getBackendService ();
    assertTrue (PeppolReportingBackend.isBackendServiceConfigured ());
    assertFalse (aBackendService.isInitialized ());

    // The selection fails without configuration
    assertTrue (PeppolReportingBackend.withBackendDo (_createConfig (Map.of ()), x -> fail ()).isFailure ());
    assertFalse (aBackendService.isInitialized ());

    final ICommonsList <PeppolReportingItem> aFound = new CommonsArrayList <> ();
    assertTrue (PeppolReportingBackend.withBackendDo (_createConfigForBackend ("mock-b"), aBackend -> {
      assertEquals ("MockB", aBackend.getDisplayName ());
      aBackend.storeReportingItem (aItem);
      aBackend.forEachReportingItem (aDate, aDate, aFound::add);
    }).isSuccess ());
    assertEquals (new CommonsArrayList <> (aItem), aFound);

    // Closed again
    assertFalse (aBackendService.isInitialized ());
  }
}
