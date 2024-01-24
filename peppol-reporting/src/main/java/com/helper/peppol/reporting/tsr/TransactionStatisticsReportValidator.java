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
package com.helper.peppol.reporting.tsr;

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
  public static final String SCH_TSR_104_PATH = "external/schematron/peppol-transaction-statistics-reporting-1.0.4.sch";

  private static final ISchematronResource SCH_TSR_104 = SchematronResourceSCH.fromClassPath (SCH_TSR_104_PATH);

  static
  {
    for (final ISchematronResource aSch : new ISchematronResource [] { SCH_TSR_104 })
      if (!aSch.isValidSchematron ())
        throw new InitializationException ("Schematron in " + aSch.getResource ().getPath () + " is invalid");
  }

  private TransactionStatisticsReportValidator ()
  {}

  /**
   * @return Schematron TSR v1.0.1
   * @deprecated Use latest version instead
   */
  @Nonnull
  @Deprecated (forRemoval = true, since = "2.1.3")
  public static ISchematronResource getSchematronTSR_101 ()
  {
    return SCH_TSR_104;
  }

  /**
   * @return Schematron TSR v1.0.2
   * @since 2.1.3
   */
  @Nonnull
  @Deprecated (forRemoval = true, since = "2.1.4")
  public static ISchematronResource getSchematronTSR_102 ()
  {
    return SCH_TSR_104;
  }

  /**
   * @return Schematron TSR v1.0.3
   * @since 2.1.4
   */
  @Nonnull
  @Deprecated (forRemoval = true, since = "2.1.5")
  public static ISchematronResource getSchematronTSR_103 ()
  {
    return SCH_TSR_104;
  }

  /**
   * @return Schematron TSR v1.0.4
   * @since 2.1.5
   */
  @Nonnull
  public static ISchematronResource getSchematronTSR_104 ()
  {
    return SCH_TSR_104;
  }

  /**
   * @return Schematron TSR v1.0 latest micro version
   * @since 2.1.3
   */
  @Nonnull
  public static ISchematronResource getSchematronTSR_10 ()
  {
    return SCH_TSR_104;
  }

  /**
   * @return Schematron TSR v1 latest minor and micro version
   * @since 2.1.3
   */
  @Nonnull
  public static ISchematronResource getSchematronTSR_1 ()
  {
    return SCH_TSR_104;
  }
}
