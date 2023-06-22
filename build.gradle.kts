import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.apache.tools.ant.taskdefs.condition.Os

group = "no.nav.syfo"
version = "1.0.0"

object Versions {
    const val confluent = "7.3.1"
    const val dialogmeldingVersion = "1.5d21db9"
    const val fellesformat2Version = "1.0329dd1"
    const val flyway = "9.7.0"
    const val hikari = "5.0.1"
    const val jackson = "2.14.0"
    const val jaxb = "2.3.1"
    const val kafka = "3.3.1"
    const val kafkaEmbedded = "3.2.1"
    const val kithApprecVersion = "2019.07.30-04-23-2a0d1388209441ec05d2e92a821eed4f796a3ae2"
    const val kithHodemeldingVersion = "2019.07.30-12-26-5c924ef4f04022bbb850aaf299eb8e4464c1ca6a"
    const val kluent = "1.72"
    const val ktor = "2.2.2"
    const val logback = "1.4.4"
    const val logstashEncoder = "7.2"
    const val micrometerRegistry = "1.10.0"
    const val mockk = "1.13.2"
    const val mq = "9.2.5.0"
    const val nimbusjosejwt = "9.25.6"
    const val postgres = "42.5.1"
    val postgresEmbedded = if (Os.isFamily(Os.FAMILY_MAC)) "1.0.0" else "0.13.4"
    const val scala = "2.13.9"
    const val spek = "2.0.19"
    const val syfotjenester = "1.2021.06.09-13.09-b3d30de9996e"
}

plugins {
    kotlin("jvm") version "1.8.22"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("org.jlleitschuh.gradle.ktlint") version "11.4.1"
}

val githubUser: String by project
val githubPassword: String by project
repositories {
    mavenCentral()
    maven(url = "https://packages.confluent.io/maven/")
    maven(url = "https://jitpack.io")
    maven {
        url = uri("https://maven.pkg.github.com/navikt/syfotjenester")
        credentials {
            username = githubUser
            password = githubPassword
        }
    }
    maven {
        url = uri("https://maven.pkg.github.com/navikt/syfo-xml-codegen")
        credentials {
            username = githubUser
            password = githubPassword
        }
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))

    implementation("io.ktor:ktor-client-apache:${Versions.ktor}")
    implementation("io.ktor:ktor-client-cio:${Versions.ktor}")
    implementation("io.ktor:ktor-client-content-negotiation:${Versions.ktor}")
    implementation("io.ktor:ktor-serialization-jackson:${Versions.ktor}")
    implementation("io.ktor:ktor-server-auth-jwt:${Versions.ktor}")
    implementation("io.ktor:ktor-server-content-negotiation:${Versions.ktor}")
    implementation("io.ktor:ktor-server-netty:${Versions.ktor}")
    implementation("io.ktor:ktor-server-status-pages:${Versions.ktor}")

    implementation("ch.qos.logback:logback-classic:${Versions.logback}")
    implementation("net.logstash.logback:logstash-logback-encoder:${Versions.logstashEncoder}")

    // Metrics and Prometheus
    implementation("io.ktor:ktor-server-metrics-micrometer:${Versions.ktor}")
    implementation("io.micrometer:micrometer-registry-prometheus:${Versions.micrometerRegistry}")

    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:${Versions.jackson}")
    implementation("javax.xml.bind:jaxb-api:${Versions.jaxb}")
    implementation("org.glassfish.jaxb:jaxb-runtime:${Versions.jaxb}")

    // MQ
    implementation("com.ibm.mq:com.ibm.mq.allclient:${Versions.mq}")

    implementation("no.nav.helse.xml:xmlfellesformat2:${Versions.fellesformat2Version}")
    implementation("no.nav.helse.xml:kith-hodemelding:${Versions.kithHodemeldingVersion}")
    implementation("no.nav.helse.xml:kith-apprec:${Versions.kithApprecVersion}")
    implementation("no.nav.helse.xml:dialogmelding:${Versions.dialogmeldingVersion}")

    implementation("no.nav.syfotjenester:fellesformat:${Versions.syfotjenester}")
    implementation("no.nav.syfotjenester:kith-base64:${Versions.syfotjenester}")
    implementation("no.nav.syfotjenester:kith-dialogmelding:${Versions.syfotjenester}")
    implementation("no.nav.syfotjenester:kith-hodemelding:${Versions.syfotjenester}")

    // Kafka
    val excludeLog4j = fun ExternalModuleDependency.() {
        exclude(group = "log4j")
    }
    implementation("org.apache.kafka:kafka_2.13:${Versions.kafka}", excludeLog4j)
    implementation("io.confluent:kafka-avro-serializer:${Versions.confluent}", excludeLog4j)
    implementation("org.scala-lang:scala-library") {
        version {
            strictly(Versions.scala)
        }
    }
    testImplementation("no.nav:kafka-embedded-env:${Versions.kafkaEmbedded}", excludeLog4j)
    constraints {
        implementation("org.eclipse.jetty.http2:http2-server") {
            because("no.nav:kafka-embedded-env:${Versions.kafkaEmbedded} -> https://advisory.checkmarx.net/advisory/vulnerability/CVE-2022-2048/")
            version {
                require("9.4.48.v20220622")
            }
        }
        implementation("org.yaml:snakeyaml") {
            because("no.nav:kafka-embedded-env:${Versions.kafkaEmbedded} -> https://advisory.checkmarx.net/advisory/vulnerability/CVE-2022-25857/")
            version {
                require("1.33")
            }
        }
        implementation("com.google.protobuf:protobuf-java") {
            because("no.nav:kafka-embedded-env:${Versions.kafkaEmbedded} -> https://cwe.mitre.org/data/definitions/400.html")
            version {
                require("3.21.7")
            }
        }
    }

    // Database
    implementation("org.flywaydb:flyway-core:${Versions.flyway}")
    implementation("com.zaxxer:HikariCP:${Versions.hikari}")
    implementation("org.postgresql:postgresql:${Versions.postgres}")
    testImplementation("com.opentable.components:otj-pg-embedded:${Versions.postgresEmbedded}")

    testImplementation("com.nimbusds:nimbus-jose-jwt:${Versions.nimbusjosejwt}")
    testImplementation("io.ktor:ktor-server-test-host:${Versions.ktor}")
    testImplementation("io.mockk:mockk:${Versions.mockk}")
    testImplementation("org.amshove.kluent:kluent:${Versions.kluent}")
    testImplementation("org.spekframework.spek2:spek-dsl-jvm:${Versions.spek}") {
        exclude(group = "org.jetbrains.kotlin")
    }
    testRuntimeOnly("org.spekframework.spek2:spek-runner-junit5:${Versions.spek}") {
        exclude(group = "org.jetbrains.kotlin")
    }
}

kotlin {
    jvmToolchain(17)
}

tasks {
    withType<Jar> {
        manifest.attributes["Main-Class"] = "no.nav.syfo.AppKt"
    }

    create("printVersion") {
        doLast {
            println(project.version)
        }
    }

    withType<ShadowJar> {
        archiveBaseName.set("app")
        archiveClassifier.set("")
        archiveVersion.set("")
    }

    withType<Test> {
        useJUnitPlatform {
            includeEngines("spek2")
        }
        testLogging.showStandardStreams = true
    }
}
