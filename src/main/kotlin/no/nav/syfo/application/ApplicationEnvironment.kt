package no.nav.syfo.application

import io.ktor.application.*

data class Environment(
    val aadAppClient: String = getEnvVar("AZURE_APP_CLIENT_ID"),
    val azureAppWellKnownUrl: String = getEnvVar("AZURE_APP_WELL_KNOWN_URL"),
    val serviceuserUsername: String = getEnvVar("SERVICEUSER_USERNAME"),
    val serviceuserPassword: String = getEnvVar("SERVICEUSER_PASSWORD"),
    val mqQueueManager: String = getEnvVar("MQGATEWAY_NAME"),
    val mqHostname: String = getEnvVar("MQGATEWAY_HOSTNAME"),
    val mqPort: Int = getEnvVar("MQGATEWAY_PORT", "1413").toInt(),
    val mqChannelName: String = getEnvVar("MQGATEWAY_CHANNEL_NAME"),
    val emottakQueuename: String = getEnvVar("MOTTAK_QUEUE_UTSENDING_QUEUENAME"),
    val mqApplicationName: String = "isdialogmelding",
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
