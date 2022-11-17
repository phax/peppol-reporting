package com.helper.peppol.reporting.tsr;

import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.junit.Test;

public final class TransactionStatisticsReportMarshallerTest
{
  @Test
  public void testTSRGoodCases () throws Exception
  {
    final TransactionStatisticsReportMarshaller m = new TransactionStatisticsReportMarshaller ();
    for (final File f : TSRTestHelper.getAllGoodFiles ())
    {
      // Read and XSD validate
      assertNotNull ("The file " + f.getAbsolutePath () + " is not XSD compliant", m.read (f));
    }
  }
}
