package no.nav.syfo.api

import io.ktor.server.application.*
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.syfo.application.api.APISystemConsumerAccessService
import no.nav.syfo.behandler.api.person.RSOppfolgingsplan
import no.nav.syfo.oppfolgingsplan.OppfolgingsplanService
import no.nav.syfo.util.getBearerHeader
import no.nav.syfo.util.getCallId
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val log: Logger = LoggerFactory.getLogger("no.nav.syfo")

const val sendOppfolgingsplanPath = "/api/v2/send/oppfolgingsplan"

fun Route.registerOppfolgingsplanApi(
    oppfolgingsplanService: OppfolgingsplanService,
    apiConsumerAccessService: APISystemConsumerAccessService,
    authorizedApplicationNameList: List<String>,
) {
    route(sendOppfolgingsplanPath) {
        post() {
            try {
                val callId = getCallId()
                val token = getBearerHeader()
                    ?: throw IllegalArgumentException("No Authorization header supplied")
                apiConsumerAccessService.validateSystemConsumerApplicationClientId(
                    authorizedApplicationNameList = authorizedApplicationNameList,
                    token = token,
                )
                val oppfolgingsplan = call.receive<RSOppfolgingsplan>()
                oppfolgingsplanService.sendOppfolgingsplan(
                    callId = callId,
                    token = token,
                    oppfolgingsplan = oppfolgingsplan,
                )
                call.respond(HttpStatusCode.OK, "Vellykket deling av oppfolgingsplan med lege!")
            } catch (e: IllegalArgumentException) {
                val errorMessage = "Feil ved sending av OP til lege!"
                log.error(errorMessage, e)
                call.respond(HttpStatusCode.BadRequest, e.message ?: errorMessage)
            }
        }
    }
}
