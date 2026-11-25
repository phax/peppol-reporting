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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.jspecify.annotations.NonNull;
import org.junit.Test;

import com.helger.collection.commons.CommonsArrayList;
import com.helger.json.IJsonObject;
import com.helger.unittest.support.TestHelper;

/**
 * Test class for classes {@link PeppolReportingSubmissionResponse} and
 * {@link PeppolReportingSubmissionResponseJsonHelper}.
 *
 * @author Philip Helger
 */
public final class PeppolReportingSubmissionResponseJsonHelperTest
{
  private static void _expectIAE (@NonNull final IJsonObject aJson)
  {
    try
    {
      PeppolReportingSubmissionResponseJsonHelper.toDomain (aJson);
      fail ();
    }
    catch (final IllegalArgumentException ex)
    {
      // expected
    }
  }

  @Test
  public void testAccepted ()
  {
    final PeppolReportingSubmissionResponse aResponse = PeppolReportingSubmissionResponse.createAccepted (5, 2);
    assertTrue (aResponse.isAccepted ());
    assertEquals (5, aResponse.getAcceptedCount ());
    assertEquals (2, aResponse.getDuplicateCount ());
    assertTrue (aResponse.getAllRejectedItems ().isEmpty ());

    final String sJson = PeppolReportingSubmissionResponseJsonHelper.toJsonString (aResponse);
    assertTrue (sJson.contains ("\"version\":1"));
    assertEquals (aResponse, PeppolReportingSubmissionResponseJsonHelper.toDomain (sJson));

    TestHelper.testDefaultImplementationWithEqualContentObject (aResponse,
                                                                PeppolReportingSubmissionResponse.createAccepted (5,
                                                                                                                  2));
    TestHelper.testDefaultImplementationWithDifferentContentObject (aResponse,
                                                                    PeppolReportingSubmissionResponse.createAccepted (5,
                                                                                                                      3));
  }

  @Test
  public void testRejected ()
  {
    final PeppolReportingSubmissionResponse aResponse = PeppolReportingSubmissionResponse.createRejected (new CommonsArrayList <> (new PeppolReportingRejectedItem ("id1",
                                                                                                                                                                  "C1 country code is invalid"),
                                                                                                                                  new PeppolReportingRejectedItem ("id7",
                                                                                                                                                                  "Seat ID mismatch")));
    assertFalse (aResponse.isAccepted ());
    assertEquals (0, aResponse.getAcceptedCount ());
    assertEquals (0, aResponse.getDuplicateCount ());
    assertEquals (2, aResponse.getAllRejectedItems ().size ());
    assertEquals ("id7", aResponse.getAllRejectedItems ().get (1).getItemID ());

    assertEquals (aResponse,
                  PeppolReportingSubmissionResponseJsonHelper.toDomain (PeppolReportingSubmissionResponseJsonHelper.toJson (aResponse)));
  }

  @Test
  public void testInvalid ()
  {
    assertNull (PeppolReportingSubmissionResponseJsonHelper.toDomain ((String) null));
    assertNull (PeppolReportingSubmissionResponseJsonHelper.toDomain ("no JSON"));

    // Unsupported version
    IJsonObject aJson = PeppolReportingSubmissionResponseJsonHelper.toJson (PeppolReportingSubmissionResponse.createAccepted (1,
                                                                                                                              0));
    aJson.add (PeppolReportingSubmissionResponseJsonHelper.JSON_VERSION, 2);
    _expectIAE (aJson);

    // Negative count
    aJson = PeppolReportingSubmissionResponseJsonHelper.toJson (PeppolReportingSubmissionResponse.createAccepted (1, 0));
    aJson.add (PeppolReportingSubmissionResponseJsonHelper.JSON_ACCEPTED, -1);
    _expectIAE (aJson);

    // Rejected and accepted at the same time
    aJson = PeppolReportingSubmissionResponseJsonHelper.toJson (PeppolReportingSubmissionResponse.createRejected (new CommonsArrayList <> (new PeppolReportingRejectedItem ("id1",
                                                                                                                                                                        "bad"))));
    aJson.add (PeppolReportingSubmissionResponseJsonHelper.JSON_ACCEPTED, 1);
    _expectIAE (aJson);

    // Rejected item without reason
    aJson = PeppolReportingSubmissionResponseJsonHelper.toJson (PeppolReportingSubmissionResponse.createRejected (new CommonsArrayList <> (new PeppolReportingRejectedItem ("id1",
                                                                                                                                                                        "bad"))));
    aJson.getAsArray (PeppolReportingSubmissionResponseJsonHelper.JSON_REJECTED)
         .getObjectAtIndex (0)
         .removeKey (PeppolReportingSubmissionResponseJsonHelper.JSON_REASON);
    _expectIAE (aJson);
  }
}
