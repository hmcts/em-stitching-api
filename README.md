# EVIDENCE MANAGEMENT Stitching App

[![Build Status](https://travis-ci.org/hmcts/rpa-em-stitching-api.svg?branch=master)](https://travis-ci.org/hmcts/rpa-em-stitching-api)
[![codecov](https://codecov.io/gh/hmcts/rpa-em-stitching-api/branch/master/graph/badge.svg)](https://codecov.io/gh/hmcts/rpa-em-stitching-api)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

Stitching API is a backend service to merge Word/PDF documents.

# Setup.

```
#Cloning repo and running though docker
git clone https://github.com/hmcts/rpa-em-stitching-api.git
cd rpa-em-stitching-api/
az login
az acr login --name hmctspublic
docker-compose -f docker-compose-dependencies-simulator.yml pull
docker-compose -f docker-compose-dependencies-simulator.yml up

wait for 2-3 minutes till all the dependencies in the docker are up and running.
./gradlew clean
./gradlew build

# Set up DB (name, password and db are called emstitch)
gradle migratePostgresDatabase
DOCMOSIS_ACCESS_KEY=<DOCMOSIS_ACCESS_KEY_VALUE> gradle bootRun

To set up IDAM data run: `./idam-client-setup.sh`. 
To check the data you can log into IDAM-web-admin `http://localhost:8082` with:
Username `idamOwner@hmcts.net`
Password `Ref0rmIsFun`

```
Note that your VPN needs to be on when running functional tests.

# Document Tasks

This API makes use of [Spring Batch](https://spring.io/projects/spring-batch) scheduled tasks in order to process stitching jobs asynchronously. The `versioned_document_task` table acts as a queue for the processing nodes.

## Distributed locks

In order to run in a highly available environment with redundant servers the task runner implements a distributed lock using [shedlock](https://github.com/lukas-krecan/ShedLock) to ensure tasks are only executed on a single server.

## Task versioning

In order to support zero-downtime deployments document tasks are versioned. This enables two versions of the API (the old version and the new version) to run side-by-side and share the same database, while ensuring that any document tasks created by the new version are processed by the new version.

Tasks created by the old version of the code may be processed by the new version, when this occurs the version number of the task will be updated when it is saved to the database.

As the version of the code may be ahead of the version of the document task, any non-nullable columns added to the schema must have a default value.

The document task version number is derived from the build number inside the `build-info.properties` file. In a development environment this will default to version 1. 
# Swagger UI
To view our REST API go to {HOST}:{PORT}/swagger-ui.html
> http://localhost:8080/swagger-ui.html

## API Endpoints
A list of our endpoints can be found here
> https://hmcts.github.io/reform-api-docs/swagger.html?url=https://hmcts.github.io/reform-api-docs/specs/rpa-em-stitching-api.json

### Running contract or pact tests:

You can run contract or pact tests as follows:

```
./gradlew clean
```

```
./gradlew contract
```

You can then publish your pact tests locally by first running the pact docker-compose:

```
docker-compose -f docker-pactbroker-compose.yml up
```

and then using it to publish your tests:

```
./gradlew pactPublish
```

# Tech
It uses:

* Java8
* Spring boot
* Spring batch
* Junit, Mockito and SpringBootTest and Powermockito
* Gradle
* [lombok project](https://projectlombok.org/) - Lombok project
* Postgres
* Liquibase

## License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details