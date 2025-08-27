/*
 * Copyright (C) 2022-2025 Philip Helger
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.collection.commons.CommonsHashSet;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsSet;
import com.helger.io.resource.ClassPathResource;
import com.helger.peppol.reporting.jaxb.eusr.EndUserStatisticsReport110Marshaller;
import com.helger.peppol.reporting.testfiles.CReportingTestFiles;
import com.helger.peppol.reporting.testfiles.EUSRTestHelper;
import com.helger.schematron.svrl.AbstractSVRLMessage;
import com.helger.schematron.svrl.SVRLHelper;
import com.helger.schematron.svrl.SVRLMarshaller;
import com.helger.schematron.svrl.jaxb.SchematronOutputType;

import jakarta.annotation.Nonnull;

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
    for (final ClassPathResource f : EUSRTestHelper.getAllGoodFiles ())
    {
      final SchematronOutputType aSVRL = EndUserStatisticsReportValidator.getSchematronEUSR_11 ()
                                                                         .applySchematronValidationToSVRL (f);
      assertNotNull (aSVRL);

      if (false)
        LOGGER.info (new SVRLMarshaller ().getAsString (aSVRL));

      final ICommonsList <AbstractSVRLMessage> aList = SVRLHelper.getAllFailedAssertionsAndSuccessfulReports (aSVRL);
      assertEquals ("Found errors: " + aList.toString (), 0, aList.size ());
    }
  }

  @Test
  public void testSpecificEUSRGoodCase () throws Exception
  {
    final ClassPathResource f = EUSRTestHelper.getAllGoodFiles ()
                                              .findFirst (x -> x.getPath ().endsWith ("eusr-in-the-wild-1.xml"));

    final SchematronOutputType aSVRL = EndUserStatisticsReportValidator.getSchematronEUSR_11 ()
                                                                       .applySchematronValidationToSVRL (f);
    assertNotNull (aSVRL);

    if (false)
      LOGGER.info (new SVRLMarshaller ().getAsString (aSVRL));

    final ICommonsList <AbstractSVRLMessage> aList = SVRLHelper.getAllFailedAssertionsAndSuccessfulReports (aSVRL);
    assertEquals ("Found errors: " + aList.toString (), 0, aList.size ());
  }

  @Nonnull
  private static ICommonsSet <String> _getAllFailedIDs (@Nonnull final String sFilename) throws Exception
  {
    final ClassPathResource f = new ClassPathResource ("external/eusr/bad/" + sFilename,
                                                       CReportingTestFiles.getTestClassLoader ());

    // Ensure correct according to XSD
    assertNotNull ("Failed to read " + sFilename, new EndUserStatisticsReport110Marshaller ().read (f));

    final SchematronOutputType aSVRL = EndUserStatisticsReportValidator.getSchematronEUSR_11 ()
                                                                       .applySchematronValidationToSVRL (f);
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

    assertTrue (_checkFailedID ("eusr-22-1.xml", "SCH-EUSR-22"));

    assertTrue (_checkFailedID ("eusr-23-1.xml", "SCH-EUSR-23"));

    assertTrue (_checkFailedID ("eusr-24-1.xml", "SCH-EUSR-24"));
    assertTrue (_checkFailedID ("eusr-24-2.xml", "SCH-EUSR-24"));

    assertTrue (_checkFailedID ("eusr-25-1.xml", "SCH-EUSR-25"));
    assertTrue (_checkFailedID ("eusr-25-2.xml", "SCH-EUSR-25"));

    if (false)
      assertTrue (_checkFailedID ("eusr-26-1.xml", "SCH-EUSR-26"));
    assertTrue (_checkFailedID ("eusr-26-2.xml", "SCH-EUSR-26"));

    assertTrue (_checkFailedID ("eusr-27-1.xml", "SCH-EUSR-27"));
    assertTrue (_checkFailedID ("eusr-27-2.xml", "SCH-EUSR-27"));

    assertTrue (_checkFailedID ("eusr-29-1.xml", "SCH-EUSR-29"));

    assertTrue (_checkFailedID ("eusr-30-1.xml", "SCH-EUSR-30"));
    assertTrue (_checkFailedID ("eusr-30-2.xml", "SCH-EUSR-30"));

    assertTrue (_checkFailedID ("eusr-33-1.xml", "SCH-EUSR-33"));

    assertTrue (_checkFailedID ("eusr-34-1.xml", "SCH-EUSR-34"));

    assertTrue (_checkFailedID ("eusr-35-1.xml", "SCH-EUSR-35"));

    assertTrue (_checkFailedID ("eusr-36-1.xml", "SCH-EUSR-36"));

    assertTrue (_checkFailedID ("eusr-37-1.xml", "SCH-EUSR-37"));

    assertTrue (_checkFailedID ("eusr-38-1.xml", "SCH-EUSR-38"));
    assertTrue (_checkFailedID ("eusr-38-2.xml", "SCH-EUSR-38"));

    assertTrue (_checkFailedID ("eusr-39-1.xml", "SCH-EUSR-39"));

    assertTrue (_checkFailedID ("eusr-40-1.xml", "SCH-EUSR-40"));
    assertTrue (_checkFailedID ("eusr-40-2.xml", "SCH-EUSR-40"));

    assertTrue (_checkFailedID ("eusr-41-1.xml", "SCH-EUSR-41"));
    assertTrue (_checkFailedID ("eusr-41-2.xml", "SCH-EUSR-41"));

    assertTrue (_checkFailedID ("eusr-42-1.xml", "SCH-EUSR-42"));
    assertTrue (_checkFailedID ("eusr-42-2.xml", "SCH-EUSR-42"));

    assertTrue (_checkFailedID ("eusr-43-1.xml", "SCH-EUSR-43"));
    assertTrue (_checkFailedID ("eusr-43-2.xml", "SCH-EUSR-43"));

    assertTrue (_checkFailedID ("eusr-44-1.xml", "SCH-EUSR-44"));
    assertTrue (_checkFailedID ("eusr-44-2.xml", "SCH-EUSR-44"));

    assertTrue (_checkFailedID ("eusr-45-1.xml", "SCH-EUSR-45"));

    assertTrue (_checkFailedID ("eusr-46-1.xml", "SCH-EUSR-46"));
    assertTrue (_checkFailedID ("eusr-46-2.xml", "SCH-EUSR-46"));

    assertTrue (_checkFailedID ("eusr-47-1.xml", "SCH-EUSR-47"));
    assertTrue (_checkFailedID ("eusr-47-2.xml", "SCH-EUSR-47"));
  }
}
