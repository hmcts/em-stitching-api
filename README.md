# Evidence Management Stitching App

[![Build Status](https://travis-ci.org/hmcts/rpa-em-stitching-api.svg?branch=master)](https://travis-ci.org/hmcts/rpa-em-stitching-api)
[![codecov](https://codecov.io/gh/hmcts/rpa-em-stitching-api/branch/master/graph/badge.svg)](https://codecov.io/gh/hmcts/rpa-em-stitching-api)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

Stitching API is a backend service to merge Word/PDF documents.

# Setup

```
#Cloning repo and running though docker
git clone https://github.com/hmcts/rpa-em-stitching-api.git
cd rpa-em-stitching-api/
az login
az acr login --name hmcts --subscription 1c4f0704-a29e-403d-b719-b90c34ef14c9
docker-compose -f docker-compose-dependencies.yml pull
docker-compose -f docker-compose-dependencies.yml up
gradle migratePostgresDatabase
DOCMOSIS_ACCESS_KEY=xxx gradle bootRun
```
Note that your VPN needs to be on when running functional tests.

### Swagger UI
To view our REST API go to {HOST}:{PORT}/swagger-ui.html
> http://localhost:8080/swagger-ui.html

### API Endpoints
A list of our endpoints can be found here
> https://hmcts.github.io/reform-api-docs/swagger.html?url=https://hmcts.github.io/reform-api-docs/specs/rpa-em-stitching-api.json

### Tech
It uses:

* Java8
* Spring boot
* Junit, Mockito and SpringBootTest and Powermockito
* Gradle
* [lombok project](https://projectlombok.org/) - Lombok project

### Plugins
* [lombok plugin](https://plugins.jetbrains.com/idea/plugin/6317-lombok-plugin) - Lombok IDEA plugin

## License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details
