# EVIDENCE MANAGEMENT Stitching App

[![Build Status](https://travis-ci.org/hmcts/rpa-em-stitching-api.svg?branch=master)](https://travis-ci.org/hmcts/rpa-em-stitching-api)
[![codecov](https://codecov.io/gh/hmcts/rpa-em-stitching-api/branch/master/graph/badge.svg)](https://codecov.io/gh/hmcts/rpa-em-stitching-api)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

Stitching API is a backend service to merge Word/PDF documents.


# Setup.

#### To clone repo and prepare to pull containers:
```
git clone https://github.com/hmcts/em-stitching-api.git
cd em-stitching-api/
```

#### Clean and build the application:
```
./gradlew clean
./gradlew build
```

#### To run the application:

VPN connection is required

```
az login
./gradlew bootWithCCD
```

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
To view our REST API go to http://{HOST}/swagger-ui/index.html
On local machine with server up and running, link to swagger is as below
> http://localhost:8080/swagger-ui/index.html
> if running on AAT, replace localhost with ingressHost data inside values.yaml class in the necessary component, making sure port number is also removed.

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

* Java11
* Spring boot
* Spring batch
* Junit, Mockito and SpringBootTest and Powermockito
* Gradle
* [lombok project](https://projectlombok.org/) - Lombok project
* Postgres
* Flyway

## License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details
