package com.helper.peppol.reporting.eusr;

import javax.annotation.concurrent.NotThreadSafe;

import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.io.resource.ClassPathResource;
import com.helger.jaxb.GenericJAXBMarshaller;
import com.helger.peppol.reporting.jaxb.eusr.v100rc2.EndUserStatisticsReportType;
import com.helger.peppol.reporting.jaxb.eusr.v100rc2.ObjectFactory;

/**
 * XML marshaller for Peppol End User Statistics Reports, version 1.0.0-RC2.
 *
 * @author Philip Helger
 */
@NotThreadSafe
public class EndUserStatisticsReportMarshaller extends GenericJAXBMarshaller <EndUserStatisticsReportType>
{
  public EndUserStatisticsReportMarshaller ()
  {
    super (EndUserStatisticsReportType.class,
           new CommonsArrayList <> (new ClassPathResource ("schemas/peppol-end-user-statistics-reporting-1.0.0-RC2.xsd",
                                                           EndUserStatisticsReportMarshaller.class.getClassLoader ())),
           new ObjectFactory ()::createEndUserStatisticsReport);
  }
}
