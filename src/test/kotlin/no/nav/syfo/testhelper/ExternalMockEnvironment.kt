package no.nav.syfo.testhelper

import io.ktor.server.netty.*
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.testhelper.mocks.AzureAdMock
import no.nav.syfo.testhelper.mocks.FastlegeRestMock
import no.nav.syfo.testhelper.mocks.SyfopartnerInfoMock
import no.nav.syfo.testhelper.mocks.wellKnownInternalAzureAD

class ExternalMockEnvironment {
    val applicationState: ApplicationState = testAppState()
    private val azureAdMock = AzureAdMock()

    val fastlegeRestMock = FastlegeRestMock()
    val syfopartnerInfoMock = SyfopartnerInfoMock()

    val externalApplicationMockMap = hashMapOf(
        azureAdMock.name to azureAdMock.server,
        fastlegeRestMock.name to fastlegeRestMock.server,
        syfopartnerInfoMock.name to syfopartnerInfoMock.server
    )

    val environment = testEnvironment(
        azureOpenidConfigTokenEndpoint = azureAdMock.url,
        fastlegeRestUrl = fastlegeRestMock.url,
        syfoPartnerinfoUrl = syfopartnerInfoMock.url,
    )
    val wellKnownInternalAzureAD = wellKnownInternalAzureAD()
}

fun ExternalMockEnvironment.startExternalMocks() {
    this.externalApplicationMockMap.start()
}

fun ExternalMockEnvironment.stopExternalMocks() {
    this.externalApplicationMockMap.stop()
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
