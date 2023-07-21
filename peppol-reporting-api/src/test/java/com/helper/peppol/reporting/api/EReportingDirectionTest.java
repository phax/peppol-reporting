package com.helper.peppol.reporting.api;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Test class for class {@link EReportingDirection}.
 *
 * @author Philip Helger
 */
public class EReportingDirectionTest
{
  @Test
  public void testBasic ()
  {
    assertTrue (EReportingDirection.SENDING.isSending ());
    assertFalse (EReportingDirection.SENDING.isReceiving ());

    assertFalse (EReportingDirection.RECEIVING.isSending ());
    assertTrue (EReportingDirection.RECEIVING.isReceiving ());
  }
}
