/*
 * Copyright (C) 2022-2023 Philip Helger
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
package com.helper.peppol.reporting.tsr.model;

import com.helger.commons.annotation.MustImplementEqualsAndHashcode;

/**
 * This is a marker interface for TSR Subtotal keys
 *
 * @author Philip Helger
 * @param <T>
 *        The implementation type
 * @since 1.2.0
 */
@MustImplementEqualsAndHashcode
public interface ITSRSubtotalKey <T extends ITSRSubtotalKey <T>> extends Comparable <T>
{
  /* empty */
}
