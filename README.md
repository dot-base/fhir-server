# Movebase FHIR Server
FHIR Server for the Movebase infrastructure.

[![GitHub release (latest by date)](https://img.shields.io/github/v/release/dot-base/fhir-server)](https://github.com/dot-base/fhir-server/releases)
[![GitHub Workflow Status](https://img.shields.io/github/workflow/status/dot-base/fhir-server/Docker?label=Docker%20Build&logo=Docker)](https://github.com/dot-base/fhir-server/packages/331005)


## Quick Nav
1. [Production Deployment](#Production-Deployment)
1. [Contributing](#Contributing)


## Production Deployment
Want a FHIR Server that supports profiles established throughout the Movebase project? The easiest way is to deploy our docker stack for that. Just follow the steps below to get started.

❗ This setup is meant for folks that just want to deploy the Movebase FHIR Server including a database. If you want to deploy an instance of the whole Movebase project see the [central Movebase repository](https://github.com/dot-base/dot-base).

[![Docker Build Status](https://img.shields.io/badge/We%20love-Docker-blue?style=flat&logo=Docker)](https://github.com/orgs/dot-base/packages)

### Requirements
- [Docker Engine >= v1.13](https://www.docker.com/get-started)

### Deployment
1. Create a docker swarm if you don't already have one:
    ```
    docker swarm init
    ```
1. Start both, a FHIR Server as well as a database container:
    ```
    curl https://raw.githubusercontent.com/dot-base/fhir-server/master/docker-compose.yml --output docker-compose.yml
    docker stack deploy -c docker-compose.yml fhir-server
    ```
1. Set the following environment variables on your production system. We advice auto generating a secure password and choosing a different username than `admin` ;).
    ```sh
    export FHIR_DB_USER="YOUR_DB_USER"
    export FHIR_DB_PASSWORD="YOUR_DB_PW"
    ```
1. Done and dusted 🎉. This will deploy two containers: a Movebase fhir server as well as a postgres database container.
1. [optional] Add these containers to your docker swarm or kubernetes config. Hint: You can use the `docker-compose.yml` as a template for this.

❗ Per default, port 8080 of the FHIR server container is exposed. You can change that, and other deployment options inside the docker-compose.yml. Remember to redeploy the stack after updating that file.


## Contributing

### Requirements
- [Java JDK >= v11](https://www.oracle.com/java/technologies/javase-jdk11-downloads.html)
- [Maven >= v3](https://maven.apache.org/download.cgi)
- [Docker Engine >= v1.13](https://www.docker.com/get-started)
- A local copy of this repository

### Running Locally
1. Create a docker swarm if you don't already have one:
    ```
    docker swarm init
    ```
1. Start a database container:
    ```
    docker stack deploy -c docker-compose-dev.yml dev-database
    ```
1. Start the development server
    ```
    mvn jetty:run
    ```
1. By default the server is available at http://localhost:8080.
1. Go and mix up some code 👩‍💻. The server will reload automatically once you save. Remember to keep an eye on the console.


FHIR® is the registered trademark of HL7. Use of the FHIR trademark does not constitute endorsement of this product by HL7.
