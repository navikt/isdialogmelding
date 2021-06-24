package no.nav.syfo.application

import io.ktor.application.*

data class Environment(
    val aadAppClient: String = getEnvVar("AZURE_APP_CLIENT_ID"),
    val azureAppWellKnownUrl: String = getEnvVar("AZURE_APP_WELL_KNOWN_URL"),
    val serviceuserUsername: String = getEnvVarAllowNull("SERVICEUSER_USERNAME"),
    val serviceuserPassword: String = getEnvVarAllowNull("SERVICEUSER_PASSWORD"),
    val mqChannelName: String = getEnvVarAllowNull("MQGATEWAY_CHANNEL_NAME", "DEV.APP.SVRCONN"),
    val mqHostname: String = getEnvVarAllowNull("MQGATEWAY_HOSTNAME", "localhost"),
    val mqQueueManager: String = getEnvVarAllowNull("MQGATEWAY_NAME", "QM1"),
    val mqPort: Int = getEnvVarAllowNull("MQGATEWAY_PORT", "1414").toInt(),
    val mqApplicationName: String = "isdialogmelding",
    val emottakQueuename: String = getEnvVarAllowNull("EMOTTAK_QUEUENAME"),
)

fun getEnvVar(varName: String, defaultValue: String? = null) =
    System.getenv(varName) ?: defaultValue ?: throw RuntimeException("Missing required variable \"$varName\"")

fun getEnvVarAllowNull(varName: String, defaultValue: String? = null) =
    System.getenv(varName) ?: defaultValue ?: ""

val Application.envKind get() = environment.config.property("ktor.environment").getString()

fun Application.isDev(block: () -> Unit) {
    if (envKind == "dev") block()
}

fun Application.isProd(block: () -> Unit) {
    if (envKind == "production") block()
}
