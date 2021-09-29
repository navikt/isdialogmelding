package no.nav.syfo.testhelper

import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.Environment
import java.net.ServerSocket

fun testEnvironment(
    azureOpenidConfigTokenEndpoint: String,
    fastlegeRestUrl: String,
    syfoPartnerinfoUrl: String,
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
)

fun testAppState() = ApplicationState(
    alive = true,
    ready = true,
)

fun getRandomPort() = ServerSocket(0).use {
    it.localPort
}
