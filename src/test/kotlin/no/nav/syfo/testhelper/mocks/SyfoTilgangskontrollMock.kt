package no.nav.syfo.testhelper.mocks

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.syfo.application.api.authentication.installContentNegotiation
import no.nav.syfo.client.veiledertilgang.Tilgang
import no.nav.syfo.client.veiledertilgang.VeilederTilgangskontrollClient.Companion.TILGANGSKONTROLL_PERSON_PATH
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_VEILEDER_NO_ACCESS
import no.nav.syfo.testhelper.getRandomPort
import no.nav.syfo.util.getPersonIdentHeader

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
                when (getPersonIdentHeader()) {
                    ARBEIDSTAKER_VEILEDER_NO_ACCESS.value -> call.respond(Tilgang(false, "Ingen tilgang"))
                    else -> call.respond(Tilgang(true, ""))
                }
            }
        }
    }
}
