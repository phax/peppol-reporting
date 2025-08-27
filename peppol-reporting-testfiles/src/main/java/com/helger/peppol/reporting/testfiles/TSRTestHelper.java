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
package com.helger.peppol.reporting.testfiles;

import com.helger.annotation.concurrent.Immutable;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.io.resource.ClassPathResource;

import jakarta.annotation.Nonnull;

@Immutable
public final class TSRTestHelper
{
  private TSRTestHelper ()
  {}

  @Nonnull
  @ReturnsMutableCopy
  public static ICommonsList <ClassPathResource> getAllGoodFiles ()
  {
    final ClassLoader aCL = CReportingTestFiles.getTestClassLoader ();
    final ICommonsList <ClassPathResource> ret = new CommonsArrayList <> ();
    for (final String s : new String [] { "transaction-statistics-2.xml",
                                          "transaction-statistics-3.xml",
                                          "transaction-statistics-4.xml",
                                          "transaction-statistics-minimal.xml" })
    {
      ret.add (new ClassPathResource ("external/tsr/good/" + s, aCL));
    }
    return ret;
  }
}
