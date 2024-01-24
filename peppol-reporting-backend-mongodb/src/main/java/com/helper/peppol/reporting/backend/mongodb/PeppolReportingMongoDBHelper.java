/*
 * Copyright (C) 2022-2024 Philip Helger
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
package com.helper.peppol.reporting.backend.mongodb;

import java.time.OffsetDateTime;
import java.util.Date;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import org.bson.Document;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.typeconvert.TypeConverter;
import com.helper.peppol.reporting.api.EReportingDirection;
import com.helper.peppol.reporting.api.PeppolReportingItem;

@Immutable
public final class PeppolReportingMongoDBHelper
{
  public static final String BSON_EXCHANGEDT = "exchangedt";
  public static final String BSON_EXCHANGEDATE = "exchangedate";
  public static final String BSON_DIRECTION = "direction";
  public static final String BSON_C2ID = "c2id";
  public static final String BSON_C3ID = "c3id";
  public static final String BSON_DTIDSCHEME = "dtidscheme";
  public static final String BSON_DTIDVALUE = "dtidvalue";
  public static final String BSON_PROCIDSCHEME = "procidscheme";
  public static final String BSON_PROCIDVALUE = "procidvalue";
  public static final String BSON_TRANSPORTID = "transportid";
  public static final String BSON_C1CC = "c1cc";
  public static final String BSON_C4CC = "c4cc";
  public static final String BSON_ENDUSERID = "enduserid";

  private PeppolReportingMongoDBHelper ()
  {}

  /**
   * Convert a {@link PeppolReportingItem} to a BSON document.
   *
   * @param aValue
   *        The Reporting item to be converted. May not be <code>null</code>.
   * @return The created BSON document and never <code>null</code>.
   */
  @Nonnull
  public static Document toBson (@Nonnull final PeppolReportingItem aValue)
  {
    ValueEnforcer.notNull (aValue, "Value");

    return new Document ().append (BSON_EXCHANGEDT, TypeConverter.convert (aValue.getExchangeDTUTC (), Date.class))
                          // For selection only
                          .append (BSON_EXCHANGEDATE, aValue.getExchangeDTUTC ().toLocalDate ())
                          .append (BSON_DIRECTION, aValue.getDirection ().getID ())
                          .append (BSON_C2ID, aValue.getC2ID ())
                          .append (BSON_C3ID, aValue.getC3ID ())
                          .append (BSON_DTIDSCHEME, aValue.getDocTypeIDScheme ())
                          .append (BSON_DTIDVALUE, aValue.getDocTypeIDValue ())
                          .append (BSON_PROCIDSCHEME, aValue.getProcessIDScheme ())
                          .append (BSON_PROCIDVALUE, aValue.getProcessIDValue ())
                          .append (BSON_TRANSPORTID, aValue.getTransportProtocol ())
                          .append (BSON_C1CC, aValue.getC1CountryCode ())
                          .append (BSON_C4CC, aValue.getC4CountryCode ())
                          .append (BSON_ENDUSERID, aValue.getEndUserID ());
  }

  /**
   * Convert a BSON document back to a {@link PeppolReportingItem}.
   *
   * @param aDoc
   *        The document to be converted. May not be <code>null</code>.
   * @return The restored Peppol reporting item
   * @throws IllegalStateException
   *         if the Peppol reporting item is not complete
   */
  @Nonnull
  public static PeppolReportingItem toDomain (@Nonnull final Document aDoc)
  {
    ValueEnforcer.notNull (aDoc, "Doc");

    return PeppolReportingItem.builder ()
                              .exchangeDateTime (TypeConverter.convert (aDoc.get (BSON_EXCHANGEDT),
                                                                        OffsetDateTime.class))
                              .direction (EReportingDirection.getFromIDOrThrow (aDoc.getString (BSON_DIRECTION)))
                              .c2ID (aDoc.getString (BSON_C2ID))
                              .c3ID (aDoc.getString (BSON_C3ID))
                              .docTypeIDScheme (aDoc.getString (BSON_DTIDSCHEME))
                              .docTypeIDValue (aDoc.getString (BSON_DTIDVALUE))
                              .processIDScheme (aDoc.getString (BSON_PROCIDSCHEME))
                              .processIDValue (aDoc.getString (BSON_PROCIDVALUE))
                              .transportProtocol (aDoc.getString (BSON_TRANSPORTID))
                              .c1CountryCode (aDoc.getString (BSON_C1CC))
                              .c4CountryCode (aDoc.getString (BSON_C4CC))
                              .endUserID (aDoc.getString (BSON_ENDUSERID))
                              .build ();
  }
}
