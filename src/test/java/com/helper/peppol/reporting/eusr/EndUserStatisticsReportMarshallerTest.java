/*
 * Copyright (C) 2022-2023 Philip Helger
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
