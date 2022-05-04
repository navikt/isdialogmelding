package no.nav.syfo.behandler.api

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.syfo.behandler.BehandlerService
import no.nav.syfo.behandler.api.access.validateVeilederAccess
import no.nav.syfo.behandler.domain.*
import no.nav.syfo.client.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.domain.PersonIdentNumber
import no.nav.syfo.util.*
import java.time.OffsetDateTime

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
                val behandlerDTOList = mutableListOf<BehandlerDTO>()
                val fastlege = behandlerService.getAktivFastlegeBehandler(
                    personIdentNumber = personIdentNumber,
                    token = token,
                    callId = callId,
                )
                if (fastlege != null && fastlege.hasAnId()) {
                    val behandlerArbeidstakerRelasjon = BehandlerArbeidstakerRelasjon(
                        type = BehandlerArbeidstakerRelasjonstype.FASTLEGE,
                        arbeidstakerPersonident = personIdentNumber,
                        mottatt = OffsetDateTime.now(),
                    )
                    val behandler = behandlerService.createOrGetBehandler(
                        behandler = fastlege,
                        behandlerArbeidstakerRelasjon = behandlerArbeidstakerRelasjon,
                    )
                    behandlerDTOList.add(
                        behandler.toBehandlerDTO(
                            behandlerType = BehandlerArbeidstakerRelasjonstype.FASTLEGE,
                        )
                    )
                }
                call.respond(behandlerDTOList)
            }
        }
    }
}
