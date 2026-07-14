# Local Oracle for testing peppol-reporting

This describes how to start a local Oracle instance to run the Oracle unit tests of the
`peppol-reporting-backend-sql` submodule.

The values below match `peppol-reporting-backend-sql/src/test/resources/application-oracle.properties`:
* Pluggable database (service name) `ORCLPDB1`
* Application user / schema `peppol` (Oracle folds this to `PEPPOL`)
* Password `peppol`
* Port `1521`

## Download and run

To test Oracle via Docker you need an Oracle account and the image from the Oracle Container
Registry: https://container-registry.oracle.com/

Run it like this, with a persistent volume:
```shell
docker run -d --name orcl19c -p 1521:1521 -p 5500:5500 -e ORACLE_PWD=password -v OracleDBData:/opt/oracle/oradata container-registry.oracle.com/database/enterprise:19.19.0.0
```

The first startup takes a while. Watch the log until the database is open:
```shell
docker logs -f orcl19c
```

## Init DB

Create the application user in the pluggable database. Connect as SYSDBA, e.g. with:
`sqlplus sys/password@localhost:1521/ORCLPDB1 as sysdba`

```sql
-- Unquoted names are upper-cased anyway
CREATE USER PEPPOL IDENTIFIED BY peppol;

ALTER SESSION SET CURRENT_SCHEMA = PEPPOL;

GRANT CREATE SESSION TO PEPPOL;
GRANT CREATE TABLE TO PEPPOL;
GRANT CREATE SEQUENCE TO PEPPOL;
GRANT CREATE TRIGGER TO PEPPOL;
GRANT CREATE VIEW TO PEPPOL;
GRANT CREATE PROCEDURE TO PEPPOL;
GRANT CREATE TYPE TO PEPPOL;
GRANT CREATE MATERIALIZED VIEW TO PEPPOL;

-- Optional: Unlimited quota on default tablespace (common for dev)
GRANT UNLIMITED TABLESPACE TO PEPPOL;
```

Once Oracle is up and the user exists, run the Oracle backend tests (Flyway creates the schema
automatically):

```shell
mvn -pl peppol-reporting-backend-sql test -Dtest=PeppolReportingBackendOracleSPITest
```

The test suite is skipped automatically (via JUnit `Assume`) when the Oracle server is not reachable.
