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
package com.helger.peppol.reporting.api;

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
