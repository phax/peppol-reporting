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
package com.helger.peppol.reporting.testbackend;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.function.Consumer;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.collection.commons.CommonsHashSet;
import com.helger.collection.commons.ICommonsSet;
import com.helger.config.ConfigFactory;
import com.helger.config.IConfig;
import com.helger.peppol.reporting.api.PeppolReportingItem;
import com.helger.peppol.reporting.api.backend.IPeppolReportingBackendSPI;
import com.helger.peppol.reporting.api.backend.PeppolReportingBackendException;

/**
 * Abstract JUnit 4 base class containing the SPI contract tests every
 * {@link IPeppolReportingBackendSPI} implementation must satisfy. Concrete subclasses provide the
 * backend instance via {@link #createBackend()} and optionally override {@link #getConfig()} to
 * supply a backend-specific configuration.
 * <p>
 * The base class manages the backend lifecycle: a fresh backend is created before each test and
 * shut down afterwards. A cheap connectivity probe is issued in {@link #setUp()} so that DB-backed
 * tests skip cleanly (via {@link Assume}) when the underlying server is not reachable, instead of
 * failing the build.
 * <p>
 * The contract tests deliberately do <b>not</b> assert any ordering of items returned by
 * <code>iterateReportingItems</code> / <code>forEachReportingItem</code>: per the SPI Javadoc the
 * ordering is unspecified, so assertions use multiset (set + count) equality.
 *
 * @author Philip Helger
 */
public abstract class AbstractPeppolReportingBackendSPITest
{
  private static final Logger LOGGER = LoggerFactory.getLogger (AbstractPeppolReportingBackendSPITest.class);

  protected IPeppolReportingBackendSPI m_aBackend;

  /**
   * Stable per-test "base date" used by all queries within a single test method. Computed once in
   * {@link #setUp()} and never mutated, so multiple calls to {@link #testRunBaseDate()} within one
   * test see the same value.
   */
  private LocalDate m_aTestRunBaseDate;

  /**
   * @return A fresh, not-yet-initialized backend instance to test. Called once per test method.
   */
  @NonNull
  protected abstract IPeppolReportingBackendSPI createBackend ();

  /**
   * @return The configuration to pass to <code>initBackend</code>. Default is
   *         {@link ConfigFactory#getDefaultConfig()} which loads
   *         <code>application.properties</code> from the classpath.
   */
  @NonNull
  protected IConfig getConfig ()
  {
    return ConfigFactory.getDefaultConfig ();
  }

  @Before
  public void setUp ()
  {
    // Stable per-test base date. 1900-01-01 is well before any real Peppol
    // data and the random offset reduces collisions across separate test
    // runs that share a backend (e.g. a real database).
    final long nOffset = (System.nanoTime () & 0x7fffffffL) % 1000L;
    m_aTestRunBaseDate = LocalDate.of (1900, 1, 1).plusDays (nOffset);

    m_aBackend = createBackend ();
    assertNotNull ("createBackend() returned null", m_aBackend);
    assertFalse ("Fresh backend must report isInitialized() == false before initBackend() is called",
                 m_aBackend.isInitialized ());

    // Init may either return ESuccess.FAILURE (e.g. CSV with bad config) or
    // throw an exception from inside the backend's wiring (e.g. SQL/Flyway
    // when the database server is not reachable). Both cases should skip the
    // suite cleanly via Assume so the build doesn't fail when an optional
    // dependency isn't running.
    boolean bInitOk;
    try
    {
      bInitOk = m_aBackend.initBackend (getConfig ()).isSuccess ();
    }
    catch (final RuntimeException ex)
    {
      LOGGER.info ("Backend " +
                   m_aBackend.getDisplayName () +
                   " could not be initialized, skipping: " +
                   ex.getMessage ());
      Assume.assumeNoException ("Backend init threw, treating as not available", ex);
      return; // unreachable, Assume aborts
    }
    Assume.assumeTrue ("Backend " + m_aBackend.getDisplayName () + " could not be initialized — skipping tests",
                       bInitOk);

    // Cheap connectivity probe: issue a single-day query for today. For
    // DB-backed backends whose initBackend() does not eagerly connect (Mongo,
    // Redis), this surfaces "DB not running" so the suite is skipped cleanly
    // via Assume rather than reported as a failure.
    try
    {
      final LocalDate aToday = LocalDate.now ();
      m_aBackend.iterateReportingItems (aToday, aToday).forEach (x -> {});
    }
    catch (final RuntimeException | PeppolReportingBackendException ex)
    {
      LOGGER.info ("Backend " +
                   m_aBackend.getDisplayName () +
                   " not available for testing, skipping: " +
                   ex.getMessage ());
      Assume.assumeNoException ("Backend not reachable", ex);
    }
  }

  @After
  public void tearDown ()
  {
    if (m_aBackend != null && m_aBackend.isInitialized ())
      m_aBackend.shutdownBackend ();
    m_aBackend = null;
  }

  // ---------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------

  /**
   * @return The stable base date for the current test method. Always returns the same value within
   *         one test, so it is safe to call multiple times when constructing items and queries.
   */
  @NonNull
  protected final LocalDate testRunBaseDate ()
  {
    return m_aTestRunBaseDate;
  }

  /**
   * Drain an {@link Iterable} into a multiset-friendly collection (a Set of items, since
   * {@link PeppolReportingItem} implements equals/hashCode and the test items are all distinct by
   * end user ID + timestamp).
   */
  @NonNull
  private static ICommonsSet <PeppolReportingItem> _drainToSet (@Nullable final Iterable <PeppolReportingItem> aIterable)
  {
    final ICommonsSet <PeppolReportingItem> ret = new CommonsHashSet <> ();
    if (aIterable != null)
      for (final PeppolReportingItem aItem : aIterable)
        ret.add (aItem);
    return ret;
  }

  // ---------------------------------------------------------------------
  // Lifecycle / state machine
  // ---------------------------------------------------------------------

  @Test
  public void testIsInitializedTrueAfterInit ()
  {
    assertTrue ("After @Before setUp the backend must report isInitialized() == true", m_aBackend.isInitialized ());
  }

  @Test
  public void testShutdownReturnsToUninitialized ()
  {
    m_aBackend.shutdownBackend ();
    assertFalse ("After shutdownBackend() the backend must report isInitialized() == false",
                 m_aBackend.isInitialized ());
  }

  @Test
  public void testReinitAfterShutdown () throws PeppolReportingBackendException
  {
    m_aBackend.shutdownBackend ();
    assertFalse (m_aBackend.isInitialized ());

    assertTrue ("Re-initialization after shutdown must succeed", m_aBackend.initBackend (getConfig ()).isSuccess ());
    assertTrue (m_aBackend.isInitialized ());

    // And basic ops still work
    final LocalDate aDate = testRunBaseDate ();
    m_aBackend.storeReportingItem (PeppolReportingTestItems.sendingItem (aDate, 0));
  }

  @Test
  public void testStoreBeforeInitRejected ()
  {
    // Use a brand-new backend instance that has never been initialized.
    final IPeppolReportingBackendSPI aFresh = createBackend ();
    assertFalse (aFresh.isInitialized ());
    try
    {
      aFresh.storeReportingItem (PeppolReportingTestItems.sendingItem (testRunBaseDate (), 0));
      fail ("storeReportingItem on an uninitialized backend must throw IllegalStateException");
    }
    catch (final IllegalStateException ex)
    {
      // expected
    }
    catch (final PeppolReportingBackendException ex)
    {
      fail ("Expected IllegalStateException but got PeppolReportingBackendException: " + ex.getMessage ());
    }
  }

  @Test
  public void testQueryBeforeInitRejected ()
  {
    final IPeppolReportingBackendSPI aFresh = createBackend ();
    assertFalse (aFresh.isInitialized ());
    final LocalDate aDate = testRunBaseDate ();
    try
    {
      aFresh.iterateReportingItems (aDate, aDate).forEach (x -> {});
      fail ("iterateReportingItems on an uninitialized backend must throw IllegalStateException");
    }
    catch (final IllegalStateException ex)
    {
      // expected
    }
    catch (final PeppolReportingBackendException ex)
    {
      fail ("Expected IllegalStateException but got PeppolReportingBackendException: " + ex.getMessage ());
    }
  }

  // ---------------------------------------------------------------------
  // Argument validation
  // ---------------------------------------------------------------------

  @Test
  public void testNullArgumentsRejected () throws PeppolReportingBackendException
  {
    final LocalDate aDate = testRunBaseDate ();

    try
    {
      m_aBackend.storeReportingItem (null);
      fail ("storeReportingItem(null) must throw");
    }
    catch (final NullPointerException ex)
    {
      // expected (ValueEnforcer.notNull throws NullPointerException)
    }

    try
    {
      m_aBackend.iterateReportingItems (null, aDate);
      fail ("iterateReportingItems(null, end) must throw");
    }
    catch (final NullPointerException ex)
    {
      // expected
    }

    try
    {
      m_aBackend.iterateReportingItems (aDate, null);
      fail ("iterateReportingItems(start, null) must throw");
    }
    catch (final NullPointerException ex)
    {
      // expected
    }

    try
    {
      m_aBackend.forEachReportingItem (aDate, aDate, (Consumer <? super PeppolReportingItem>) null);
      fail ("forEachReportingItem(_, _, null) must throw");
    }
    catch (final NullPointerException ex)
    {
      // expected
    }
  }

  @Test
  public void testReversedRangeRejected () throws PeppolReportingBackendException
  {
    final LocalDate aStart = testRunBaseDate ().plusDays (5);
    final LocalDate aEnd = aStart.minusDays (1);
    try
    {
      m_aBackend.iterateReportingItems (aStart, aEnd);
      fail ("iterateReportingItems with end < start must throw");
    }
    catch (final IllegalArgumentException ex)
    {
      // expected
    }
  }

  // ---------------------------------------------------------------------
  // Store / retrieve contract
  // ---------------------------------------------------------------------

  @Test
  public void testStoreAndRetrieveSingleItem () throws PeppolReportingBackendException
  {
    final LocalDate aDate = testRunBaseDate ();
    final PeppolReportingItem aItem = PeppolReportingTestItems.sendingItem (aDate, 1);

    m_aBackend.storeReportingItem (aItem);

    final ICommonsSet <PeppolReportingItem> aFound = _drainToSet (m_aBackend.iterateReportingItems (aDate, aDate));
    assertTrue ("Stored item must be returned by a single-day query: " + aFound, aFound.contains (aItem));
  }

  @Test
  public void testStoreMultipleAcrossDays () throws PeppolReportingBackendException
  {
    final LocalDate aBase = testRunBaseDate ();
    final PeppolReportingItem aDay0a = PeppolReportingTestItems.sendingItem (aBase, 100);
    final PeppolReportingItem aDay0b = PeppolReportingTestItems.receivingItem (aBase, 101);
    final PeppolReportingItem aDay1 = PeppolReportingTestItems.sendingItem (aBase.plusDays (1), 102);
    final PeppolReportingItem aDay2 = PeppolReportingTestItems.receivingItem (aBase.plusDays (2), 103);

    m_aBackend.storeReportingItem (aDay0a);
    m_aBackend.storeReportingItem (aDay0b);
    m_aBackend.storeReportingItem (aDay1);
    m_aBackend.storeReportingItem (aDay2);

    // Full range
    final ICommonsSet <PeppolReportingItem> aAll = _drainToSet (m_aBackend.iterateReportingItems (aBase,
                                                                                                  aBase.plusDays (2)));
    assertTrue (aAll.contains (aDay0a));
    assertTrue (aAll.contains (aDay0b));
    assertTrue (aAll.contains (aDay1));
    assertTrue (aAll.contains (aDay2));

    // Day 0 only
    final ICommonsSet <PeppolReportingItem> aDay0 = _drainToSet (m_aBackend.iterateReportingItems (aBase, aBase));
    assertTrue (aDay0.contains (aDay0a));
    assertTrue (aDay0.contains (aDay0b));
    assertFalse (aDay0.contains (aDay1));
    assertFalse (aDay0.contains (aDay2));

    // Days 1..2
    final ICommonsSet <PeppolReportingItem> aDay12 = _drainToSet (m_aBackend.iterateReportingItems (aBase.plusDays (1),
                                                                                                    aBase.plusDays (2)));
    assertFalse (aDay12.contains (aDay0a));
    assertFalse (aDay12.contains (aDay0b));
    assertTrue (aDay12.contains (aDay1));
    assertTrue (aDay12.contains (aDay2));
  }

  @Test
  public void testInclusiveStartAndEndDate () throws PeppolReportingBackendException
  {
    final LocalDate aBase = testRunBaseDate ();
    final PeppolReportingItem aOnStart = PeppolReportingTestItems.sendingItem (aBase, 200);
    final PeppolReportingItem aOnEnd = PeppolReportingTestItems.receivingItem (aBase.plusDays (3), 201);

    m_aBackend.storeReportingItem (aOnStart);
    m_aBackend.storeReportingItem (aOnEnd);

    final ICommonsSet <PeppolReportingItem> aFound = _drainToSet (m_aBackend.iterateReportingItems (aBase,
                                                                                                    aBase.plusDays (3)));
    assertTrue ("Item exactly on start date must be inclusive: " + aFound, aFound.contains (aOnStart));
    assertTrue ("Item exactly on end date must be inclusive: " + aFound, aFound.contains (aOnEnd));
  }

  @Test
  public void testSingleDayRange () throws PeppolReportingBackendException
  {
    final LocalDate aDate = testRunBaseDate ();
    final PeppolReportingItem aItem = PeppolReportingTestItems.sendingItem (aDate, 300);
    m_aBackend.storeReportingItem (aItem);

    // start == end is legal
    final ICommonsSet <PeppolReportingItem> aFound = _drainToSet (m_aBackend.iterateReportingItems (aDate, aDate));
    assertTrue (aFound.contains (aItem));
  }

  @Test
  public void testEmptyResultRange () throws PeppolReportingBackendException
  {
    // Pick a far-future date that we never store anything on. The iterable
    // must be non-null and produce no items.
    final LocalDate aFuture = LocalDate.of (2999, 1, 1);
    final Iterable <PeppolReportingItem> aIt = m_aBackend.iterateReportingItems (aFuture, aFuture.plusDays (10));
    assertNotNull ("iterateReportingItems must never return null", aIt);
    int nCount = 0;
    for (final PeppolReportingItem aItem : aIt)
    {
      // Should not iterate anything for an empty range, but tolerate items
      // that other tests on the same shared backend might have stored.
      if (!aItem.getExchangeDTUTC ().toLocalDate ().isBefore (aFuture) &&
          !aItem.getExchangeDTUTC ().toLocalDate ().isAfter (aFuture.plusDays (10)))
        nCount++;
    }
    assertEquals ("Future-only range must produce zero items", 0, nCount);
  }

  @Test
  public void testQueryYearMonthOverloadMatchesDateRange () throws PeppolReportingBackendException
  {
    final YearMonth aYM = YearMonth.of (1900, 1);
    // Items in this YearMonth — using deterministic dates that won't collide
    // with testRunBaseDate's offset window cleanly, but YearMonth = 1900-01
    // covers all of testRunBaseDate's possible days.
    final PeppolReportingItem a1 = PeppolReportingTestItems.sendingItem (aYM.atDay (1), 400);
    final PeppolReportingItem a2 = PeppolReportingTestItems.receivingItem (aYM.atEndOfMonth (), 401);

    m_aBackend.storeReportingItem (a1);
    m_aBackend.storeReportingItem (a2);

    final ICommonsSet <PeppolReportingItem> aViaYM = _drainToSet (m_aBackend.iterateReportingItems (aYM));
    final ICommonsSet <PeppolReportingItem> aViaRange = _drainToSet (m_aBackend.iterateReportingItems (aYM.atDay (1),
                                                                                                       aYM.atEndOfMonth ()));
    assertEquals ("YearMonth overload must return the same items as the equivalent date range", aViaRange, aViaYM);
    assertTrue (aViaYM.contains (a1));
    assertTrue (aViaYM.contains (a2));
  }

  @Test
  public void testForEachMatchesIterate () throws PeppolReportingBackendException
  {
    final LocalDate aBase = testRunBaseDate ();
    m_aBackend.storeReportingItem (PeppolReportingTestItems.sendingItem (aBase, 500));
    m_aBackend.storeReportingItem (PeppolReportingTestItems.receivingItem (aBase, 501));
    m_aBackend.storeReportingItem (PeppolReportingTestItems.sendingItem (aBase.plusDays (1), 502));

    final ICommonsSet <PeppolReportingItem> aViaIterate = _drainToSet (m_aBackend.iterateReportingItems (aBase,
                                                                                                         aBase.plusDays (1)));

    final ICommonsSet <PeppolReportingItem> aViaForEach = new CommonsHashSet <> ();
    m_aBackend.forEachReportingItem (aBase, aBase.plusDays (1), aViaForEach::add);

    assertEquals ("forEachReportingItem must return the same multiset as iterateReportingItems",
                  aViaIterate,
                  aViaForEach);
  }

  @Test
  public void testNonEligibleDocTypeFiltered () throws PeppolReportingBackendException
  {
    final LocalDate aDate = testRunBaseDate ();
    final PeppolReportingItem aDropped = PeppolReportingTestItems.nonEligibleItem (aDate);

    // Must not throw — non-eligible items are silently filtered out.
    m_aBackend.storeReportingItem (aDropped);

    final ICommonsSet <PeppolReportingItem> aFound = _drainToSet (m_aBackend.iterateReportingItems (aDate, aDate));
    assertFalse ("Non-eligible doctype items must be silently filtered and not stored: " + aFound,
                 aFound.contains (aDropped));
  }

  @NonNull
  private static ICommonsSet <PeppolReportingItem> _missing (@NonNull final ICommonsSet <PeppolReportingItem> aExpected,
                                                             @NonNull final ICommonsSet <PeppolReportingItem> aActual)
  {
    final ICommonsSet <PeppolReportingItem> ret = new CommonsHashSet <> (aExpected);
    ret.removeAll (aActual);
    return ret;
  }

  @Test
  public void testStoreManyAndRetrieveAllRegardlessOfOrder () throws PeppolReportingBackendException
  {
    final LocalDate aBase = testRunBaseDate ();
    final ICommonsSet <PeppolReportingItem> aStored = new CommonsHashSet <> ();
    for (int i = 0; i < 50; i++)
    {
      final PeppolReportingItem aItem = (i % 2 == 0) ? PeppolReportingTestItems.sendingItem (aBase, 600 + i)
                                                     : PeppolReportingTestItems.receivingItem (aBase.plusDays (1),
                                                                                               600 + i);
      m_aBackend.storeReportingItem (aItem);
      aStored.add (aItem);
    }

    final ICommonsSet <PeppolReportingItem> aRetrieved = _drainToSet (m_aBackend.iterateReportingItems (aBase,
                                                                                                        aBase.plusDays (1)));
    // Subset check (other test runs in a shared backend may have left noise).
    assertTrue ("All stored items must be retrievable, regardless of ordering. Missing: " +
                _missing (aStored, aRetrieved),
                aRetrieved.containsAll (aStored));
  }
}
