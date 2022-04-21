package no.nav.syfo.testhelper.mocks

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.syfo.application.api.authentication.installContentNegotiation
import no.nav.syfo.testhelper.generator.generatePdlPersonResponse
import no.nav.syfo.testhelper.getRandomPort

class PdlMock {
    private val port = getRandomPort()
    val url = "http://localhost:$port"
    val name = "pdl"
    val server = embeddedServer(
        factory = Netty,
        port = port
    ) {
        installContentNegotiation()
        routing {
            post {
                call.respond(generatePdlPersonResponse())
            }
        }
    }
}
