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
package com.helger.peppol.reporting.api.backend;

import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.name.IHasDisplayName;
import com.helger.peppol.reporting.api.PeppolReportingItem;
import com.helger.peppol.reporting.api.PeppolReportingItemWithID;

/**
 * A ready to use Peppol Reporting backend, that stores reporting items. Instances are created via
 * {@link IPeppolReportingBackendProviderSPI#createBackend(com.helger.config.IConfig)} or
 * {@link PeppolReportingBackend#createBackend(com.helger.config.IConfig)} and must be closed once
 * they are no longer needed. Implementations must be thread-safe.<br>
 * Backends that can also read the stored reporting items additionally implement
 * {@link IPeppolReportingItemReader}.
 *
 * @author Philip Helger
 * @since 5.0.0
 */
public interface IPeppolReportingBackend extends IHasDisplayName, AutoCloseable
{
  /**
   * @return The display name of the backend. That is mainly for logging purposes. Neither
   *         <code>null</code> nor empty.
   */
  @NonNull
  @Nonempty
  String getDisplayName ();

  /**
   * Write a new {@link PeppolReportingItem} together with its unique item ID. The item ID is
   * assigned by the caller (e.g. the transaction ID) and must stay the same if the same item is
   * stored again, so that backends that support it can detect duplicates. Items with document types
   * that are not eligible for reporting are not stored.
   *
   * @param sItemID
   *        The unique ID of the reporting item. May neither be <code>null</code> nor empty and must
   *        not be longer than {@link PeppolReportingItemWithID#MAX_LEN_ITEM_ID} characters.
   * @param aReportingItem
   *        The reporting item to write. Must not be <code>null</code>.
   * @throws PeppolReportingBackendException
   *         In case of an unrecoverable error
   * @throws IllegalStateException
   *         If the backend is already closed
   */
  void storeReportingItem (@NonNull @Nonempty String sItemID,
                           @NonNull PeppolReportingItem aReportingItem) throws PeppolReportingBackendException;

  /**
   * Write a new {@link PeppolReportingItem}, using a new random UUID as the item ID. Because a new
   * item ID is created on every call, storing the same item again cannot be detected as a
   * duplicate.
   *
   * @param aReportingItem
   *        The reporting item to write. Must not be <code>null</code>.
   * @throws PeppolReportingBackendException
   *         In case of an unrecoverable error
   * @deprecated Use {@link #storeReportingItem(String, PeppolReportingItem)} with a stable item ID
   *             instead
   */
  @Deprecated (forRemoval = true, since = "5.0.0")
  default void storeReportingItem (@NonNull final PeppolReportingItem aReportingItem) throws PeppolReportingBackendException
  {
    storeReportingItem (UUID.randomUUID ().toString (), aReportingItem);
  }

  /**
   * Write multiple {@link PeppolReportingItem} objects together with their unique item IDs.
   * Backends may override this method to store all items in a single operation. The default
   * implementation calls {@link #storeReportingItem(String, PeppolReportingItem)} for each item. If
   * an exception is thrown, it is unspecified which of the items were stored, so callers must be
   * prepared to store the same items again.
   *
   * @param aReportingItems
   *        The reporting items with their IDs to write. Must not be <code>null</code> and must not
   *        contain <code>null</code> elements.
   * @throws PeppolReportingBackendException
   *         In case of an unrecoverable error
   * @throws IllegalStateException
   *         If the backend is already closed
   */
  default void storeReportingItems (@NonNull final List <PeppolReportingItemWithID> aReportingItems) throws PeppolReportingBackendException
  {
    ValueEnforcer.notNullNoNullValue (aReportingItems, "ReportingItems");
    for (final PeppolReportingItemWithID aReportingItem : aReportingItems)
      storeReportingItem (aReportingItem.getItemID (), aReportingItem.getReportingItem ());
  }

  /**
   * Close the backend and release all resources. Calling this method more than once has no effect.
   * After closing, all other methods throw an {@link IllegalStateException}.
   */
  void close ();
}
