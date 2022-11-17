package com.helper.peppol.reporting.tsr;

import javax.annotation.concurrent.NotThreadSafe;

import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.jaxb.GenericJAXBMarshaller;
import com.helger.peppol.reporting.jaxb.tsr.v100.ObjectFactory;
import com.helger.peppol.reporting.jaxb.tsr.v100.TransactionStatisticsReportType;

/**
 * XML marshaller for Peppol Transaction Statistics Reports, version 1.0.0 final
 * draft.
 *
 * @author Philip Helger
 */
@NotThreadSafe
public class TransactionStatisticsReportMarshaller extends GenericJAXBMarshaller <TransactionStatisticsReportType>
{
  public TransactionStatisticsReportMarshaller ()
  {
    super (TransactionStatisticsReportType.class,
           new CommonsArrayList <> (new ClassPathResource ("schemas/peppol-transaction-statistics-reporting-1.0.0.xsd",
                                                           TransactionStatisticsReportMarshaller.class.getClassLoader ())),
           new ObjectFactory ()::createTransactionStatisticsReport);
  }
}
