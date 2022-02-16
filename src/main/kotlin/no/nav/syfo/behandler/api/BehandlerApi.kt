package no.nav.syfo.behandler.api

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.syfo.behandler.BehandlerService
import no.nav.syfo.behandler.api.access.validateVeilederAccess
import no.nav.syfo.behandler.domain.hasAnId
import no.nav.syfo.behandler.domain.toBehandlerDialogmeldingDTO
import no.nav.syfo.client.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.domain.PersonIdentNumber
import no.nav.syfo.util.*
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
            val token = getBearerHeader()
                ?: throw IllegalArgumentException("No Authorization header supplied")
            val personIdentNumber = getPersonIdentHeader()?.let { personIdent ->
                PersonIdentNumber(personIdent)
            } ?: throw IllegalArgumentException("No PersonIdent supplied")

            validateVeilederAccess(
                action = "Read BehandlerList of Person with PersonIdent",
                personIdentToAccess = personIdentNumber,
                veilederTilgangskontrollClient = veilederTilgangskontrollClient,
            ) {
                val behandlerDialogmeldingDTOList = mutableListOf<BehandlerDialogmeldingDTO>()
                val fastlege = behandlerService.getAktivFastlegeMedPartnerId(
                    personIdentNumber = personIdentNumber,
                    token = token,
                    callId = callId,
                )
                if (fastlege != null && fastlege.hasAnId()) {
                    val behandler = behandlerService.createOrGetBehandler(
                        behandler = fastlege,
                        arbeidstakerPersonIdent = personIdentNumber,
                    )
                    behandlerDialogmeldingDTOList.add(behandler.toBehandlerDialogmeldingDTO())
                }
                call.respond(behandlerDialogmeldingDTOList)
            }
        }
    }
}
