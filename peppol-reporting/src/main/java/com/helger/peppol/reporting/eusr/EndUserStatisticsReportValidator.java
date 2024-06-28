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
package com.helger.peppol.reporting.eusr;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.commons.exception.InitializationException;
import com.helger.schematron.ISchematronResource;
import com.helger.schematron.sch.SchematronResourceSCH;

/**
 * This class can be used to trigger Schematron validation of EUSR documents.
 *
 * @author Philip Helger
 */
@NotThreadSafe
public final class EndUserStatisticsReportValidator
{
  @Deprecated (forRemoval = true, since = "2.2.3")
  public static final String SCH_EUSR_114_PATH = "external/schematron/peppol-end-user-statistics-reporting-1.1.4.sch";
  public static final String SCH_EUSR_115_PATH = "external/schematron/peppol-end-user-statistics-reporting-1.1.5.sch";

  @Deprecated (forRemoval = true, since = "2.2.3")
  private static final ISchematronResource SCH_EUSR_114 = SchematronResourceSCH.fromClassPath (SCH_EUSR_114_PATH);
  private static final ISchematronResource SCH_EUSR_115 = SchematronResourceSCH.fromClassPath (SCH_EUSR_115_PATH);

  static
  {
    for (final ISchematronResource aSch : new ISchematronResource [] { SCH_EUSR_114, SCH_EUSR_115 })
      if (!aSch.isValidSchematron ())
        throw new InitializationException ("Schematron in " + aSch.getResource ().getPath () + " is invalid");
  }

  private EndUserStatisticsReportValidator ()
  {}

  /**
   * @return Schematron EUSR v1.1.4
   * @since 2.1.6
   */
  @Nonnull
  @Deprecated (forRemoval = true, since = "2.2.3")
  public static ISchematronResource getSchematronEUSR_114 ()
  {
    return SCH_EUSR_114;
  }

  /**
   * @return Schematron EUSR v1.1.5
   * @since 2.2.3
   */
  @Nonnull
  public static ISchematronResource getSchematronEUSR_115 ()
  {
    return SCH_EUSR_115;
  }

  /**
   * @return Schematron EUSR v1.1 latest micro version
   * @since 2.1.3
   */
  @Nonnull
  public static ISchematronResource getSchematronEUSR_11 ()
  {
    return getSchematronEUSR_115 ();
  }

  /**
   * @return Schematron EUSR v1 latest minor and micro version
   * @since 2.1.3
   */
  @Nonnull
  public static ISchematronResource getSchematronEUSR_1 ()
  {
    return getSchematronEUSR_11 ();
  }
}
