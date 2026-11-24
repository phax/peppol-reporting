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
package com.helger.peppol.reporting.api.envelope;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.jspecify.annotations.NonNull;
import org.junit.Test;

import com.helger.collection.commons.CommonsArrayList;
import com.helger.json.IJsonArray;
import com.helger.json.IJsonObject;
import com.helger.json.JsonArray;
import com.helger.json.JsonValue;
import com.helger.peppol.reporting.api.PeppolReportingItemWithID;
import com.helger.peppol.reporting.api.PeppolReportingJsonHelper;

/**
 * Test class for class {@link PeppolReportingEnvelopeJsonHelper}.
 *
 * @author Philip Helger
 */
public final class PeppolReportingEnvelopeJsonHelperTest
{
  @NonNull
  private static PeppolReportingEnvelope _createEnvelope ()
  {
    return new PeppolReportingEnvelope ("PAT000001",
                                        "source1",
                                        new CommonsArrayList <> (PeppolReportingEnvelopeTest.createItem ("id1"),
                                                                 PeppolReportingEnvelopeTest.createItem ("id2")));
  }

  private static void _expectIAE (@NonNull final IJsonObject aJson)
  {
    try
    {
      PeppolReportingEnvelopeJsonHelper.toDomain (aJson);
      fail ();
    }
    catch (final IllegalArgumentException ex)
    {
      // expected
    }
  }

  @Test
  public void testRoundTrip ()
  {
    final PeppolReportingEnvelope aEnvelope = _createEnvelope ();
    final IJsonObject aJson = PeppolReportingEnvelopeJsonHelper.toJson (aEnvelope);
    assertNotNull (aJson);
    assertEquals (PeppolReportingEnvelopeJsonHelper.JSON_VERSION_1,
                  aJson.getAsInt (PeppolReportingEnvelopeJsonHelper.JSON_VERSION, -1));
    assertEquals ("PAT000001", aJson.getAsString (PeppolReportingEnvelopeJsonHelper.JSON_SEATID));
    assertEquals ("source1", aJson.getAsString (PeppolReportingEnvelopeJsonHelper.JSON_SOURCEID));

    final IJsonArray aData = aJson.getAsArray (PeppolReportingEnvelopeJsonHelper.JSON_DATA);
    assertNotNull (aData);
    assertEquals (2, aData.size ());
    final IJsonObject aFirst = aData.getObjectAtIndex (0);
    assertEquals ("id1", aFirst.getAsString (PeppolReportingEnvelopeJsonHelper.JSON_ID));
    // The item itself contains its own version
    assertEquals (PeppolReportingJsonHelper.JSON_VERSION_1,
                  aFirst.getAsObject (PeppolReportingEnvelopeJsonHelper.JSON_ITEM)
                        .getAsInt (PeppolReportingJsonHelper.JSON_VERSION, -1));

    // Object round trip
    assertEquals (aEnvelope, PeppolReportingEnvelopeJsonHelper.toDomain (aJson));

    // String round trip
    final String sJson = PeppolReportingEnvelopeJsonHelper.toJsonString (aEnvelope);
    assertTrue (sJson.contains ("\"version\":1"));
    assertEquals (aEnvelope, PeppolReportingEnvelopeJsonHelper.toDomain (sJson));
  }

  @Test
  public void testItemRoundTrip ()
  {
    final PeppolReportingItemWithID aItem = PeppolReportingEnvelopeTest.createItem ("abc");
    assertEquals (aItem,
                  PeppolReportingEnvelopeJsonHelper.toDomainItem (PeppolReportingEnvelopeJsonHelper.toJson (aItem)));
  }

  @Test
  public void testToDomainInvalid ()
  {
    assertNull (PeppolReportingEnvelopeJsonHelper.toDomain ((String) null));
    assertNull (PeppolReportingEnvelopeJsonHelper.toDomain ("this is no JSON"));
    assertNull (PeppolReportingEnvelopeJsonHelper.toDomain ("[1,2,3]"));

    // Unsupported envelope version
    IJsonObject aJson = PeppolReportingEnvelopeJsonHelper.toJson (_createEnvelope ());
    aJson.add (PeppolReportingEnvelopeJsonHelper.JSON_VERSION, 2);
    _expectIAE (aJson);

    // Missing Seat ID
    aJson = PeppolReportingEnvelopeJsonHelper.toJson (_createEnvelope ());
    aJson.removeKey (PeppolReportingEnvelopeJsonHelper.JSON_SEATID);
    _expectIAE (aJson);

    // Missing source ID
    aJson = PeppolReportingEnvelopeJsonHelper.toJson (_createEnvelope ());
    aJson.removeKey (PeppolReportingEnvelopeJsonHelper.JSON_SOURCEID);
    _expectIAE (aJson);

    // Empty data
    aJson = PeppolReportingEnvelopeJsonHelper.toJson (_createEnvelope ());
    aJson.add (PeppolReportingEnvelopeJsonHelper.JSON_DATA, new JsonArray ());
    _expectIAE (aJson);

    // Data contains a non-object
    aJson = PeppolReportingEnvelopeJsonHelper.toJson (_createEnvelope ());
    aJson.getAsArray (PeppolReportingEnvelopeJsonHelper.JSON_DATA).add (JsonValue.create (5));
    _expectIAE (aJson);

    // Item without ID
    aJson = PeppolReportingEnvelopeJsonHelper.toJson (_createEnvelope ());
    aJson.getAsArray (PeppolReportingEnvelopeJsonHelper.JSON_DATA)
         .getObjectAtIndex (0)
         .removeKey (PeppolReportingEnvelopeJsonHelper.JSON_ID);
    _expectIAE (aJson);

    // Item without item
    aJson = PeppolReportingEnvelopeJsonHelper.toJson (_createEnvelope ());
    aJson.getAsArray (PeppolReportingEnvelopeJsonHelper.JSON_DATA)
         .getObjectAtIndex (0)
         .removeKey (PeppolReportingEnvelopeJsonHelper.JSON_ITEM);
    _expectIAE (aJson);

    // Unsupported item version
    aJson = PeppolReportingEnvelopeJsonHelper.toJson (_createEnvelope ());
    aJson.getAsArray (PeppolReportingEnvelopeJsonHelper.JSON_DATA)
         .getObjectAtIndex (0)
         .getAsObject (PeppolReportingEnvelopeJsonHelper.JSON_ITEM)
         .add (PeppolReportingJsonHelper.JSON_VERSION, 2);
    _expectIAE (aJson);

    // Duplicate item ID
    aJson = PeppolReportingEnvelopeJsonHelper.toJson (_createEnvelope ());
    aJson.getAsArray (PeppolReportingEnvelopeJsonHelper.JSON_DATA)
         .getObjectAtIndex (1)
         .add (PeppolReportingEnvelopeJsonHelper.JSON_ID, "id1");
    _expectIAE (aJson);
  }
}
