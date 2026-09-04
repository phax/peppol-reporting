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
package com.helger.peppol.reporting.api;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.json.IJson;
import com.helger.json.IJsonObject;
import com.helger.json.JsonObject;
import com.helger.json.serialize.JsonReader;

/**
 * The default JSON serialization of a {@link PeppolReportingItem}, to be used
 * to transfer Peppol Reporting raw data over the wire.<br>
 * Each created JSON object contains the field {@value #JSON_VERSION} with the
 * value {@link #JSON_VERSION_1}, so that the layout can be extended in a
 * backwards compatible way later on.
 *
 * @author Philip Helger
 * @since 4.3.0
 */
@Immutable
public final class PeppolReportingJsonHelper
{
  /** The name of the JSON field containing the layout version */
  public static final String JSON_VERSION = "version";
  public static final String JSON_EXCHANGEDT = "exchangedt";
  public static final String JSON_DIRECTION = "direction";
  public static final String JSON_C2ID = "c2id";
  public static final String JSON_C3ID = "c3id";
  public static final String JSON_DTIDSCHEME = "dtidscheme";
  public static final String JSON_DTIDVALUE = "dtidvalue";
  public static final String JSON_PROCIDSCHEME = "procidscheme";
  public static final String JSON_PROCIDVALUE = "procidvalue";
  public static final String JSON_TRANSPORTID = "transportid";
  public static final String JSON_C1CC = "c1cc";
  public static final String JSON_C4CC = "c4cc";
  public static final String JSON_ENDUSERID = "enduserid";

  /** The initial layout version */
  public static final int JSON_VERSION_1 = 1;

  /** The layout version used when writing */
  public static final int JSON_VERSION_CURRENT = JSON_VERSION_1;

  /** The date time format used for the exchange date time */
  private static final DateTimeFormatter DTF = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

  private PeppolReportingJsonHelper ()
  {}

  /**
   * Convert a {@link PeppolReportingItem} to a JSON object.
   *
   * @param aValue
   *        The Reporting item to be converted. May not be <code>null</code>.
   * @return The created JSON object and never <code>null</code>.
   */
  @NonNull
  public static IJsonObject toJson (@NonNull final PeppolReportingItem aValue)
  {
    ValueEnforcer.notNull (aValue, "Value");

    final IJsonObject ret = new JsonObject ().add (JSON_VERSION, JSON_VERSION_CURRENT)
                                             .add (JSON_EXCHANGEDT, DTF.format (aValue.getExchangeDTUTC ()))
                                             .add (JSON_DIRECTION, aValue.getDirection ().getID ())
                                             .add (JSON_C2ID, aValue.getC2ID ())
                                             .add (JSON_C3ID, aValue.getC3ID ())
                                             .add (JSON_DTIDSCHEME, aValue.getDocTypeIDScheme ())
                                             .add (JSON_DTIDVALUE, aValue.getDocTypeIDValue ())
                                             .add (JSON_PROCIDSCHEME, aValue.getProcessIDScheme ())
                                             .add (JSON_PROCIDVALUE, aValue.getProcessIDValue ())
                                             .add (JSON_TRANSPORTID, aValue.getTransportProtocol ())
                                             .add (JSON_C1CC, aValue.getC1CountryCode ());
    // Only present for receiving items
    if (aValue.hasC4CountryCode ())
      ret.add (JSON_C4CC, aValue.getC4CountryCode ());
    ret.add (JSON_ENDUSERID, aValue.getEndUserID ());
    return ret;
  }

  /**
   * Convert a {@link PeppolReportingItem} to a JSON string, ready to be
   * transferred over the wire.
   *
   * @param aValue
   *        The Reporting item to be converted. May not be <code>null</code>.
   * @return The created JSON string and never <code>null</code>.
   * @see #toJson(PeppolReportingItem)
   */
  @NonNull
  public static String toJsonString (@NonNull final PeppolReportingItem aValue)
  {
    return toJson (aValue).getAsJsonString ();
  }

  /**
   * Convert a JSON object back to a {@link PeppolReportingItem}.
   *
   * @param aJson
   *        The JSON object to be converted. May not be <code>null</code>.
   * @return The restored Peppol reporting item. Never <code>null</code>.
   * @throws IllegalArgumentException
   *         if the contained layout version is not supported
   * @throws IllegalStateException
   *         if the Peppol reporting item is not complete
   */
  @NonNull
  public static PeppolReportingItem toDomain (@NonNull final IJsonObject aJson)
  {
    ValueEnforcer.notNull (aJson, "Json");

    final int nVersion = aJson.getAsInt (JSON_VERSION, -1);
    if (nVersion != JSON_VERSION_1)
      throw new IllegalArgumentException ("The provided JSON object uses the unsupported Peppol Reporting Item layout version " +
                                          nVersion);

    return PeppolReportingItem.builder ()
                              .exchangeDateTime (OffsetDateTime.parse (aJson.getAsString (JSON_EXCHANGEDT), DTF))
                              .direction (EReportingDirection.getFromIDOrThrow (aJson.getAsString (JSON_DIRECTION)))
                              .c2ID (aJson.getAsString (JSON_C2ID))
                              .c3ID (aJson.getAsString (JSON_C3ID))
                              .docTypeIDScheme (aJson.getAsString (JSON_DTIDSCHEME))
                              .docTypeIDValue (aJson.getAsString (JSON_DTIDVALUE))
                              .processIDScheme (aJson.getAsString (JSON_PROCIDSCHEME))
                              .processIDValue (aJson.getAsString (JSON_PROCIDVALUE))
                              .transportProtocol (aJson.getAsString (JSON_TRANSPORTID))
                              .c1CountryCode (aJson.getAsString (JSON_C1CC))
                              .c4CountryCode (aJson.getAsString (JSON_C4CC))
                              .endUserID (aJson.getAsString (JSON_ENDUSERID))
                              .build ();
  }

  /**
   * Convert a JSON string back to a {@link PeppolReportingItem}.
   *
   * @param sJson
   *        The JSON string to be converted. May be <code>null</code>.
   * @return <code>null</code> if the provided String is not a JSON object.
   * @throws IllegalArgumentException
   *         if the contained layout version is not supported
   * @throws IllegalStateException
   *         if the Peppol reporting item is not complete
   * @see #toDomain(IJsonObject)
   */
  @Nullable
  public static PeppolReportingItem toDomain (@Nullable final String sJson)
  {
    if (sJson == null)
      return null;

    final IJson aJson = JsonReader.readFromString (sJson);
    if (aJson == null || !aJson.isObject ())
      return null;

    return toDomain (aJson.getAsObject ());
  }
}
