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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.json.IJson;
import com.helger.json.IJsonArray;
import com.helger.json.IJsonObject;
import com.helger.json.JsonArray;
import com.helger.json.JsonObject;
import com.helger.json.serialize.JsonReader;

/**
 * The default JSON serialization of a {@link PeppolReportingSubmissionResponse}.<br>
 * Each created JSON object contains the field {@value #JSON_VERSION} with the value
 * {@link #JSON_VERSION_1}, so that the layout can be extended in a backwards compatible way later
 * on.
 *
 * @author Philip Helger
 * @since 5.0.0
 */
@Immutable
public final class PeppolReportingSubmissionResponseJsonHelper
{
  /** The name of the JSON field containing the response layout version */
  public static final String JSON_VERSION = "version";
  public static final String JSON_ACCEPTED = "accepted";
  public static final String JSON_DUPLICATES = "duplicates";
  public static final String JSON_REJECTED = "rejected";
  public static final String JSON_ID = "id";
  public static final String JSON_REASON = "reason";

  /** The initial response layout version */
  public static final int JSON_VERSION_1 = 1;

  /** The response layout version used when writing */
  public static final int JSON_VERSION_CURRENT = JSON_VERSION_1;

  private PeppolReportingSubmissionResponseJsonHelper ()
  {}

  /**
   * Convert a {@link PeppolReportingSubmissionResponse} to a JSON object.
   *
   * @param aValue
   *        The response to be converted. May not be <code>null</code>.
   * @return The created JSON object and never <code>null</code>.
   */
  @NonNull
  public static IJsonObject toJson (@NonNull final PeppolReportingSubmissionResponse aValue)
  {
    ValueEnforcer.notNull (aValue, "Value");

    final IJsonArray aRejected = new JsonArray ();
    for (final PeppolReportingRejectedItem aItem : aValue.getAllRejectedItems ())
      aRejected.add (new JsonObject ().add (JSON_ID, aItem.getItemID ()).add (JSON_REASON, aItem.getReason ()));

    return new JsonObject ().add (JSON_VERSION, JSON_VERSION_CURRENT)
                            .add (JSON_ACCEPTED, aValue.getAcceptedCount ())
                            .add (JSON_DUPLICATES, aValue.getDuplicateCount ())
                            .add (JSON_REJECTED, aRejected);
  }

  /**
   * Convert a {@link PeppolReportingSubmissionResponse} to a JSON string.
   *
   * @param aValue
   *        The response to be converted. May not be <code>null</code>.
   * @return The created JSON string and never <code>null</code>.
   * @see #toJson(PeppolReportingSubmissionResponse)
   */
  @NonNull
  public static String toJsonString (@NonNull final PeppolReportingSubmissionResponse aValue)
  {
    return toJson (aValue).getAsJsonString ();
  }

  /**
   * Convert a JSON object back to a {@link PeppolReportingSubmissionResponse}.
   *
   * @param aJson
   *        The JSON object to be converted. May not be <code>null</code>.
   * @return The restored response. Never <code>null</code>.
   * @throws IllegalArgumentException
   *         if the layout version is not supported or if the response is invalid
   */
  @NonNull
  public static PeppolReportingSubmissionResponse toDomain (@NonNull final IJsonObject aJson)
  {
    ValueEnforcer.notNull (aJson, "Json");

    final int nVersion = aJson.getAsInt (JSON_VERSION, -1);
    if (nVersion != JSON_VERSION_1)
      throw new IllegalArgumentException ("The provided JSON object uses the unsupported Peppol Reporting Submission Response layout version " +
                                          nVersion);

    final int nAccepted = aJson.getAsInt (JSON_ACCEPTED, 0);
    final int nDuplicates = aJson.getAsInt (JSON_DUPLICATES, 0);

    final ICommonsList <PeppolReportingRejectedItem> aRejectedItems = new CommonsArrayList <> ();
    final IJsonArray aRejected = aJson.getAsArray (JSON_REJECTED);
    if (aRejected != null)
      for (final IJson aEntry : aRejected)
      {
        if (!aEntry.isObject ())
          throw new IllegalArgumentException ("The array field '" + JSON_REJECTED + "' may only contain objects");

        final IJsonObject aObj = aEntry.getAsObject ();
        final String sItemID = aObj.getAsString (JSON_ID);
        if (StringHelper.isEmpty (sItemID))
          throw new IllegalArgumentException ("A rejected item is missing the field '" + JSON_ID + "'");
        final String sReason = aObj.getAsString (JSON_REASON);
        if (StringHelper.isEmpty (sReason))
          throw new IllegalArgumentException ("The rejected item '" +
                                              sItemID +
                                              "' is missing the field '" +
                                              JSON_REASON +
                                              "'");
        aRejectedItems.add (new PeppolReportingRejectedItem (sItemID, sReason));
      }

    return new PeppolReportingSubmissionResponse (nAccepted, nDuplicates, aRejectedItems);
  }

  /**
   * Convert a JSON string back to a {@link PeppolReportingSubmissionResponse}.
   *
   * @param sJson
   *        The JSON string to be converted. May be <code>null</code>.
   * @return <code>null</code> if the provided String is not a JSON object.
   * @throws IllegalArgumentException
   *         if the layout version is not supported or if the response is invalid
   * @see #toDomain(IJsonObject)
   */
  @Nullable
  public static PeppolReportingSubmissionResponse toDomain (@Nullable final String sJson)
  {
    if (sJson == null)
      return null;

    final IJson aJson = JsonReader.readFromString (sJson);
    if (aJson == null || !aJson.isObject ())
      return null;

    return toDomain (aJson.getAsObject ());
  }
}
