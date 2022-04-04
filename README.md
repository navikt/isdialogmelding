![Build status](https://github.com/navikt/isdialogmelding/workflows/main/badge.svg?branch=master)

# isdialogmelding

Applikasjon for håndtering av dialogmeldinger i SYFO-domenet. Funksjonalitet:

* Deling av oppfølgingsplan
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

* Kluent
* Mockk
* Spek

#### Requirements

* JDK 17

## Download packages from Github Package Registry

Certain packages (syfotjenester) must be downloaded from Github Package Registry, which requires authentication. The
packages can be downloaded via build.gradle:

```
val githubUser: String by project
val githubPassword: String by project
repositories {
    jcenter()
    maven(url = "https://packages.confluent.io/maven/")
    maven(url = "https://jitpack.io")
    maven {
        url = uri("https://maven.pkg.github.com/navikt/syfotjenester")
        credentials {
            username = githubUser
            password = githubPassword
        }
    }
}
```

`githubUser` and `githubPassword` are properties that are set in `~/.gradle/gradle.properties`:

```
githubUser=x-access-token
githubPassword=<token>
```

Where `<token>` is a personal access token with scope `read:packages`(and SSO enabled).

The variables can alternatively be configured as environment variables or used in the command lines:

* `ORG_GRADLE_PROJECT_githubUser`
* `ORG_GRADLE_PROJECT_githubPassword`

```
./gradlew -PgithubUser=x-access-token -PgithubPassword=[token]
```

### Build

Run `./gradlew clean shadowJar`

### Lint

Run `./gradlew --continue ktlintCheck`

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

## Contact

### For NAV employees

We are available at the Slack channel `#isyfo`.
