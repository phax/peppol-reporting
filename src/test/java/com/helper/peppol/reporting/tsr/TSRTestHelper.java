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
    for (final String s : new String [] { "transaction-statistics-2.xml", "transaction-statistics-minimal.xml" })
    {
      final File f = new File ("src/test/resources/tsr/good/" + s);
      assertTrue (f.isFile ());
      ret.add (f);
    }
    return ret;
  }
}
