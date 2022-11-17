package com.helper.peppol.reporting.eusr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import javax.annotation.Nonnull;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.collection.impl.CommonsHashSet;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsSet;
import com.helger.commons.io.resource.FileSystemResource;
import com.helger.schematron.svrl.AbstractSVRLMessage;
import com.helger.schematron.svrl.SVRLHelper;
import com.helger.schematron.svrl.SVRLMarshaller;
import com.helger.schematron.svrl.jaxb.SchematronOutputType;

/**
 * Test class for class {@link EndUserStatisticsReportValidator}.
 *
 * @author Philip Helger
 */
public final class EndUserStatisticsReportValidatorTest
{
  private static final Logger LOGGER = LoggerFactory.getLogger (EndUserStatisticsReportValidatorTest.class);

  @Test
  public void testEUSRGoodCases () throws Exception
  {
    for (final File f : EUSRTestHelper.getAllGoodFiles ())
    {
      final SchematronOutputType aSVRL = EndUserStatisticsReportValidator.getSchematronEUSR_100RC2 ()
                                                                         .applySchematronValidationToSVRL (new FileSystemResource (f));
      assertNotNull (aSVRL);

      if (false)
        LOGGER.info (new SVRLMarshaller ().getAsString (aSVRL));

      final ICommonsList <AbstractSVRLMessage> aList = SVRLHelper.getAllFailedAssertionsAndSuccessfulReports (aSVRL);
      assertEquals ("Found errors: " + aList.toString (), 0, aList.size ());
    }
  }

  @Nonnull
  private static ICommonsSet <String> _getAllFailedIDs (@Nonnull final String sFilename) throws Exception
  {
    final File f = new File ("src/test/resources/eusr/bad/" + sFilename);

    // Ensure correct according to XSD
    assertNotNull ("Failed to read " + sFilename, new EndUserStatisticsReportMarshaller ().read (f));

    final SchematronOutputType aSVRL = EndUserStatisticsReportValidator.getSchematronEUSR_100RC2 ()
                                                                       .applySchematronValidationToSVRL (new FileSystemResource (f));
    assertNotNull (aSVRL);

    if (false)
      LOGGER.info (new SVRLMarshaller ().getAsString (aSVRL));

    final ICommonsList <AbstractSVRLMessage> aList = SVRLHelper.getAllFailedAssertionsAndSuccessfulReports (aSVRL);
    assertFalse ("Found no errors in " + sFilename, aList.isEmpty ());

    return new CommonsHashSet <> (aList, AbstractSVRLMessage::getID);
  }

  private static boolean _checkFailedID (@Nonnull final String sFilename, final String sExpected) throws Exception
  {
    final ICommonsSet <String> aFailed = _getAllFailedIDs (sFilename);
    final boolean bRet = aFailed.contains (sExpected);
    assertTrue ("Expected " + sExpected + " but got " + aFailed, bRet);
    return bRet;
  }

  @Test
  public void testEURBadCases () throws Exception
  {
    assertTrue (_checkFailedID ("eusr-01-1.xml", "SCH-EUSR-01"));
    assertTrue (_checkFailedID ("eusr-01-2.xml", "SCH-EUSR-01"));

    assertTrue (_checkFailedID ("eusr-02-1.xml", "SCH-EUSR-02"));
    assertTrue (_checkFailedID ("eusr-02-2.xml", "SCH-EUSR-02"));

    assertTrue (_checkFailedID ("eusr-03-1.xml", "SCH-EUSR-03"));

    assertTrue (_checkFailedID ("eusr-04-1.xml", "SCH-EUSR-04"));

    assertTrue (_checkFailedID ("eusr-06-1.xml", "SCH-EUSR-06"));

    assertTrue (_checkFailedID ("eusr-07-1.xml", "SCH-EUSR-07"));
    assertTrue (_checkFailedID ("eusr-07-2.xml", "SCH-EUSR-07"));

    assertTrue (_checkFailedID ("eusr-08-1.xml", "SCH-EUSR-08"));
    assertTrue (_checkFailedID ("eusr-08-2.xml", "SCH-EUSR-08"));

    assertTrue (_checkFailedID ("eusr-09-1.xml", "SCH-EUSR-09"));
    assertTrue (_checkFailedID ("eusr-09-2.xml", "SCH-EUSR-09"));
    assertTrue (_checkFailedID ("eusr-09-3.xml", "SCH-EUSR-09"));

    assertTrue (_checkFailedID ("eusr-10-1.xml", "SCH-EUSR-10"));
    assertTrue (_checkFailedID ("eusr-10-2.xml", "SCH-EUSR-10"));

    assertTrue (_checkFailedID ("eusr-11-1.xml", "SCH-EUSR-11"));
    assertTrue (_checkFailedID ("eusr-11-2.xml", "SCH-EUSR-11"));

    assertTrue (_checkFailedID ("eusr-13-1.xml", "SCH-EUSR-13"));

    assertTrue (_checkFailedID ("eusr-14-1.xml", "SCH-EUSR-14"));

    assertTrue (_checkFailedID ("eusr-15-1.xml", "SCH-EUSR-15"));

    assertTrue (_checkFailedID ("eusr-16-1.xml", "SCH-EUSR-16"));

    assertTrue (_checkFailedID ("eusr-17-1.xml", "SCH-EUSR-17"));

    assertTrue (_checkFailedID ("eusr-18-1.xml", "SCH-EUSR-18"));

    assertTrue (_checkFailedID ("eusr-19-1.xml", "SCH-EUSR-19"));

    assertTrue (_checkFailedID ("eusr-20-1.xml", "SCH-EUSR-20"));

    assertTrue (_checkFailedID ("eusr-21-1.xml", "SCH-EUSR-21"));
  }
}
