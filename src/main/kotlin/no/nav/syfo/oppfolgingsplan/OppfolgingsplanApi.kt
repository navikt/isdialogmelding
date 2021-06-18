package no.nav.syfo.oppfolgingsplan

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val log: Logger = LoggerFactory.getLogger("no.nav.syfo")

const val oppfolgingsplanApiPath = "/api/v1/oppfolgingsplan"

fun Route.registerOppfolgingsplanApi(
//    oppfolgingsplanService: OppfolgingsplanService,
) {
    route(oppfolgingsplanApiPath) {
        get {
            try {
                call.respond(HttpStatusCode.OK, "Vellykket!")
            } catch (e: IllegalArgumentException) {
                val illegalArgumentMessage = "Could not get from oppfolgingsplanApi"
                log.warn("$illegalArgumentMessage: {}", e.message)
                call.respond(HttpStatusCode.BadRequest, e.message ?: illegalArgumentMessage)
            }
        }
    }
}
