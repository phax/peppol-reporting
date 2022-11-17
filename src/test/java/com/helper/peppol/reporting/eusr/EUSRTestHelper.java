package com.helper.peppol.reporting.eusr;

import static org.junit.Assert.assertTrue;

import java.io.File;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;

@Immutable
public final class EUSRTestHelper
{
  private EUSRTestHelper ()
  {}

  @Nonnull
  @ReturnsMutableCopy
  public static ICommonsList <File> getAllGoodFiles ()
  {
    final ICommonsList <File> ret = new CommonsArrayList <> ();
    for (final String s : new String [] { "end-user-statistics-reporting-1.xml",
                                          "end-user-statistics-reporting-empty.xml",
                                          "end-user-statistics-reporting-minimal.xml" })
    {
      final File f = new File ("src/test/resources/eusr/good/" + s);
      assertTrue (f.isFile ());
      ret.add (f);
    }
    return ret;
  }
}
