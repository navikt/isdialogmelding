package no.nav.syfo.testhelper

import io.ktor.server.netty.*
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.testhelper.mocks.*

class ExternalMockEnvironment {
    val applicationState: ApplicationState = testAppState()
    val database = TestDatabase()
    private val azureAdMock = AzureAdMock()

    private val fastlegeRestMock = FastlegeRestMock()
    private val syfopartnerInfoMock = SyfopartnerInfoMock()
    private val syfoTilgangskontrollMock = SyfoTilgangskontrollMock()

    val externalApplicationMockMap = hashMapOf(
        azureAdMock.name to azureAdMock.server,
        fastlegeRestMock.name to fastlegeRestMock.server,
        syfopartnerInfoMock.name to syfopartnerInfoMock.server,
        syfoTilgangskontrollMock.name to syfoTilgangskontrollMock.server
    )

    val environment = testEnvironment(
        azureOpenidConfigTokenEndpoint = azureAdMock.url,
        fastlegeRestUrl = fastlegeRestMock.url,
        syfoPartnerinfoUrl = syfopartnerInfoMock.url,
        syfoTilgangskontrollUrl = syfoTilgangskontrollMock.url
    )
    val wellKnownInternalAzureAD = wellKnownInternalAzureAD()
}

fun ExternalMockEnvironment.startExternalMocks() {
    this.externalApplicationMockMap.start()
}

fun ExternalMockEnvironment.stopExternalMocks() {
    this.externalApplicationMockMap.stop()
    this.database.stop()
}

fun HashMap<String, NettyApplicationEngine>.start() {
    this.forEach {
        it.value.start()
    }
}

fun HashMap<String, NettyApplicationEngine>.stop(
    gracePeriodMillis: Long = 1L,
    timeoutMillis: Long = 10L,
) {
    this.forEach {
        it.value.stop(gracePeriodMillis, timeoutMillis)
    }
}
