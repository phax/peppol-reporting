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

import static org.junit.Assert.assertTrue;

import java.io.File;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;

@Immutable
public final class TSRTestHelper
{
  private TSRTestHelper ()
  {}

  @Nonnull
  @ReturnsMutableCopy
  public static ICommonsList <File> getAllGoodFiles ()
  {
    final ICommonsList <File> ret = new CommonsArrayList <> ();
    for (final String s : new String [] { "transaction-statistics-2.xml",
                                          "transaction-statistics-3.xml",
                                          "transaction-statistics-4.xml",
                                          "transaction-statistics-minimal.xml" })
    {
      final File f = new File ("src/test/resources/external/tsr/good/" + s);
      assertTrue (f.isFile ());
      ret.add (f);
    }
    return ret;
  }
}
