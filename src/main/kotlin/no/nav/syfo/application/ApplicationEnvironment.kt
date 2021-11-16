package no.nav.syfo.application

import io.ktor.application.*

data class Environment(
    val aadAppClient: String = getEnvVar("AZURE_APP_CLIENT_ID"),
    val azureAppClientSecret: String = getEnvVar("AZURE_APP_CLIENT_SECRET"),
    val azureOpenidConfigTokenEndpoint: String = getEnvVar("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"),
    val azureAppWellKnownUrl: String = getEnvVar("AZURE_APP_WELL_KNOWN_URL"),
    val electorPath: String = getEnvVar("ELECTOR_PATH"),
    val serviceuserUsername: String = getEnvVar("SERVICEUSER_USERNAME"),
    val serviceuserPassword: String = getEnvVar("SERVICEUSER_PASSWORD"),
    val isdialogmeldingDbHost: String = getEnvVar("NAIS_DATABASE_ISDIALOGMELDING_ISDIALOGMELDING_DB_HOST"),
    val isdialogmeldingDbPort: String = getEnvVar("NAIS_DATABASE_ISDIALOGMELDING_ISDIALOGMELDING_DB_PORT"),
    val isdialogmeldingDbName: String = getEnvVar("NAIS_DATABASE_ISDIALOGMELDING_ISDIALOGMELDING_DB_DATABASE"),
    val isdialogmeldingDbUsername: String = getEnvVar("NAIS_DATABASE_ISDIALOGMELDING_ISDIALOGMELDING_DB_USERNAME"),
    val isdialogmeldingDbPassword: String = getEnvVar("NAIS_DATABASE_ISDIALOGMELDING_ISDIALOGMELDING_DB_PASSWORD"),
    val kafka: ApplicationEnvironmentKafka = ApplicationEnvironmentKafka(
        aivenBootstrapServers = getEnvVar("KAFKA_BROKERS"),
        aivenCredstorePassword = getEnvVar("KAFKA_CREDSTORE_PASSWORD"),
        aivenKeystoreLocation = getEnvVar("KAFKA_KEYSTORE_PATH"),
        aivenSecurityProtocol = "SSL",
        aivenTruststoreLocation = getEnvVar("KAFKA_TRUSTSTORE_PATH"),
    ),
    val mqQueueManager: String = getEnvVar("MQGATEWAY_NAME"),
    val mqHostname: String = getEnvVar("MQGATEWAY_HOSTNAME"),
    val mqPort: Int = getEnvVar("MQGATEWAY_PORT", "1413").toInt(),
    val mqChannelName: String = getEnvVar("MQGATEWAY_CHANNEL_NAME"),
    val emottakQueuename: String = getEnvVar("MOTTAK_QUEUE_UTSENDING_QUEUENAME"),
    val mqApplicationName: String = "isdialogmelding",
    val fastlegeRestClientId: String = getEnvVar("FASTLEGEREST_CLIENT_ID"),
    val fastlegeRestUrl: String = getEnvVar("FASTLEGEREST_URL"),
    val syfoPartnerinfoClientId: String = getEnvVar("SYFOPARTNERINFO_CLIENT_ID"),
    val syfoPartnerinfoUrl: String = getEnvVar("SYFOPARTNERINFO_URL"),
    val syfotilgangskontrollClientId: String = getEnvVar("SYFOTILGANGSKONTROLL_CLIENT_ID"),
    val syfotilgangskontrollUrl: String = getEnvVar("SYFOTILGANGSKONTROLL_URL"),
    val toggleKafkaProcessingEnabled: Boolean = getEnvVar("TOGGLE_KAFKA_PROCESSING_ENABLED").toBoolean(),
) {
    fun jdbcUrl(): String {
        return "jdbc:postgresql://$isdialogmeldingDbHost:$isdialogmeldingDbPort/$isdialogmeldingDbName"
    }
}

data class ApplicationEnvironmentKafka(
    val aivenBootstrapServers: String,
    val aivenCredstorePassword: String,
    val aivenKeystoreLocation: String,
    val aivenSecurityProtocol: String,
    val aivenTruststoreLocation: String,
)

fun getEnvVar(varName: String, defaultValue: String? = null) =
    System.getenv(varName) ?: defaultValue ?: throw RuntimeException("Missing required variable \"$varName\"")

val Application.envKind get() = environment.config.property("ktor.environment").getString()

fun Application.isDev(block: () -> Unit) {
    if (envKind == "dev") block()
}

fun Application.isProd(block: () -> Unit) {
    if (envKind == "production") block()
}
