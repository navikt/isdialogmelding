
group = "no.nav.syfo"
version = "1.0.0"

val confluentVersion = "7.6.1"
val dialogmeldingVersion = "1.5d21db9"
val fellesformat2Version = "1.0329dd1"
val flywayVersion = "10.17.3"
val hikariVersion = "5.1.0"
val jacksonVersion = "2.18.2"
val jaxbVersion = "2.3.1"
val jsonVersion = "20240303"
val kafkaVersion = "3.7.0"
val kithApprecVersion = "2019.07.30-04-23-2a0d1388209441ec05d2e92a821eed4f796a3ae2"
val kithHodemeldingVersion = "2019.07.30-12-26-5c924ef4f04022bbb850aaf299eb8e4464c1ca6a"
val kluentVersion = "1.73"
val ktorVersion = "3.0.2"
val logbackVersion = "1.5.12"
val logstashEncoderVersion = "7.4"
val micrometerRegistryVersion = "1.12.7"
val mockkVersion = "1.13.13"
val mqVersion = "9.3.4.1"
val nimbusjosejwtVersion = "9.47"
val postgresVersion = "42.7.4"
val postgresEmbeddedVersion = "2.1.0"
val spekVersion = "2.0.19"
val syfotjenesterVersion = "1.2021.06.09-13.09-b3d30de9996e"

plugins {
    kotlin("jvm") version "2.1.0"
    id("com.gradleup.shadow") version "8.3.5"
    id("org.jlleitschuh.gradle.ktlint") version "11.4.2"
}

repositories {
    mavenCentral()
    maven(url = "https://packages.confluent.io/maven/")
    maven(url = "https://jitpack.io")
    maven {
        url = uri("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))

    implementation("io.ktor:ktor-client-apache:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")

    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashEncoderVersion")
    implementation("org.json:json:$jsonVersion")

    // Metrics and Prometheus
    implementation("io.ktor:ktor-server-metrics-micrometer:$ktorVersion")
    implementation("io.micrometer:micrometer-registry-prometheus:$micrometerRegistryVersion")

    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    implementation("javax.xml.bind:jaxb-api:$jaxbVersion")
    implementation("org.glassfish.jaxb:jaxb-runtime:$jaxbVersion")

    // MQ
    implementation("com.ibm.mq:com.ibm.mq.allclient:$mqVersion")

    implementation("no.nav.helse.xml:xmlfellesformat2:$fellesformat2Version")
    implementation("no.nav.helse.xml:kith-hodemelding:$kithHodemeldingVersion")
    implementation("no.nav.helse.xml:kith-apprec:$kithApprecVersion")
    implementation("no.nav.helse.xml:dialogmelding:$dialogmeldingVersion")

    implementation("no.nav.syfotjenester:fellesformat:$syfotjenesterVersion")
    implementation("no.nav.syfotjenester:kith-base64:$syfotjenesterVersion")
    implementation("no.nav.syfotjenester:kith-dialogmelding:$syfotjenesterVersion")
    implementation("no.nav.syfotjenester:kith-hodemelding:$syfotjenesterVersion")

    // Kafka
    val excludeLog4j = fun ExternalModuleDependency.() {
        exclude(group = "log4j")
    }
    implementation("org.apache.kafka:kafka_2.13:$kafkaVersion", excludeLog4j)
    implementation("io.confluent:kafka-avro-serializer:$confluentVersion", excludeLog4j)
    constraints {
        implementation("org.apache.avro:avro") {
            because("org.apache.avro:avro:1.11.0 -> https://www.cve.org/CVERecord?id=CVE-2023-39410")
            version {
                require("1.11.3")
            }
        }
        implementation("org.apache.commons:commons-compress") {
            because("org.apache.commons:commons-compress:1.22 -> https://www.cve.org/CVERecord?id=CVE-2012-2098")
            version {
                require("1.26.0")
            }
        }
        implementation("com.google.guava:guava") {
            because("com.google.guava:guava:30.1.1-jre -> https://www.cve.org/CVERecord?id=CVE-2020-8908")
            version {
                require("32.1.3-jre")
            }
        }
    }

    // Database
    implementation("org.flywaydb:flyway-database-postgresql:$flywayVersion")
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("org.postgresql:postgresql:$postgresVersion")
    testImplementation("io.zonky.test:embedded-postgres:$postgresEmbeddedVersion")

    testImplementation("com.nimbusds:nimbus-jose-jwt:$nimbusjosejwtVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")
    testImplementation("org.amshove.kluent:kluent:$kluentVersion")
    testImplementation("org.spekframework.spek2:spek-dsl-jvm:$spekVersion") {
        exclude(group = "org.jetbrains.kotlin")
    }
    testRuntimeOnly("org.spekframework.spek2:spek-runner-junit5:$spekVersion") {
        exclude(group = "org.jetbrains.kotlin")
    }
}

kotlin {
    jvmToolchain(21)
}

tasks {
    jar {
        manifest.attributes["Main-Class"] = "no.nav.syfo.AppKt"
    }

    create("printVersion") {
        doLast {
            println(project.version)
        }
    }

    shadowJar {
        mergeServiceFiles()
        archiveBaseName.set("app")
        archiveClassifier.set("")
        archiveVersion.set("")
    }

    test {
        useJUnitPlatform {
            includeEngines("spek2")
        }
        testLogging.showStandardStreams = true
    }
}
