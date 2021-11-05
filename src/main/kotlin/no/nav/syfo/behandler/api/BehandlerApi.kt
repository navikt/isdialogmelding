package no.nav.syfo.behandler.api

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.syfo.behandler.BehandlerService
import no.nav.syfo.behandler.domain.toBehandlerDialogmeldingDTO
import no.nav.syfo.client.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.domain.PersonIdentNumber
import no.nav.syfo.util.getBearerHeader
import no.nav.syfo.util.getCallId
import no.nav.syfo.util.getPersonIdentHeader
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val log: Logger = LoggerFactory.getLogger("no.nav.syfo")

const val behandlerPath = "/api/v1/behandler"
const val behandlerPersonident = "/personident"

fun Route.registerBehandlerApi(
    behandlerService: BehandlerService,
    veilederTilgangskontrollClient: VeilederTilgangskontrollClient,
) {
    route(behandlerPath) {
        get(behandlerPersonident) {
            val callId = getCallId()
            try {
                val token = getBearerHeader()
                    ?: throw IllegalArgumentException("No Authorization header supplied")
                val personIdentNumber = getPersonIdentHeader()?.let { personIdent ->
                    PersonIdentNumber(personIdent)
                } ?: throw IllegalArgumentException("No PersonIdent supplied")

                val hasAccess = veilederTilgangskontrollClient.hasAccess(
                    callId = callId,
                    personIdentNumber = personIdentNumber,
                    token = token,
                )
                if (hasAccess) {
                    val behandlere = behandlerService.getBehandlere(
                        personIdentNumber = personIdentNumber,
                        token = token,
                        callId = callId,
                    )
                    val behandlerDialogmeldingDTOList =
                        behandlere.map { behandler -> behandler.toBehandlerDialogmeldingDTO() }
                    call.respond(behandlerDialogmeldingDTOList)
                } else {
                    val accessDeniedMessage = "Denied Veileder access to PersonIdent"
                    log.warn("$accessDeniedMessage, {}", callId)
                    call.respond(HttpStatusCode.Forbidden, accessDeniedMessage)
                }
            } catch (e: IllegalArgumentException) {
                val illegalArgumentMessage = "Could not retrieve list of BehandlerDialogmelding for PersonIdent"
                log.warn("$illegalArgumentMessage: {}, {}", e.message, callId)
                call.respond(HttpStatusCode.BadRequest, e.message ?: illegalArgumentMessage)
            }
        }
    }
}