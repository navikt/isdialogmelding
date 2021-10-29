package no.nav.syfo.testhelper

import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.Environment
import java.net.ServerSocket

fun testEnvironment(
    azureOpenidConfigTokenEndpoint: String,
    fastlegeRestUrl: String,
    syfoPartnerinfoUrl: String,
    syfoTilgangskontrollUrl: String,
) = Environment(
    aadAppClient = "isdialogmelding-client-id",
    azureAppClientSecret = "isdialogmelding-secret",
    azureAppWellKnownUrl = "wellknown",
    azureOpenidConfigTokenEndpoint = azureOpenidConfigTokenEndpoint,
    serviceuserUsername = "user",
    serviceuserPassword = "password",
    mqQueueManager = "mq-queue",
    mqHostname = "mq-host",
    mqPort = 2121,
    mqChannelName = "channel",
    emottakQueuename = "emottak-queue",
    fastlegeRestClientId = "fastlegerest-client-id",
    fastlegeRestUrl = fastlegeRestUrl,
    syfoPartnerinfoClientId = "syfopartnerinfo-client-id",
    syfoPartnerinfoUrl = syfoPartnerinfoUrl,
    syfotilgangskontrollClientId = "syfo-tilgangskontroll-client-id",
    syfotilgangskontrollUrl = syfoTilgangskontrollUrl,
    isdialogmeldingDbHost = "localhost",
    isdialogmeldingDbPort = "5432",
    isdialogmeldingDbName = "isdialogmelding_dev",
    isdialogmeldingDbUsername = "username",
    isdialogmeldingDbPassword = "password",
)

fun testAppState() = ApplicationState(
    alive = true,
    ready = true,
)

fun getRandomPort() = ServerSocket(0).use {
    it.localPort
}
