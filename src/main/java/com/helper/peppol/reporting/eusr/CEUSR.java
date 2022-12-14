/*
 * Copyright (C) 2022-2023 Philip Helger
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
package com.helper.peppol.reporting.eusr;

import javax.annotation.concurrent.Immutable;

/**
 * Constants for EUSR
 *
 * @author Philip Helger
 */
@Immutable
public final class CEUSR
{
  /** The v1.0 Customization ID to be used */
  public static final String CUSTOMIZATION_ID_V10 = "urn:fdc:peppol.eu:edec:trns:end-user-statistics-report:1.0";

  /** The v1.0 Profile ID to be used */
  public static final String PROFILE_ID_V10 = "urn:fdc:peppol.eu:edec:bis:reporting:1.0";

  private CEUSR ()
  {}
}
