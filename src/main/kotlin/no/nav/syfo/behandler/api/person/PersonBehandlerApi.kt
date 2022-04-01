package no.nav.syfo.behandler.api.person

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.syfo.application.api.authentication.personIdent
import no.nav.syfo.behandler.BehandlerService
import no.nav.syfo.behandler.api.person.access.PersonAPIConsumerAccessService
import no.nav.syfo.behandler.domain.*
import no.nav.syfo.util.getBearerHeader
import no.nav.syfo.util.getCallId
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val log: Logger = LoggerFactory.getLogger("no.nav.syfo")

const val personApiBehandlerPath = "/api/person/v1/behandler"
const val personBehandlerSelfPath = "/self"

fun Route.registerPersonBehandlerApi(
    behandlerService: BehandlerService,
    personAPIConsumerAccessService: PersonAPIConsumerAccessService,
) {
    route(personApiBehandlerPath) {
        get(personBehandlerSelfPath) {
            val callId = getCallId()
            try {
                val token = getBearerHeader()
                    ?: throw IllegalArgumentException("No Authorization header supplied")
                val requestPersonIdent = call.personIdent()
                    ?: throw IllegalArgumentException("No PersonIdent found in token")

                personAPIConsumerAccessService.validateConsumerApplicationClientId(
                    token = token,
                )

                val personBehandlerDTOList = mutableListOf<PersonBehandlerDTO>()
                val fastlege = behandlerService.getAktivFastlegeMedPartnerId(
                    personIdentNumber = requestPersonIdent,
                    systemRequest = true,
                    token = token,
                    callId = callId,
                )
                if (fastlege != null && fastlege.hasAnId()) {
                    val behandlerDialogmeldingArbeidstaker = BehandlerDialogmeldingArbeidstaker(
                        type = BehandlerType.FASTLEGE,
                        arbeidstakerPersonident = requestPersonIdent,
                    )
                    val behandler = behandlerService.createOrGetBehandler(
                        behandler = fastlege,
                        behandlerDialogmeldingArbeidstaker = behandlerDialogmeldingArbeidstaker,
                    )
                    personBehandlerDTOList.add(
                        behandler.toPersonBehandlerDTO(
                            behandlerType = BehandlerType.FASTLEGE,
                        )
                    )
                }
                call.respond(personBehandlerDTOList)
            } catch (e: IllegalArgumentException) {
                val illegalArgumentMessage = "Could not retrieve list of BehandlerDialogmelding for PersonIdent"
                log.warn("$illegalArgumentMessage: {}, {}", e.message, callId)
                call.respond(HttpStatusCode.BadRequest, e.message ?: illegalArgumentMessage)
            }
        }
    }
}
