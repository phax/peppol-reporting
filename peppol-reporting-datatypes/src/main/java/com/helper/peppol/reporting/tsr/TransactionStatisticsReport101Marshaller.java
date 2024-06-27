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

import javax.annotation.concurrent.NotThreadSafe;

import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.jaxb.GenericJAXBMarshaller;
import com.helger.peppol.reporting.jaxb.tsr.v101.ObjectFactory;
import com.helger.peppol.reporting.jaxb.tsr.v101.TransactionStatisticsReportType;

/**
 * XML marshaller for Peppol Transaction Statistics Reports, version 1.0.1 final
 * draft.
 *
 * @author Philip Helger
 */
@NotThreadSafe
public class TransactionStatisticsReport101Marshaller extends GenericJAXBMarshaller <TransactionStatisticsReportType>
{
  public static final ClassPathResource XSD_RES = new ClassPathResource ("external/schemas/peppol-transaction-statistics-reporting-1.0.1.xsd",
                                                                         TransactionStatisticsReport101Marshaller.class.getClassLoader ());

  public TransactionStatisticsReport101Marshaller ()
  {
    super (TransactionStatisticsReportType.class,
           new CommonsArrayList <> (XSD_RES),
           new ObjectFactory ()::createTransactionStatisticsReport);
  }
}
