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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import javax.annotation.Nonnull;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.collection.impl.CommonsTreeSet;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsSet;
import com.helger.commons.io.resource.FileSystemResource;
import com.helger.schematron.svrl.AbstractSVRLMessage;
import com.helger.schematron.svrl.SVRLHelper;
import com.helger.schematron.svrl.SVRLMarshaller;
import com.helger.schematron.svrl.jaxb.SchematronOutputType;

/**
 * Test class for class {@link TransactionStatisticsReportValidator}.
 *
 * @author Philip Helger
 */
public final class TransactionStatisticsReportValidatorTest
{
  private static final Logger LOGGER = LoggerFactory.getLogger (TransactionStatisticsReportValidatorTest.class);

  @Test
  public void testTSRGoodCases () throws Exception
  {
    for (final File f : TSRTestHelper.getAllGoodFiles ())
    {
      final SchematronOutputType aSVRL = TransactionStatisticsReportValidator.getSchematronTSR_10 ()
                                                                             .applySchematronValidationToSVRL (new FileSystemResource (f));
      assertNotNull (aSVRL);

      if (false)
        LOGGER.info (new SVRLMarshaller ().getAsString (aSVRL));

      final ICommonsList <AbstractSVRLMessage> aList = SVRLHelper.getAllFailedAssertionsAndSuccessfulReports (aSVRL);
      assertEquals ("Failures: " + new CommonsTreeSet <> (aList, AbstractSVRLMessage::getID), 0, aList.size ());
    }
  }

  @Nonnull
  private static ICommonsSet <String> _getAllFailedIDs (@Nonnull final String sFilename) throws Exception
  {
    final File f = new File ("src/test/resources/external/tsr/bad/" + sFilename);
    assertNotNull ("The file is not XSD compliant", new TransactionStatisticsReport101Marshaller ().read (f));

    final SchematronOutputType aSVRL = TransactionStatisticsReportValidator.getSchematronTSR_10 ()
                                                                           .applySchematronValidationToSVRL (new FileSystemResource (f));
    assertNotNull (aSVRL);

    if (false)
      LOGGER.info (new SVRLMarshaller ().getAsString (aSVRL));

    final ICommonsList <AbstractSVRLMessage> aList = SVRLHelper.getAllFailedAssertionsAndSuccessfulReports (aSVRL);
    assertFalse ("Found no errors in " + sFilename, aList.isEmpty ());

    final ICommonsSet <String> ret = new CommonsTreeSet <> (aList, AbstractSVRLMessage::getID);
    if (false)
      LOGGER.info ("Failures found: " + ret);
    return ret;
  }

  private static boolean _checkFailedID (@Nonnull final String sFilename, final String sExpected) throws Exception
  {
    final ICommonsSet <String> aFailed = _getAllFailedIDs (sFilename);
    final boolean bRet = aFailed.contains (sExpected);
    assertTrue ("Expected " + sExpected + " but got " + aFailed, bRet);
    return bRet;
  }

  @Test
  public void testTSRBadCases () throws Exception
  {
    assertTrue (_checkFailedID ("tsr-01-1.xml", "SCH-TSR-01"));
    assertTrue (_checkFailedID ("tsr-01-2.xml", "SCH-TSR-01"));

    assertTrue (_checkFailedID ("tsr-02-1.xml", "SCH-TSR-02"));
    assertTrue (_checkFailedID ("tsr-02-2.xml", "SCH-TSR-02"));

    // PerTP
    assertTrue (_checkFailedID ("tsr-03.xml", "SCH-TSR-03"));

    assertTrue (_checkFailedID ("tsr-04-1.xml", "SCH-TSR-04"));
    assertTrue (_checkFailedID ("tsr-04-2.xml", "SCH-TSR-04"));

    assertTrue (_checkFailedID ("tsr-05.xml", "SCH-TSR-05"));

    assertTrue (_checkFailedID ("tsr-06.xml", "SCH-TSR-06"));

    // PerSP-DT
    assertTrue (_checkFailedID ("tsr-07.xml", "SCH-TSR-07"));

    assertTrue (_checkFailedID ("tsr-08-1.xml", "SCH-TSR-08"));
    assertTrue (_checkFailedID ("tsr-08-2.xml", "SCH-TSR-08"));

    assertTrue (_checkFailedID ("tsr-09.xml", "SCH-TSR-09"));

    assertTrue (_checkFailedID ("tsr-10-1.xml", "SCH-TSR-10"));
    assertTrue (_checkFailedID ("tsr-10-2.xml", "SCH-TSR-10"));

    // PerSP-DT-CC
    assertTrue (_checkFailedID ("tsr-12-1.xml", "SCH-TSR-12"));
    assertTrue (_checkFailedID ("tsr-12-2.xml", "SCH-TSR-12"));

    assertTrue (_checkFailedID ("tsr-14-1.xml", "SCH-TSR-14"));
    assertTrue (_checkFailedID ("tsr-14-2.xml", "SCH-TSR-14"));

    // others
    assertTrue (_checkFailedID ("tsr-16-1.xml", "SCH-TSR-16"));
    assertTrue (_checkFailedID ("tsr-16-2.xml", "SCH-TSR-16"));

    assertTrue (_checkFailedID ("tsr-17-1.xml", "SCH-TSR-17"));
    assertTrue (_checkFailedID ("tsr-17-2.xml", "SCH-TSR-17"));

    assertTrue (_checkFailedID ("tsr-18-1.xml", "SCH-TSR-18"));
    assertTrue (_checkFailedID ("tsr-18-2.xml", "SCH-TSR-18"));

    assertTrue (_checkFailedID ("tsr-19-1.xml", "SCH-TSR-19"));
    assertTrue (_checkFailedID ("tsr-19-2.xml", "SCH-TSR-19"));

    assertTrue (_checkFailedID ("tsr-20-1.xml", "SCH-TSR-20"));
    assertTrue (_checkFailedID ("tsr-20-2.xml", "SCH-TSR-20"));

    assertTrue (_checkFailedID ("tsr-21.xml", "SCH-TSR-21"));

    assertTrue (_checkFailedID ("tsr-22-1.xml", "SCH-TSR-22"));
    assertTrue (_checkFailedID ("tsr-22-2.xml", "SCH-TSR-22"));

    assertTrue (_checkFailedID ("tsr-23-1.xml", "SCH-TSR-23"));
    assertTrue (_checkFailedID ("tsr-23-2.xml", "SCH-TSR-23"));

    assertTrue (_checkFailedID ("tsr-24-1.xml", "SCH-TSR-24"));
    assertTrue (_checkFailedID ("tsr-24-2.xml", "SCH-TSR-24"));

    assertTrue (_checkFailedID ("tsr-25-1.xml", "SCH-TSR-25"));
    assertTrue (_checkFailedID ("tsr-25-2.xml", "SCH-TSR-25"));

    assertTrue (_checkFailedID ("tsr-26-1.xml", "SCH-TSR-26"));
    assertTrue (_checkFailedID ("tsr-26-2.xml", "SCH-TSR-26"));

    assertTrue (_checkFailedID ("tsr-27-1.xml", "SCH-TSR-27"));
    assertTrue (_checkFailedID ("tsr-27-2.xml", "SCH-TSR-27"));

    assertTrue (_checkFailedID ("tsr-28-1.xml", "SCH-TSR-28"));
    assertTrue (_checkFailedID ("tsr-28-2.xml", "SCH-TSR-28"));

    assertTrue (_checkFailedID ("tsr-29-1.xml", "SCH-TSR-29"));
    assertTrue (_checkFailedID ("tsr-29-2.xml", "SCH-TSR-29"));

    assertTrue (_checkFailedID ("tsr-30-1.xml", "SCH-TSR-30"));
    assertTrue (_checkFailedID ("tsr-30-2.xml", "SCH-TSR-30"));

    assertTrue (_checkFailedID ("tsr-31-1.xml", "SCH-TSR-31"));
    assertTrue (_checkFailedID ("tsr-31-2.xml", "SCH-TSR-31"));

    assertTrue (_checkFailedID ("tsr-32-1.xml", "SCH-TSR-32"));
    assertTrue (_checkFailedID ("tsr-32-2.xml", "SCH-TSR-32"));

    assertTrue (_checkFailedID ("tsr-33-1.xml", "SCH-TSR-33"));
    assertTrue (_checkFailedID ("tsr-33-2.xml", "SCH-TSR-33"));

    assertTrue (_checkFailedID ("tsr-34-1.xml", "SCH-TSR-34"));
    assertTrue (_checkFailedID ("tsr-34-2.xml", "SCH-TSR-34"));

    assertTrue (_checkFailedID ("tsr-35-1.xml", "SCH-TSR-35"));
    assertTrue (_checkFailedID ("tsr-35-2.xml", "SCH-TSR-35"));

    assertTrue (_checkFailedID ("tsr-36-1.xml", "SCH-TSR-36"));
    assertTrue (_checkFailedID ("tsr-36-2.xml", "SCH-TSR-36"));

    assertTrue (_checkFailedID ("tsr-37-1.xml", "SCH-TSR-37"));
    assertTrue (_checkFailedID ("tsr-37-2.xml", "SCH-TSR-37"));

    assertTrue (_checkFailedID ("tsr-39-1.xml", "SCH-TSR-39"));

    assertTrue (_checkFailedID ("tsr-40-1.xml", "SCH-TSR-40"));

    assertTrue (_checkFailedID ("tsr-41-1.xml", "SCH-TSR-41"));

    assertTrue (_checkFailedID ("tsr-42-1.xml", "SCH-TSR-42"));

    assertTrue (_checkFailedID ("tsr-43-1.xml", "SCH-TSR-43"));

    assertTrue (_checkFailedID ("tsr-in-the-wild-1.xml", "SCH-TSR-11"));
  }
}
