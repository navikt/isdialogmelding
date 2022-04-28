package no.nav.syfo.testhelper

import io.ktor.server.netty.*
import no.nav.common.KafkaEnvironment
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.Environment
import no.nav.syfo.testhelper.mocks.*

class ExternalMockEnvironment private constructor() {
    val applicationState: ApplicationState = testAppState()
    val database = TestDatabase()
    private val embeddedEnvironment: KafkaEnvironment = testKafka()
    private val azureAdMock = AzureAdMock()

    private val fastlegeRestMock = FastlegeRestMock()
    private val syfopartnerInfoMock = SyfopartnerInfoMock()
    private val syfoTilgangskontrollMock = SyfoTilgangskontrollMock()
    private val pdlMock = PdlMock()

    val externalApplicationMockMap = hashMapOf(
        azureAdMock.name to azureAdMock.server,
        fastlegeRestMock.name to fastlegeRestMock.server,
        syfopartnerInfoMock.name to syfopartnerInfoMock.server,
        syfoTilgangskontrollMock.name to syfoTilgangskontrollMock.server,
        pdlMock.name to pdlMock.server,
    )

    val environment: Environment by lazy {
        testEnvironment(
            azureOpenidConfigTokenEndpoint = azureAdMock.url(),
            kafkaBootstrapServers = embeddedEnvironment.brokersURL,
            fastlegeRestUrl = fastlegeRestMock.url(),
            syfoPartnerinfoUrl = syfopartnerInfoMock.url(),
            syfoTilgangskontrollUrl = syfoTilgangskontrollMock.url(),
            pdlUrl = pdlMock.url(),
        )
    }
    val wellKnownInternalAzureAD = wellKnownInternalAzureAD()
    val wellKnownInternalIdportenTokenX = wellKnownInternalIdportenTokenX()

    companion object {
        val instance: ExternalMockEnvironment by lazy { ExternalMockEnvironment().also { it.externalApplicationMockMap.start() } }
    }
}

fun HashMap<String, NettyApplicationEngine>.start() {
    this.forEach {
        it.value.start()
    }
}
