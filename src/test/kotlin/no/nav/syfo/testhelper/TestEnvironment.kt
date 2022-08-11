package no.nav.syfo.testhelper

import no.nav.syfo.application.*
import java.net.ServerSocket

fun testEnvironment(
    azureOpenidConfigTokenEndpoint: String,
    kafkaBootstrapServers: String,
    fastlegeRestUrl: String,
    syfoPartnerinfoUrl: String,
    syfoTilgangskontrollUrl: String,
    pdlUrl: String,
) = Environment(
    aadAppClient = "isdialogmelding-client-id",
    apprecQueueName = "apprec-test-queue",
    azureAppClientSecret = "isdialogmelding-secret",
    azureAppWellKnownUrl = "wellknown",
    azureOpenidConfigTokenEndpoint = azureOpenidConfigTokenEndpoint,
    idportenTokenXClientId = "dev-gcp.teamsykefravr.isdialogmelding",
    idportenTokenXWellKnownUrl = "wellknown-idporten-tokenx",
    aapSoknadApiClientId = testAapSoknadApiClientId,
    personAPIAuthorizedConsumerClientIdList = listOf(testAapSoknadApiClientId),
    electorPath = "/tmp",
    kafka = ApplicationEnvironmentKafka(
        aivenBootstrapServers = kafkaBootstrapServers,
        aivenCredstorePassword = "credstorepassord",
        aivenKeystoreLocation = "keystore",
        aivenSecurityProtocol = "SSL",
        aivenTruststoreLocation = "truststore",
    ),
    serviceuserUsername = "user",
    serviceuserPassword = "password",
    mqQueueManager = "mq-queue",
    mqHostname = "mq-host",
    mqPort = 2121,
    mqChannelName = "channel",
    emottakQueuename = "emottak-queue",
    fastlegeRestClientId = "fastlegerest-client-id",
    fastlegeRestUrl = fastlegeRestUrl,
    pdlClientId = "pdlclientid",
    pdlUrl = pdlUrl,
    syfoPartnerinfoClientId = "syfopartnerinfo-client-id",
    syfoPartnerinfoUrl = syfoPartnerinfoUrl,
    syfotilgangskontrollClientId = "syfo-tilgangskontroll-client-id",
    syfotilgangskontrollUrl = syfoTilgangskontrollUrl,
    isdialogmeldingDbHost = "localhost",
    isdialogmeldingDbPort = "5432",
    isdialogmeldingDbName = "isdialogmelding_dev",
    isdialogmeldingDbUsername = "username",
    isdialogmeldingDbPassword = "password",
    toggleApprecs = true,
    toggleKafkaProcessingSykmeldingEnabled = true,
    toggleKafkaProcessingDialogmeldingEnabled = true,
    toggleSykmeldingbehandlere = true,
)

const val testAapSoknadApiClientId = "soknad-api-client-id"

fun testAppState() = ApplicationState(
    alive = true,
    ready = true,
)

fun getRandomPort() = ServerSocket(0).use {
    it.localPort
}
