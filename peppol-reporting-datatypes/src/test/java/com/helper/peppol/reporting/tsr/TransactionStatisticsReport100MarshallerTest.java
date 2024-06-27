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

import static org.junit.Assert.assertNotNull;

import java.io.File;

import org.junit.Test;

public final class TransactionStatisticsReport100MarshallerTest
{
  @Test
  public void testTSRGoodCases () throws Exception
  {
    final TransactionStatisticsReport101Marshaller m = new TransactionStatisticsReport101Marshaller ();
    for (final File f : TSRTestHelper.getAllGoodFiles ())
    {
      // Read and XSD validate
      assertNotNull ("The file " + f.getAbsolutePath () + " is not XSD compliant", m.read (f));
    }
  }
}
