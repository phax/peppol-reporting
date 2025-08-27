/*
 * Copyright (C) 2022-2025 Philip Helger
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
package com.helger.peppol.reporting.eusr.model;

import com.helger.annotation.concurrent.Immutable;

/**
 * Constants for EUSR - End User Statistics Reporting
 *
 * @author Philip Helger
 */
@Immutable
public final class CEUSR
{

  public static final String EUSR_METASCHEME_DT = "DT";
  public static final String EUSR_METASCHEME_PR = "PR";
  public static final String EUSR_METASCHEME_CC = "CC";

  public static final String EUSR_SCHEME_CC_END_USER_COUNTRY = "EndUserCountry";

  private CEUSR ()
  {}
}
