package com.helper.peppol.reporting.eusr;

import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.junit.Test;

/**
 * Test class for class {@link EndUserStatisticsReportMarshaller}.
 *
 * @author Philip Helger
 */
public final class EndUserStatisticsReportMarshallerTest
{
  @Test
  public void testEUSRGoodCases () throws Exception
  {
    final EndUserStatisticsReportMarshaller m = new EndUserStatisticsReportMarshaller ();
    for (final File f : EUSRTestHelper.getAllGoodFiles ())
    {
      // Read and XSD validate
      assertNotNull ("The file " + f.getAbsolutePath () + " is not XSD compliant", m.read (f));
    }
  }
}
