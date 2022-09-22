package no.nav.syfo.behandler.api.person

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.syfo.application.api.authentication.personident
import no.nav.syfo.behandler.BehandlerService
import no.nav.syfo.behandler.DialogmeldingToBehandlerService
import no.nav.syfo.behandler.api.person.access.PersonAPIConsumerAccessService
import no.nav.syfo.oppfolgingsplan.OppfolgingsplanService
import no.nav.syfo.oppfolgingsplan.domain.createRSHodemelding
import no.nav.syfo.util.getBearerHeader
import no.nav.syfo.util.getCallId
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val log: Logger = LoggerFactory.getLogger("no.nav.syfo")

const val personApiOppfolgingsplanPath = "/api/person/v1/oppfolgingsplan"

fun Route.registerPersonOppfolgingsplanApi(
    behandlerService: BehandlerService,
    dialogmeldingToBehandlerService: DialogmeldingToBehandlerService,
    oppfolgingsplanService: OppfolgingsplanService,
    oppfolgingsplanAPIConsumerAccessService: PersonAPIConsumerAccessService,
) {
    route(personApiOppfolgingsplanPath) {
        post() {
            val callId = getCallId()
            try {
                val token = getBearerHeader()
                    ?: throw IllegalArgumentException("No Authorization header supplied")
                val requestPersonident = call.personident()
                    ?: throw IllegalArgumentException("No Personident found in token")

                oppfolgingsplanAPIConsumerAccessService.validateConsumerApplicationClientId(
                    token = token,
                )
                val oppfolgingsplan = call.receive<RSOppfolgingsplan>()
                if (requestPersonident.value != oppfolgingsplan.sykmeldtFnr) {
                    throw IllegalArgumentException("Feil ved sending av oppfølgingsplan, ugyldig fnr")
                }

                val fastlegeBehandler = behandlerService.getFastlegeBehandler(
                    personident = requestPersonident,
                    token = token,
                    callId = callId,
                    systemRequest = true,
                ) ?: throw IllegalArgumentException("Feil ved sending av oppfølgingsplan, FastlegeIkkeFunnet")

                val arbeidstaker = dialogmeldingToBehandlerService.getArbeidstakerIfRelasjonToBehandler(
                    behandlerRef = fastlegeBehandler.behandlerRef,
                    personident = requestPersonident,
                )

                val melding = createRSHodemelding(
                    behandler = fastlegeBehandler,
                    arbeidstaker = arbeidstaker,
                    oppfolgingsplan = oppfolgingsplan,
                )
                oppfolgingsplanService.sendMelding(melding)
                call.respond(HttpStatusCode.OK)
            } catch (e: IllegalArgumentException) {
                val illegalArgumentMessage = "Could not send oppfolgingsplan to behandler"
                log.warn("$illegalArgumentMessage: {}, {}", e.message, callId)
                call.respond(HttpStatusCode.BadRequest, e.message ?: illegalArgumentMessage)
            }
        }
    }
}
