/*
 * Copyright (C) 2022 Philip Helger
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

@NotThreadSafe
public final class TransactionStatisticsReportValidator
{
  public static final String SCH_TSR_100_PATH = "schematron/peppol-transaction-statistics-reporting-1.0.0.sch";

  private static final ISchematronResource SCH_TSR_100 = SchematronResourceSCH.fromClassPath (SCH_TSR_100_PATH);

  static
  {
    if (!SCH_TSR_100.isValidSchematron ())
      throw new InitializationException ("Schematron in " + SCH_TSR_100.getResource ().getPath () + " is invalid");
  }

  private TransactionStatisticsReportValidator ()
  {}

  @Nonnull
  public static ISchematronResource getSchematronTSR_100 ()
  {
    return SCH_TSR_100;
  }
}
