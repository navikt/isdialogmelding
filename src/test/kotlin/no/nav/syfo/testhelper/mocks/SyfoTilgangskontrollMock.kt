package no.nav.syfo.testhelper.mocks

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.syfo.client.veiledertilgang.Tilgang
import no.nav.syfo.client.veiledertilgang.VeilederTilgangskontrollClient.Companion.TILGANGSKONTROLL_PERSON_PATH
import no.nav.syfo.testhelper.UserConstants.ARBEIDSTAKER_VEILEDER_NO_ACCESS
import no.nav.syfo.util.getPersonIdentHeader

class SyfoTilgangskontrollMock : MockServer() {
    override val name = "syfotilgangskontroll"
    override val routingConfiguration: Routing.() -> Unit = {
        get(TILGANGSKONTROLL_PERSON_PATH) {
            when (getPersonIdentHeader()) {
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
