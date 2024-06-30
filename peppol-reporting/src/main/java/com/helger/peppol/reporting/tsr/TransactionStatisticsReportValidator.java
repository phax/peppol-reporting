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
package com.helger.peppol.reporting.tsr;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.commons.exception.InitializationException;
import com.helger.schematron.ISchematronResource;
import com.helger.schematron.sch.SchematronResourceSCH;

/**
 * This class can be used to trigger Schematron validation of TSR documents.
 *
 * @author Philip Helger
 */
@NotThreadSafe
public final class TransactionStatisticsReportValidator
{
  public static final String SCH_TSR_105_PATH = "external/schematron/peppol-transaction-statistics-reporting-1.0.5.sch";

  private static final ISchematronResource SCH_TSR_105 = SchematronResourceSCH.fromClassPath (SCH_TSR_105_PATH);

  static
  {
    for (final ISchematronResource aSch : new ISchematronResource [] { SCH_TSR_105 })
      if (!aSch.isValidSchematron ())
        throw new InitializationException ("Schematron in " + aSch.getResource ().getPath () + " is invalid");
  }

  private TransactionStatisticsReportValidator ()
  {}

  /**
   * @return Schematron TSR v1.0.5
   * @since 2.2.3
   */
  @Nonnull
  public static ISchematronResource getSchematronTSR_105 ()
  {
    return SCH_TSR_105;
  }

  /**
   * @return Schematron TSR v1.0 latest micro version
   * @since 2.1.3
   */
  @Nonnull
  public static ISchematronResource getSchematronTSR_10 ()
  {
    return getSchematronTSR_105 ();
  }

  /**
   * @return Schematron TSR v1 latest minor and micro version
   * @since 2.1.3
   */
  @Nonnull
  public static ISchematronResource getSchematronTSR_1 ()
  {
    return getSchematronTSR_10 ();
  }
}
