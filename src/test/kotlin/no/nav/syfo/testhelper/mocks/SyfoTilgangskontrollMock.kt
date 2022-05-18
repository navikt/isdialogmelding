package no.nav.syfo.testhelper.mocks

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.syfo.application.api.authentication.installContentNegotiation
import no.nav.syfo.client.veiledertilgang.Tilgang
import no.nav.syfo.client.veiledertilgang.VeilederTilgangskontrollClient.Companion.TILGANGSKONTROLL_PERSON_PATH
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_VEILEDER_NO_ACCESS
import no.nav.syfo.testhelper.getRandomPort
import no.nav.syfo.util.getPersonidentHeader

class SyfoTilgangskontrollMock {
    private val port = getRandomPort()
    val url = "http://localhost:$port"

    val name = "syfotilgangskontroll"
    val server = embeddedServer(
        factory = Netty,
        port = port,
    ) {
        installContentNegotiation()
        routing {
            get(TILGANGSKONTROLL_PERSON_PATH) {
                when (getPersonidentHeader()) {
                    ARBEIDSTAKER_VEILEDER_NO_ACCESS.value -> call.respond(
                        Tilgang(harTilgang = false)
                    )
                    else -> call.respond(
                        Tilgang(harTilgang = true)
                    )
                }
            }
        }
    }
}
