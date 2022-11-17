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
