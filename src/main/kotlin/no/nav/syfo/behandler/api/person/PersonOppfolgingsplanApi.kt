package no.nav.syfo.behandler.api.person

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.syfo.application.api.APIConsumerAccessService
import no.nav.syfo.application.api.authentication.personident
import no.nav.syfo.oppfolgingsplan.OppfolgingsplanService
import no.nav.syfo.oppfolgingsplan.exception.FastlegeNotFoundException
import no.nav.syfo.util.getBearerHeader
import no.nav.syfo.util.getCallId
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val log: Logger = LoggerFactory.getLogger("no.nav.syfo")

const val personApiOppfolgingsplanPath = "/api/person/v1/oppfolgingsplan"

fun Route.registerPersonOppfolgingsplanApi(
    oppfolgingsplanService: OppfolgingsplanService,
    apiConsumerAccessService: APIConsumerAccessService,
) {
    route(personApiOppfolgingsplanPath) {
        post() {
            val callId = getCallId()
            try {
                val token = getBearerHeader()
                    ?: throw IllegalArgumentException("No Authorization header supplied")
                val requestPersonident = call.personident()
                    ?: throw IllegalArgumentException("No Personident found in token")

                apiConsumerAccessService.validateConsumerApplicationClientId(
                    token = token,
                )
                val oppfolgingsplan = call.receive<RSOppfolgingsplan>()

                oppfolgingsplanService.sendOppfolgingsplan(
                    callId = callId,
                    token = token,
                    oppfolgingsplan = oppfolgingsplan,
                )
                call.respond(HttpStatusCode.OK)
            } catch (e: FastlegeNotFoundException) {
                call.respond(HttpStatusCode.NotFound, e.message!!)
            } catch (e: IllegalArgumentException) {
                val illegalArgumentMessage = "Could not send oppfolgingsplan to behandler"
                log.warn("$illegalArgumentMessage: {}, {}", e.message, callId)
                call.respond(HttpStatusCode.BadRequest, e.message ?: illegalArgumentMessage)
            }
        }
    }
}
