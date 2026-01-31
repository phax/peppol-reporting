/*
 * Copyright (C) 2023-2026 Philip Helger
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
package com.helger.peppol.reporting.jaxb.eusr;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.helger.io.resource.ClassPathResource;
import com.helger.peppol.reporting.testfiles.EUSRTestHelper;

/**
 * Test class for class {@link EndUserStatisticsReport110Marshaller}.
 *
 * @author Philip Helger
 */
public final class EndUserStatisticsReport110MarshallerTest
{
  @Test
  public void testEUSRGoodCases () throws Exception
  {
    final EndUserStatisticsReport110Marshaller m = new EndUserStatisticsReport110Marshaller ();
    for (final ClassPathResource aRes : EUSRTestHelper.getAllGoodFiles ())
    {
      // Read and XSD validate
      assertNotNull ("The file " + aRes.getPath () + " is not XSD compliant", m.read (aRes));
    }
  }
}
