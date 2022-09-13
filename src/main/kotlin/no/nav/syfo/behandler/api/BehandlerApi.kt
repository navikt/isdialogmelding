package no.nav.syfo.behandler.api

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.syfo.behandler.BehandlerService
import no.nav.syfo.behandler.api.access.validateVeilederAccess
import no.nav.syfo.behandler.domain.*
import no.nav.syfo.client.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.domain.Personident
import no.nav.syfo.util.*

const val behandlerPath = "/api/v1/behandler"
const val behandlerPersonident = "/personident"
const val search = "/search"

fun Route.registerBehandlerApi(
    behandlerService: BehandlerService,
    veilederTilgangskontrollClient: VeilederTilgangskontrollClient,
) {
    route(behandlerPath) {
        get(behandlerPersonident) {
            val callId = getCallId()
            val token = getBearerHeader()
                ?: throw IllegalArgumentException("No Authorization header supplied")
            val personident = getPersonidentHeader()?.let { personident ->
                Personident(personident)
            } ?: throw IllegalArgumentException("No Personident supplied")

            validateVeilederAccess(
                action = "Read BehandlerList of Person with Personident",
                personidentToAccess = personident,
                veilederTilgangskontrollClient = veilederTilgangskontrollClient,
            ) {

                val behandlere = behandlerService.getBehandlere(
                    personident = personident,
                    token = token,
                    callId = callId,
                )

                val behandlerDTOList = behandlere.toBehandlerDTOList()

                call.respond(behandlerDTOList)
            }
        }
        get(search) {
            val callId = getCallId()
            val token = getBearerHeader()
                ?: throw IllegalArgumentException("No Authorization header supplied")
            val fornavn = this.call.request.headers["fornavn"]
                ?: throw IllegalArgumentException("No fornavn supplied")
            val etternavn = this.call.request.headers["etternavn"]
                ?: throw IllegalArgumentException("No etternavn supplied")
            val kontornavn = this.call.request.headers["kontornavn"]
                ?: throw IllegalArgumentException("No kontornavn supplied")
            val behandlere = behandlerService.searchBehandlere(
                behandlerFornavn = fornavn,
                behandlerEtternavn = etternavn,
                kontornavn = kontornavn,
            )
            call.respond(behandlere.toBehandlerDTOListUtenRelasjonstype())
        }
    }
}
