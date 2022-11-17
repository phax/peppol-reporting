package com.helper.peppol.reporting.eusr;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;

import com.helger.commons.exception.InitializationException;
import com.helger.schematron.ISchematronResource;
import com.helger.schematron.sch.SchematronResourceSCH;

/**
 * This class provides the {@link ISchematronResource} for End User Statistics
 * reports.
 *
 * @author Philip Helger
 */
@NotThreadSafe
public final class EndUserStatisticsReportValidator
{
  public static final String SCH_TSR_100_PATH = "schematron/peppol-end-user-statistics-reporting-1.0.0-RC2.sch";

  private static final ISchematronResource SCH_EUSR_100RC2 = SchematronResourceSCH.fromClassPath (SCH_TSR_100_PATH);

  static
  {
    if (!SCH_EUSR_100RC2.isValidSchematron ())
      throw new InitializationException ("Schematron in " + SCH_EUSR_100RC2.getResource ().getPath () + " is invalid");
  }

  private EndUserStatisticsReportValidator ()
  {}

  @Nonnull
  public static ISchematronResource getSchematronEUSR_100RC2 ()
  {
    return SCH_EUSR_100RC2;
  }
}
