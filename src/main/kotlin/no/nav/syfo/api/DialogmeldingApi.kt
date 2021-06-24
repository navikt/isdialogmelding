package no.nav.syfo.api

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import no.nav.syfo.domain.RSHodemelding
import no.nav.syfo.oppfolgingsplan.OppfolgingsplanService
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val log: Logger = LoggerFactory.getLogger("no.nav.syfo")

const val sendDialogmeldingPath = "/api/v1/send"
const val oppfolgingsplanPath = "/oppfolgingsplan"

fun Route.registerDialogmeldingApi(
    oppfolgingsplanService: OppfolgingsplanService,
) {
    route(sendDialogmeldingPath) {
        get {
            try {
                call.respond(HttpStatusCode.OK, "Vellykket!")
            } catch (e: IllegalArgumentException) {
                val illegalArgumentMessage = "Could not get from dialogmeldingApi"
                log.warn("$illegalArgumentMessage: {}", e.message)
                call.respond(HttpStatusCode.BadRequest, e.message ?: illegalArgumentMessage)
            }
        }
        post(oppfolgingsplanPath) {
            try {
                val dialogmelding = call.receive<RSHodemelding>()

                oppfolgingsplanService.sendMelding(dialogmelding)
            } catch (e: Exception) {
                log.error("FEIL! :(")
            }
        }
    }
}
