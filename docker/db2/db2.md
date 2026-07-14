# Local IBM DB2 for testing peppol-reporting

This describes how to start a local IBM DB2 instance to run the (experimental) DB2 unit
tests of the `peppol-reporting-backend-sql` submodule.

The values below match `peppol-reporting-backend-sql/src/test/resources/application-db2.properties`:
* Database name `phossrep` (DB2 database names are limited to 8 characters, so the literal
  name `peppol-reporting` cannot be used)
* Connect as the DB2 instance owner `db2inst1` (already has full privileges, so no additional
  user needs to be created)
* Password `peppol`
* Port `50000`

Create a persistent volume for the database:

```shell
docker volume create db2_peppol_reporting_data
```

* DB2 is memory hungry

```shell
docker run -itd --name db2-peppol-reporting --platform=linux/amd64 --privileged=true --memory=4g -p 50000:50000 -e LICENSE=accept -e DB2INST1_PASSWORD=peppol -e DBNAME=phossrep -v db2_peppol_reporting_data:/database icr.io/db2_community/db2
```

The first startup takes a few minutes because the database `phossrep` is created and activated.
Watch the log until it is ready:

```shell
docker logs -f db2-peppol-reporting
```

After some troubles, DB2 refused to start up - this helped:
```shell
docker exec -ti db2-peppol-reporting bash -c "chown root:db2iadm1 /database/config/db2inst1/sqllib/adm/fencedid"
```

Once DB2 is up, run the DB2 backend tests (Flyway creates the schema automatically):

```shell
mvn -pl peppol-reporting-backend-sql test -Dtest=PeppolReportingBackendDb2SPITest
```

The test suite is skipped automatically (via JUnit `Assume`) when the DB2 server is not reachable.
