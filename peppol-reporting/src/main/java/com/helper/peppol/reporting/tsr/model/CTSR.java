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
package com.helper.peppol.reporting.tsr.model;

import javax.annotation.concurrent.Immutable;

/**
 * Constants for TSR
 *
 * @author Philip Helger
 */
@Immutable
public final class CTSR
{
  public static final String TSR_METASCHEME_TP = "TP";
  public static final String TSR_METASCHEME_SP = "SP";
  public static final String TSR_METASCHEME_DT = "DT";
  public static final String TSR_METASCHEME_PR = "PR";
  public static final String TSR_METASCHEME_CC = "CC";

  public static final String TSR_SCHEME_TP_PEPPOL = "Peppol";
  public static final String TSR_SCHEME_CC_SENDER_COUNTRY = "SenderCountry";
  public static final String TSR_SCHEME_CC_RECEIVER_COUNTRY = "ReceiverCountry";

  private CTSR ()
  {}
}
