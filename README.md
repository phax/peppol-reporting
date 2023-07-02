# peppol-reporting

Peppol Reporting support library.
Peppol Reporting is the process of collecting, aggregating and transmitting Peppol Reports to OpenPeppol. 

This library supports the following reports:
* Peppol Transaction Statistics Report 1.0.1 (March 2023)
    * Supersedes 1.0.0 ([November 2022](https://openpeppol.atlassian.net/wiki/spaces/RR/pages/2967863297/End+user+statistics+reporting+BIS+22+November+2022))
* Peppol End User Statistics Report 1.0.0 (March 2023) 
    * Supersedes 1.0.0-RC2 ([November 2022](https://openpeppol.atlassian.net/wiki/spaces/RR/pages/2967863297/End+user+statistics+reporting+BIS+22+November+2022))

This library does not deal with the transmission of Reports.
That needs to be done with [phase4](https://github.com/phax/phase4) or another AS4 solution.

This library requires Java 11 and Maven to build.

# How to use it

This library offers a Java domain model for EUSR and TSR reports.
The classes reside in the packages `com.helger.peppol.reporting.jaxb.eusr.v100` and `com.helger.peppol.reporting.jaxb.tsr.v101`.
This domain model can be read from and written to XML documents via the marshaller classes `EndUserStatisticsReport100Marshaller` and `TransactionStatisticsReport101Marshaller`.
Additionally the Schematron compatibility can be verified using the classes `EndUserStatisticsReportValidator` and `TransactionStatisticsReportValidator`.

# Glossary

* EUSR - End User Statistics Report
* TSR - Transaction Statistics Report
* Report - Document containing OpenPeppol reporting information
* Reporting - The process of transmitting a **Report** to OpenPeppol
* Reporting Period - The period for which reporting data is to be collected and transmitted to OpenPeppol 

# Maven usage

Add the following to your pom.xml to use this artifact, replacing `x.y.z` with the real version:

```xml
<dependency>
  <groupId>com.helger</groupId>
  <artifactId>peppol-reporting</artifactId>
  <version>x.y.z</version>
</dependency>
```

# News and Noteworthy

* v1.1.0 - 2023-07-02
    * Updated to support EUSR 1.1.0
* v1.0.0 - 2023-04-26
    * Initial Version
    * Supports EUSR 1.0.0 and TSR 1.0.1 

---

My personal [Coding Styleguide](https://github.com/phax/meta/blob/master/CodingStyleguide.md) |
It is appreciated if you star the GitHub project if you like it.