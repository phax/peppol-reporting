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
package com.helger.peppol.reporting.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.id.IHasID;
import com.helger.commons.lang.EnumHelper;

/**
 * This enum defines the overall reporting direction
 *
 * @author Philip Helger
 */
public enum EReportingDirection implements IHasID <String>
{
  /**
   * For an outgoing, outbound or sent message.
   */
  SENDING ("send"),
  /**
   * For an incoming, inbound or received message.
   */
  RECEIVING ("recv");

  private final String m_sID;

  EReportingDirection (@Nonnull @Nonempty final String sID)
  {
    m_sID = sID;
  }

  @Nonnull
  @Nonempty
  public String getID ()
  {
    return m_sID;
  }

  public boolean isSending ()
  {
    return this == SENDING;
  }

  public boolean isReceiving ()
  {
    return this == RECEIVING;
  }

  @Nullable
  public static EReportingDirection getFromIDOrNull (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrNull (EReportingDirection.class, sID);
  }

  @Nonnull
  public static EReportingDirection getFromIDOrThrow (@Nullable final String sID)
  {
    return EnumHelper.getFromIDOrThrow (EReportingDirection.class, sID);
  }
}
