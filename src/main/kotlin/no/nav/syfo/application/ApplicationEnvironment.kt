package no.nav.syfo.application

import io.ktor.server.application.*
import no.nav.syfo.application.kafka.ApplicationEnvironmentKafka

data class Environment(
    val aadAppClient: String = getEnvVar("AZURE_APP_CLIENT_ID"),
    val azureAppClientSecret: String = getEnvVar("AZURE_APP_CLIENT_SECRET"),
    val azureAppPreAuthorizedApps: String = getEnvVar("AZURE_APP_PRE_AUTHORIZED_APPS"),
    val azureOpenidConfigTokenEndpoint: String = getEnvVar("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"),
    val azureAppWellKnownUrl: String = getEnvVar("AZURE_APP_WELL_KNOWN_URL"),

    val idportenTokenXClientId: String = getEnvVar("TOKEN_X_CLIENT_ID"),
    val idportenTokenXWellKnownUrl: String = getEnvVar("TOKEN_X_WELL_KNOWN_URL"),
    val aapSoknadApiClientId: String = getEnvVar("AAP_SOKNAD_API_CLIENT_ID"),
    val aapOppslagClientId: String = getEnvVar("AAP_OPPSLAG_CLIENT_ID"),
    val personAPIAuthorizedConsumerClientIdList: List<String> = listOf(
        aapSoknadApiClientId,
        aapOppslagClientId,
    ),
    val syfooppfolgingsplanserviceClientId: String = getEnvVar("SYFOOPPFOLGINGSPLANSERVICE_CLIENT_ID"),
    val oppfolgingsplanBackendClientId: String = getEnvVar("SYFO_OPPFOLGINGSPLAN_BACKEND_CLIENT_ID"),
    val oppfolgingsplanAPIAuthorizedConsumerClientIdList: List<String> = listOf(
        syfooppfolgingsplanserviceClientId,
        oppfolgingsplanBackendClientId,
    ),
    private val syfooppfolgingsplanserviceApplicationName: String = "syfooppfolgingsplanservice",
    private val lpsOppfolgingsplanMottakApplicationName: String = "lps-oppfolgingsplan-mottak",
    val oppfolgingsplanSystemAPIAuthorizedConsumerApplicationNameList: List<String> = listOf(
        syfooppfolgingsplanserviceApplicationName,
        lpsOppfolgingsplanMottakApplicationName,
    ),

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
        aivenSchemaRegistryUrl = getEnvVar("KAFKA_SCHEMA_REGISTRY"),
        aivenRegistryUser = getEnvVar("KAFKA_SCHEMA_REGISTRY_USER"),
        aivenRegistryPassword = getEnvVar("KAFKA_SCHEMA_REGISTRY_PASSWORD"),
    ),
    val mqQueueManager: String = getEnvVar("MQGATEWAY_NAME"),
    val mqHostname: String = getEnvVar("MQGATEWAY_HOSTNAME"),
    val mqPort: Int = getEnvVar("MQGATEWAY_PORT", "1413").toInt(),
    val mqChannelName: String = getEnvVar("MQGATEWAY_CHANNEL_NAME"),
    val mqKeystorePassword: String = getEnvVar("MQ_KEYSTORE_PASSWORD"),
    val mqKeystorePath: String = getEnvVar("MQ_KEYSTORE_PATH"),
    val apprecQueueName: String = getEnvVar("APPREC_QUEUE"),
    val emottakQueuename: String = getEnvVar("MOTTAK_QUEUE_UTSENDING_QUEUENAME"),
    val mqApplicationName: String = "isdialogmelding",
    val fastlegeRestClientId: String = getEnvVar("FASTLEGEREST_CLIENT_ID"),
    val fastlegeRestUrl: String = getEnvVar("FASTLEGEREST_URL"),
    val pdlClientId: String = getEnvVar("PDL_CLIENT_ID"),
    val pdlUrl: String = getEnvVar("PDL_URL"),
    val syfoPartnerinfoClientId: String = getEnvVar("SYFOPARTNERINFO_CLIENT_ID"),
    val syfoPartnerinfoUrl: String = getEnvVar("SYFOPARTNERINFO_URL"),
    val istilgangskontrollClientId: String = getEnvVar("ISTILGANGSKONTROLL_CLIENT_ID"),
    val istilgangskontrollUrl: String = getEnvVar("ISTILGANGSKONTROLL_URL"),
    val btsysClientId: String = getEnvVar("BTSYS_CLIENT_ID"),
    val btsysUrl: String = getEnvVar("BTSYS_ENDPOINT_URL"),
    val syfohelsenettproxyClientId: String = getEnvVar("SYFOHELSENETTPROXY_CLIENT_ID"),
    val syfohelsenettproxyUrl: String = getEnvVar("SYFOHELSENETTPROXY_URL"),
) {
    fun jdbcUrl(): String {
        return "jdbc:postgresql://$isdialogmeldingDbHost:$isdialogmeldingDbPort/$isdialogmeldingDbName"
    }
}

fun getEnvVar(varName: String, defaultValue: String? = null) =
    System.getenv(varName) ?: defaultValue ?: throw RuntimeException("Missing required variable \"$varName\"")

val Application.envKind get() = environment.config.property("ktor.environment").getString()

fun Application.isDev(block: () -> Unit) {
    if (envKind == "dev") block()
}

fun Application.isProd(block: () -> Unit) {
    if (envKind == "production") block()
}
