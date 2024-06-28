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
package com.helger.peppol.reporting.backend.mongodb.config;

import javax.annotation.Nullable;

import com.helger.commons.annotation.IsSPIImplementation;
import com.helger.commons.thirdparty.ELicense;
import com.helger.commons.thirdparty.IThirdPartyModule;
import com.helger.commons.thirdparty.IThirdPartyModuleProviderSPI;
import com.helger.commons.thirdparty.ThirdPartyModule;
import com.helger.commons.version.Version;

/**
 * Implement this SPI interface if your JAR file contains external third party
 * modules.
 *
 * @author Philip Helger
 */
@IsSPIImplementation
public final class ThirdPartyModuleProvider_prb_mongodb implements IThirdPartyModuleProviderSPI
{
  private static final IThirdPartyModule MONGODB_JAVA_DRIVERS = new ThirdPartyModule ("MongoDB Java Drivers",
                                                                                      "MongoDB, Inc.",
                                                                                      ELicense.APACHE2,
                                                                                      new Version (5, 1, 0),
                                                                                      "https://www.mongodb.com/docs/drivers/java/sync/current/");

  @Nullable
  public IThirdPartyModule [] getAllThirdPartyModules ()
  {
    return new IThirdPartyModule [] { MONGODB_JAVA_DRIVERS };
  }
}
