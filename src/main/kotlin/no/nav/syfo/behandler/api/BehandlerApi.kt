package no.nav.syfo.behandler.api

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.syfo.behandler.BehandlerService
import no.nav.syfo.behandler.api.access.validateVeilederAccess
import no.nav.syfo.behandler.domain.*
import no.nav.syfo.client.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.domain.Personident
import no.nav.syfo.util.*
import java.util.*

const val behandlerPath = "/api/v1/behandler"
const val behandlerPersonident = "/personident"
const val behandlerRef = "/behandler-ref"
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
            withValidToken {
                val search = this.call.request.headers["searchstring"]
                    ?: throw IllegalArgumentException("No searchstring supplied")
                val behandlere = behandlerService.searchBehandlere(
                    searchStrings = search,
                )
                call.respond(behandlere.toBehandlerDTOListUtenRelasjonstype())
            }
        }
        get(behandlerRef) {
            withValidToken {
                val behandlerRef: String = this.call.request.headers["behandlerRef"]
                    ?: throw IllegalArgumentException("No behandlerRef supplied")
                val behandler = behandlerService.getBehandler(
                    behandlerRef = UUID.fromString(behandlerRef)
                )
                if (behandler != null) {
                    call.respond(behandler.toBehandlerDTO(behandlerType = null))
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }
    }
}
