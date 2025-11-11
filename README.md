![Build status](https://github.com/navikt/isdialogmelding/workflows/main/badge.svg?branch=master)

# isdialogmelding

Applikasjon for håndtering av dialogmeldinger i SYFO-domenet. Funksjonalitet:

* Deling av oppfølgingsplan 
* Sending av dialogmøterelaterte dialogmeldinger
* Sending av andre dialogmeldinger
* API med informasjon om behandlere (fastlege) som kan motta dialogmelding

## Technologies used

* Docker
* Gradle
* Kotlin
* Ktor
* Postgres
* IBM MQ
* Kafka

##### Test Libraries:

* JUnit
* Mockk

#### Requirements

* JDK 21

### Build

Run `./gradlew clean shadowJar`

### Lint

Run checking `./gradlew --continue ktlintCheck`

Run formatting: `./gradlew ktlintFormat`

### Test

Run `./gradlew test -i`

### Run Application

#### Create Docker Image

Creating a docker image should be as simple as `docker build -t isdialogmelding .`

#### Run Docker Image

`docker run --rm -it -p 8080:8080 isdialogmelding`

### Kafka

This application owns and consumes from the following topic:

* isdialogmelding-behandler-dialogmelding-bestilling

### MQ config

This application uses an encrypted channel when reading av sending messages to MQ. The certificate and password for 
the jks-file can be found in Fasit. The resources must be stored in GCP as secrets:

`kubectl create secret generic isdialogmelding-keystore-pwd --from-literal MQ_KEYSTORE_PASSWORD=xxxxxxxx`

and

`kubectl create secret generic isdialogmelding-keystore --from-file isdialogmelding-keystore.jks=./srvisdialogmelding_p.jks`

## Contact

### For NAV employees

We are available at the Slack channel `#isyfo`.
