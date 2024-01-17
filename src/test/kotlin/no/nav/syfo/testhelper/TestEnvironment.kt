package no.nav.syfo.testhelper

import no.nav.syfo.application.*
import no.nav.syfo.application.kafka.ApplicationEnvironmentKafka
import no.nav.syfo.behandler.api.person.access.PreAuthorizedClient
import no.nav.syfo.util.configuredJacksonMapper
import java.net.ServerSocket

fun testEnvironment() = Environment(
    aadAppClient = "isdialogmelding-client-id",
    azureAppClientSecret = "isdialogmelding-secret",
    azureAppPreAuthorizedApps = configuredJacksonMapper().writeValueAsString(testAzureAppPreAuthorizedApps),
    azureOpenidConfigTokenEndpoint = "azureOpenidConfigTokenEndpoint",
    azureAppWellKnownUrl = "wellknown",
    idportenTokenXClientId = "dev-gcp.teamsykefravr.isdialogmelding",
    idportenTokenXWellKnownUrl = "wellknown-idporten-tokenx",
    aapSoknadApiClientId = testAapSoknadApiClientId,
    aapOppslagClientId = testAapOppslagApiClientId,
    personAPIAuthorizedConsumerClientIdList = listOf(testAapSoknadApiClientId, testAapOppslagApiClientId),
    syfooppfolgingsplanserviceClientId = testSyfooppfolgingsplanserviceClientId,
    oppfolgingsplanAPIAuthorizedConsumerClientIdList = listOf(testSyfooppfolgingsplanserviceClientId),
    electorPath = "/tmp",
    serviceuserUsername = "user",
    serviceuserPassword = "password",
    isdialogmeldingDbHost = "localhost",
    isdialogmeldingDbPort = "5432",
    isdialogmeldingDbName = "isdialogmelding_dev",
    isdialogmeldingDbUsername = "username",
    isdialogmeldingDbPassword = "password",
    kafka = ApplicationEnvironmentKafka(
        aivenBootstrapServers = "kafkaBootstrapServers",
        aivenCredstorePassword = "credstorepassord",
        aivenKeystoreLocation = "keystore",
        aivenSecurityProtocol = "SSL",
        aivenTruststoreLocation = "truststore",
        aivenSchemaRegistryUrl = "http://kafka-schema-registry.tpa.svc.nais.local:8081",
        aivenRegistryUser = "registryuser",
        aivenRegistryPassword = "registrypassword",
    ),
    mqQueueManager = "mq-queue",
    mqHostname = "mq-host",
    mqPort = 2121,
    mqChannelName = "channel",
    mqKeystorePassword = "pw",
    mqKeystorePath = "",
    apprecQueueName = "apprec-test-queue",
    emottakQueuename = "emottak-queue",
    fastlegeRestClientId = "fastlegerest-client-id",
    fastlegeRestUrl = "fastlegeRestUrl",
    pdlClientId = "pdlclientid",
    pdlUrl = "pdlUrl",
    syfoPartnerinfoClientId = "syfopartnerinfo-client-id",
    syfoPartnerinfoUrl = "syfoPartnerinfoUrl",
    istilgangskontrollClientId = "istilgangskontroll-client-id",
    istilgangskontrollUrl = "isTilgangskontrollUrl",
    btsysClientId = "btsys-client-id",
    btsysUrl = "btsysUrl",
)

const val testAapSoknadApiClientId = "soknad-api-client-id"
const val testAapOppslagApiClientId = "oppslag-client-id"
const val testSyfooppfolgingsplanserviceClientId = "syfooppfolgingsplanservice-client-id"

fun testAppState() = ApplicationState(
    alive = true,
    ready = true,
)

val testAzureAppPreAuthorizedApps = listOf(
    PreAuthorizedClient(
        name = "dev-fss:team-esyfo:syfooppfolgingsplanservice",
        clientId = testSyfooppfolgingsplanserviceClientId,
    ),
)

fun getRandomPort() = ServerSocket(0).use {
    it.localPort
}
