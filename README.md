# peppol-reporting

[![javadoc](https://javadoc.io/badge2/com.helger.peppol/peppol-reporting-api/javadoc.svg)](https://javadoc.io/doc/com.helger.peppol/peppol-reporting-api)
[![Maven Central](https://img.shields.io/maven-central/v/com.helger.peppol/peppol-reporting.svg)](http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.helger.peppol%22%20AND%20a%3A%22peppol-reporting%22)

Peppol Reporting support library.
Peppol Reporting is the process of collecting, aggregating and transmitting Peppol Reports to OpenPeppol. 

This library supports the following reports:
* Peppol Transaction Statistics Report 1.0.1 (March 2023)
    * Specification link: https://docs.peppol.eu/edelivery/specs/reporting/tsr/
    * Supersedes 1.0.0 ([November 2022](https://openpeppol.atlassian.net/wiki/spaces/RR/pages/2967863297/End+user+statistics+reporting+BIS+22+November+2022))
* Peppol End User Statistics Report 1.1.0 (June 2023)
    * Specification link: https://docs.peppol.eu/edelivery/specs/reporting/eusr/
    * Supersedes 1.0.0 and 1.0.0-RC2 ([November 2022](https://openpeppol.atlassian.net/wiki/spaces/RR/pages/2967863297/End+user+statistics+reporting+BIS+22+November+2022))

This library does not deal with the transmission of Reports.
That needs to be done with [phase4](https://github.com/phax/phase4) or another AS4 solution.

This library requires Java 11 and Maven to build.

# How to use it

This library offers a Java domain model for EUSR and TSR reports.

*Note*: phase4 v2.2.2 and onwards has support for this project. 

## Data collection

Data collection needs to happen into your Access Point instances.

The data for reporting needs to be collected in instances of class `PeppolReportingItem`.
For each sent or received Peppol transmission, such a `PeppolReportingItem` needs to be collected, and persisted.

To facilitate this collection, the submodule `peppol-reporting-api` exists.

## Data storage

The created reporting item must be stored somewhere, to be able to retrieve them later.

This project comes with different backends for storing `PeppolReportingItem` objects, each in a separate submodule.
Each submodule is described below.

To choose a submodule, it needs to be added as a Maven dependency. The main logic is loaded via SPI.
Please make sure to only use **one** submodule at a time - storing to multiple backends is currently not supported out of the box.

Alternatively you can implement your own Reporting backend implementation, by implementing the  SPI interface
`com.helper.peppol.reporting.api.backend.IPeppolReportingBackendSPI` defined in the `peppol-reporting-api` submodule. 

### Storage in MongoDB

Submodule `peppol-reporting-backend-mongodb` stores data in a MongoDB.
This submodule was introduced in version 2.1.0.

It creates one collection called: `reporting-items`

It supports the following configuration properties:
* **`peppol.reporting.mongodb.connectionstring`**: the connection string to use to connect to MongoDB  
* **`peppol.reporting.mongodb.dbname`**: the MongoDB database name to use  

### Storage in Redis

Submodule `peppol-reporting-backend-redis` stores data in Redis. Make sure you use persistent storage for this one.
This submodule was introduced in version 2.1.0.

The used Redis keys are:
* `peppol:reporting:itemidx` - counter for unique IDs
* `peppol:reporting:item:*` - represents a single reporting item hash map
* `peppol:reporting:*` - contains a list of reporting item keys of a single day

It supports the following configuration properties:
* **`peppol.reporting.redis.host`**: the Redis host to connect to  
* **`peppol.reporting.redis.port`**: the Redis port to connect to  

### Storage in memory

Submodule `peppol-reporting-backend-inmemory` stores data in memory only and is **not persistent**.
This submodule was introduced in version 2.1.1.

This module is mainly meant for testing purposes.

## Data aggregation

To aggregate data for a single Reporting Period, all the matching `PeppolReportingItem` objects need to be collected first.
All the matching items need to be fed into the respective report builder.

Via the builder `EndUserStatisticsReport.builder ()`, the report of type `EndUserStatisticsReportType` can be created.

Via the builder `TransactionStatisticsReport.builder ()`, the report of type `TransactionStatisticsReportType` can be created.

## Report XML Serialization

The JAXB generated domain model classes reside in the packages `com.helger.peppol.reporting.jaxb.eusr.v110` and `com.helger.peppol.reporting.jaxb.tsr.v101`.
This domain model can be read from and written to XML documents via the marshaller classes `EndUserStatisticsReport110Marshaller` and `TransactionStatisticsReport101Marshaller`.

## Report Validation

Additionally, the Schematron compatibility can be verified using the classes `EndUserStatisticsReportValidator` and `TransactionStatisticsReportValidator`.
All checks are performed against the default Schematrons provided by OpenPeppol.

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
  <groupId>com.helger.peppol</groupId>
  <artifactId>peppol-reporting</artifactId>
  <version>x.y.z</version>
</dependency>
```

Usage as Maven BOM:

```xml
<dependency>
  <groupId>com.helger.peppol</groupId>
  <artifactId>peppol-reporting-parent-pom</artifactId>
  <version>x.y.z</version>
  <type>pom</type>
  <scope>import</scope>
</dependency>
```

Note: all v1.x releases used the group ID `com.helger` only.

# News and Noteworthy

* v2.1.1 - 2023-09-10
    * Added new submodule `peppol-reporting-backend-inmemory` that uses memory persistence as the backend to store reporting items
    * Added third party module descriptors
    * Fixed the date time offset when storing to MongoDB
* v2.1.0 - 2023-09-10
    * Added new API package `com.helper.peppol.reporting.api.backend` to define a generic backend API
    * Added new submodule `peppol-reporting-backend-mongodb` that uses MongoDB as the backend to store reporting items
    * Added new submodule `peppol-reporting-backend-redis` that uses Redis as the backend to store reporting items
* v2.0.0 - 2023-07-21
    * Changed the Maven Group ID to be `com.helger.peppol` instead of `com.helger`
    * Introduced the new submodule `peppol-reporting-api`
    * Changed some of the package names introduced in v1.2.0 to reflect the submodule name
    * Using Maven Bundle plugin to create OSGI bundles
* v1.2.0 - 2023-07-20
    * Added data models to easily build End User Statistics Reports v1.1.0 in code
    * Added data models to easily build Transaction Statistics Reports v1.0.1 in code
* v1.1.0 - 2023-07-02
    * Updated to support EUSR 1.1.0
* v1.0.0 - 2023-04-26
    * Initial Version
    * Supports EUSR 1.0.0 and TSR 1.0.1 

---

My personal [Coding Styleguide](https://github.com/phax/meta/blob/master/CodingStyleguide.md) |
It is appreciated if you star the GitHub project if you like it.