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
import com.helger.peppol.reporting.api.PeppolReportingItemWithID;
import com.helger.peppol.reporting.api.PeppolReportingJsonHelper;

/**
 * The default JSON serialization of a {@link PeppolReportingEnvelope}, to be used to transfer a
 * batch of Peppol Reporting items to a remote reporting collector.<br>
 * Each created JSON object contains the field {@value #JSON_VERSION} with the value
 * {@link #JSON_VERSION_1}, so that the layout can be extended in a backwards compatible way later
 * on. This version is independent of the version of the contained reporting items, which is
 * contained in each item (see {@link PeppolReportingJsonHelper}).
 *
 * @author Philip Helger
 * @since 4.6.0
 */
@Immutable
public final class PeppolReportingEnvelopeJsonHelper
{
  /** The name of the JSON field containing the envelope layout version */
  public static final String JSON_VERSION = "version";
  public static final String JSON_SEATID = "seatid";
  public static final String JSON_SOURCEID = "sourceid";
  public static final String JSON_DATA = "data";
  public static final String JSON_ID = "id";
  public static final String JSON_ITEM = "item";

  /** The initial envelope layout version */
  public static final int JSON_VERSION_1 = 1;

  /** The envelope layout version used when writing */
  public static final int JSON_VERSION_CURRENT = JSON_VERSION_1;

  private PeppolReportingEnvelopeJsonHelper ()
  {}

  /**
   * Convert a single {@link PeppolReportingItemWithID} to a JSON object, as used inside the
   * {@value #JSON_DATA} array of the envelope.
   *
   * @param aValue
   *        The reporting item to be converted. May not be <code>null</code>.
   * @return The created JSON object and never <code>null</code>.
   */
  @NonNull
  public static IJsonObject toJson (@NonNull final PeppolReportingItemWithID aValue)
  {
    ValueEnforcer.notNull (aValue, "Value");

    return new JsonObject ().add (JSON_ID, aValue.getItemID ())
                            .add (JSON_ITEM, PeppolReportingJsonHelper.toJson (aValue.getReportingItem ()));
  }

  /**
   * Convert a {@link PeppolReportingEnvelope} to a JSON object.
   *
   * @param aValue
   *        The envelope to be converted. May not be <code>null</code>.
   * @return The created JSON object and never <code>null</code>.
   */
  @NonNull
  public static IJsonObject toJson (@NonNull final PeppolReportingEnvelope aValue)
  {
    ValueEnforcer.notNull (aValue, "Value");

    final IJsonArray aData = new JsonArray ();
    for (final PeppolReportingItemWithID aItem : aValue.getAllItems ())
      aData.add (toJson (aItem));

    return new JsonObject ().add (JSON_VERSION, JSON_VERSION_CURRENT)
                            .add (JSON_SEATID, aValue.getSeatID ())
                            .add (JSON_SOURCEID, aValue.getSourceID ())
                            .add (JSON_DATA, aData);
  }

  /**
   * Convert a {@link PeppolReportingEnvelope} to a JSON string, ready to be transferred over the
   * wire.
   *
   * @param aValue
   *        The envelope to be converted. May not be <code>null</code>.
   * @return The created JSON string and never <code>null</code>.
   * @see #toJson(PeppolReportingEnvelope)
   */
  @NonNull
  public static String toJsonString (@NonNull final PeppolReportingEnvelope aValue)
  {
    return toJson (aValue).getAsJsonString ();
  }

  /**
   * Convert a single JSON object of the {@value #JSON_DATA} array back to a
   * {@link PeppolReportingItemWithID}. This method is public, so that a receiver can convert and
   * validate each item on its own and report the IDs of the invalid items.
   *
   * @param aJson
   *        The JSON object to be converted. May not be <code>null</code>.
   * @return The restored reporting item with its ID. Never <code>null</code>.
   * @throws IllegalArgumentException
   *         if the item ID is missing or invalid, if the reporting item is missing or if its layout
   *         version is not supported
   * @throws IllegalStateException
   *         if the Peppol reporting item is not complete
   */
  @NonNull
  public static PeppolReportingItemWithID toDomainItem (@NonNull final IJsonObject aJson)
  {
    ValueEnforcer.notNull (aJson, "Json");

    final String sItemID = aJson.getAsString (JSON_ID);
    if (StringHelper.isEmpty (sItemID))
      throw new IllegalArgumentException ("The reporting item is missing the field '" + JSON_ID + "'");

    final IJsonObject aItem = aJson.getAsObject (JSON_ITEM);
    if (aItem == null)
      throw new IllegalArgumentException ("The reporting item with ID '" +
                                          sItemID +
                                          "' is missing the object field '" +
                                          JSON_ITEM +
                                          "'");

    return new PeppolReportingItemWithID (sItemID, PeppolReportingJsonHelper.toDomain (aItem));
  }

  /**
   * Convert a JSON object back to a {@link PeppolReportingEnvelope}. This method fails if any of the
   * contained reporting items is invalid.
   *
   * @param aJson
   *        The JSON object to be converted. May not be <code>null</code>.
   * @return The restored envelope. Never <code>null</code>.
   * @throws IllegalArgumentException
   *         if the envelope layout version is not supported or if the envelope or one of the
   *         contained reporting items is invalid
   * @throws IllegalStateException
   *         if one of the contained Peppol reporting items is not complete
   * @see #toDomainItem(IJsonObject)
   */
  @NonNull
  public static PeppolReportingEnvelope toDomain (@NonNull final IJsonObject aJson)
  {
    ValueEnforcer.notNull (aJson, "Json");

    final int nVersion = aJson.getAsInt (JSON_VERSION, -1);
    if (nVersion != JSON_VERSION_1)
      throw new IllegalArgumentException ("The provided JSON object uses the unsupported Peppol Reporting Envelope layout version " +
                                          nVersion);

    final String sSeatID = aJson.getAsString (JSON_SEATID);
    if (StringHelper.isEmpty (sSeatID))
      throw new IllegalArgumentException ("The envelope is missing the field '" + JSON_SEATID + "'");

    final String sSourceID = aJson.getAsString (JSON_SOURCEID);
    if (StringHelper.isEmpty (sSourceID))
      throw new IllegalArgumentException ("The envelope is missing the field '" + JSON_SOURCEID + "'");

    final IJsonArray aData = aJson.getAsArray (JSON_DATA);
    if (aData == null || aData.isEmpty ())
      throw new IllegalArgumentException ("The envelope is missing the non-empty array field '" + JSON_DATA + "'");
    if (aData.size () > PeppolReportingEnvelope.MAX_ITEMS)
      throw new IllegalArgumentException ("The envelope contains " +
                                          aData.size () +
                                          " items but at most " +
                                          PeppolReportingEnvelope.MAX_ITEMS +
                                          " are allowed");

    final ICommonsList <PeppolReportingItemWithID> aItems = new CommonsArrayList <> ();
    for (final IJson aEntry : aData)
    {
      if (!aEntry.isObject ())
        throw new IllegalArgumentException ("The array field '" + JSON_DATA + "' may only contain objects");
      aItems.add (toDomainItem (aEntry.getAsObject ()));
    }

    return new PeppolReportingEnvelope (sSeatID, sSourceID, aItems);
  }

  /**
   * Convert a JSON string back to a {@link PeppolReportingEnvelope}.
   *
   * @param sJson
   *        The JSON string to be converted. May be <code>null</code>.
   * @return <code>null</code> if the provided String is not a JSON object.
   * @throws IllegalArgumentException
   *         if the envelope layout version is not supported or if the envelope or one of the
   *         contained reporting items is invalid
   * @throws IllegalStateException
   *         if one of the contained Peppol reporting items is not complete
   * @see #toDomain(IJsonObject)
   */
  @Nullable
  public static PeppolReportingEnvelope toDomain (@Nullable final String sJson)
  {
    if (sJson == null)
      return null;

    final IJson aJson = JsonReader.readFromString (sJson);
    if (aJson == null || !aJson.isObject ())
      return null;

    return toDomain (aJson.getAsObject ());
  }
}
