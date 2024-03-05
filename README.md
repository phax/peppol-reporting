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
* OpenPeppol guideline for Reporting implementation: https://docs.peppol.eu/edelivery/changelog/2023-08/2023-08-20%20Peppol%20Reporting%20-%20SP%20Guideline%20v2.0.pdf 

This library does not deal with the transmission of Reports.
That needs to be done with [phase4](https://github.com/phax/phase4) or another AS4 solution.
See the [phase4 Wiki](https://github.com/phax/phase4/wiki/Profile-Peppol#special-handling-for-peppol-reporting) for detailed guidance on integration with this project. 

This library requires Java 11 and Maven to build.

# How to use it

This library offers a Java domain model for EUSR and TSR reports.

*Note*: phase4 v2.2.2 and onwards has support for this project.

## Overview images

![Data collection on sending](https://github.com/phax/peppol-reporting/blob/main/docs/collection-sending.png)

![Data collection on receiving](https://github.com/phax/peppol-reporting/blob/main/docs/collection-receiving.png)

![Report creation and transmission](https://github.com/phax/peppol-reporting/blob/main/docs/creation-and-transmission.png)

## Data collection

Data collection needs to happen into your Access Point instances.

The data for reporting needs to be collected in instances of class `PeppolReportingItem`.
For each sent or received Peppol transmission, such a `PeppolReportingItem` needs to be collected, and persisted.

Each `PeppolReportingItem` consists of the following elements:
* `OffsetDateTime m_aExchangeDTUTC` - timing of the exchange in UTC; for TSR and EUSR
* `EReportingDirection m_eDirection` - direction of the exchange; for TSR and EUSR
* `String m_sC2ID` - Peppol Seat ID of C2; for TSR only
* `String m_sC3ID` - Peppol Seat ID of C3; for TSR only
* `String m_sDocTypeIDScheme` and `String m_sDocTypeIDValue` - Document Type ID of the exchange; for TSR and EUSR 
* `String m_sProcessIDScheme` and `String m_sProcessIDValue` - Process ID of the exchange; for TSR and EUSR
* `String m_sTransportProtocol` - the transport protocol used; for TSR only
* `String m_sC1CountryCode` - the country code of C1; for TSR and EUSR
* `String m_sC4CountryCode` - the country code of C4 - only required for received messages; for TSR and EUSR
* `String m_sEndUserID` - the end user ID to aggregate on - this ID is **not** part of any report; for EUSR only

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
* **`peppol.reporting.mongodb.collection`** (since v2.2.1): the MongoDB collection name to use. Defaults to `reporting-items`.

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
* **`peppol.reporting.redis.user`** (since v2.2.3; optional): the username used to connect to the Redis server
* **`peppol.reporting.redis.password`** (since v2.2.3; optional): the password used to connect to the Redis server

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

* v2.2.3 - 2024-03-05
    * Added the possibility to provide username and password via configuration for the Redis backend
* v2.2.2 - 2024-01-29
    * Moved the method `PeppolReportingItem.isValidCountryCode(String)` to class `PeppolReportingHelper`
    * Added a constant `CPeppolReporting.REPLACEMENT_COUNTRY_CODE` for the `ZZ` code for invalid incoming country codes
    * Added a constant `CPeppolReporting.OPENPEPPOL_PARTICIPANT_ID` for the default receiver PID
* v2.2.1 - 2023-12-31
    * Made the collection name customizable in the MongoDB backend
    * Fixed an error in iterating in the "in-memory" backend when only entries from the last day of the period are present
* v2.2.0 - 2023-12-07
    * Modified classes `EUSRReportingItemList` and `TSRReportingItemList` so that the list is only iterated once and is based on `Iterable`. Backwards incompatible change.
    * Extended class `IPeppolReportingBackendSPI` with method `iterateReportingItems` to be able to lazily iterate over a data source. See [#2](https://github.com/phax/peppol-reporting/issues/2) - thx @iansmirlis
* v2.1.6 - 2023-11-10
    * Updated EUSR Schematron to v1.1.4
* v2.1.5 - 2023-11-02
    * Updated EUSR Schematron to v1.1.3 and TSR Schematron to v1.0.4
* v2.1.4 - 2023-10-12
    * Updated EUSR Schematron to v1.1.2 and TSR Schematron to v1.0.3
* v2.1.3 - 2023-09-21
    * Updated EUSR Schematron to v1.1.1 and TSR Schematron to v1.0.2
* v2.1.2 - 2023-09-12
    * Added class `PeppolReportingHelper` with some generic helper methods
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