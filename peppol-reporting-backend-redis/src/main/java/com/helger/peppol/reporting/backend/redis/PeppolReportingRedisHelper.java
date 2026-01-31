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
package com.helger.peppol.reporting.backend.redis;

import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.collection.commons.CommonsHashMap;
import com.helger.datetime.format.PDTFromString;
import com.helger.peppol.reporting.api.EReportingDirection;
import com.helger.peppol.reporting.api.PeppolReportingItem;

@Immutable
public final class PeppolReportingRedisHelper
{
  public static final String KEY_EXCHANGEDT = "exchangedt";
  public static final String KEY_EXCHANGEDATE = "exchangedate";
  public static final String KEY_DIRECTION = "direction";
  public static final String KEY_C2ID = "c2id";
  public static final String KEY_C3ID = "c3id";
  public static final String KEY_DTIDSCHEME = "dtidscheme";
  public static final String KEY_DTIDVALUE = "dtidvalue";
  public static final String KEY_PROCIDSCHEME = "procidscheme";
  public static final String KEY_PROCIDVALUE = "procidvalue";
  public static final String KEY_TRANSPORTID = "transportid";
  public static final String KEY_C1CC = "c1cc";
  public static final String KEY_C4CC = "c4cc";
  public static final String KEY_ENDUSERID = "enduserid";

  private PeppolReportingRedisHelper ()
  {}

  /**
   * Convert a {@link PeppolReportingItem} to a String-String map.
   *
   * @param aValue
   *        The Reporting item to be converted. May not be <code>null</code>.
   * @return The created Map and never <code>null</code>.
   */
  @NonNull
  public static Map <String, String> toMap (@NonNull final PeppolReportingItem aValue)
  {
    ValueEnforcer.notNull (aValue, "Value");

    final Map <String, String> ret = new CommonsHashMap <> ();
    ret.put (KEY_EXCHANGEDT,
             DateTimeFormatter.ISO_LOCAL_DATE_TIME.format (aValue.getExchangeDTUTC ().toLocalDateTime ()));
    ret.put (KEY_EXCHANGEDATE, DateTimeFormatter.ISO_LOCAL_DATE.format (aValue.getExchangeDTUTC ().toLocalDate ()));
    ret.put (KEY_DIRECTION, aValue.getDirection ().getID ());
    ret.put (KEY_C2ID, aValue.getC2ID ());
    ret.put (KEY_C3ID, aValue.getC3ID ());
    ret.put (KEY_DTIDSCHEME, aValue.getDocTypeIDScheme ());
    ret.put (KEY_DTIDVALUE, aValue.getDocTypeIDValue ());
    ret.put (KEY_PROCIDSCHEME, aValue.getProcessIDScheme ());
    ret.put (KEY_PROCIDVALUE, aValue.getProcessIDValue ());
    ret.put (KEY_TRANSPORTID, aValue.getTransportProtocol ());
    ret.put (KEY_C1CC, aValue.getC1CountryCode ());
    if (aValue.hasC4CountryCode ())
      ret.put (KEY_C4CC, aValue.getC4CountryCode ());
    ret.put (KEY_ENDUSERID, aValue.getEndUserID ());
    return ret;
  }

  /**
   * Convert a Map back to a {@link PeppolReportingItem}.
   *
   * @param aDoc
   *        The Map to be converted. May not be <code>null</code>.
   * @return The restored Peppol reporting item
   * @throws IllegalStateException
   *         if the Peppol reporting item is not complete
   */
  @NonNull
  public static PeppolReportingItem toDomain (@NonNull final Map <String, String> aDoc)
  {
    ValueEnforcer.notNull (aDoc, "Doc");

    return PeppolReportingItem.builder ()
                              .exchangeDateTimeInUTC (PDTFromString.getLocalDateTimeFromString (aDoc.get (KEY_EXCHANGEDT),
                                                                                                DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                              .direction (EReportingDirection.getFromIDOrThrow (aDoc.get (KEY_DIRECTION)))
                              .c2ID (aDoc.get (KEY_C2ID))
                              .c3ID (aDoc.get (KEY_C3ID))
                              .docTypeIDScheme (aDoc.get (KEY_DTIDSCHEME))
                              .docTypeIDValue (aDoc.get (KEY_DTIDVALUE))
                              .processIDScheme (aDoc.get (KEY_PROCIDSCHEME))
                              .processIDValue (aDoc.get (KEY_PROCIDVALUE))
                              .transportProtocol (aDoc.get (KEY_TRANSPORTID))
                              .c1CountryCode (aDoc.get (KEY_C1CC))
                              .c4CountryCode (aDoc.get (KEY_C4CC))
                              .endUserID (aDoc.get (KEY_ENDUSERID))
                              .build ();
  }
}
