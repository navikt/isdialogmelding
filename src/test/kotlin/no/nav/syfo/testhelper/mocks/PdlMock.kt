package no.nav.syfo.testhelper.mocks

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.syfo.testhelper.generator.generatePdlPersonResponse

class PdlMock : MockServer() {
    override val name = "pdl"
    override val routingConfiguration: Routing.() -> Unit = {
        post {
            call.respond(generatePdlPersonResponse())
        }
    }
}
