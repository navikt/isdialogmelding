package no.nav.syfo.testhelper.mocks

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.syfo.application.api.authentication.installContentNegotiation
import no.nav.syfo.client.pdl.domain.*
import no.nav.syfo.testhelper.UserConstants
import no.nav.syfo.testhelper.generator.generatePdlPersonResponse
import no.nav.syfo.testhelper.getRandomPort
import no.nav.syfo.util.configuredJacksonMapper

class PdlMock {
    private val port = getRandomPort()
    private val objectMapper: ObjectMapper = configuredJacksonMapper()
    val url = "http://localhost:$port"
    val name = "pdl"
    val server = embeddedServer(
        factory = Netty,
        port = port
    ) {
        installContentNegotiation()
        routing {
            post {
                val pdlRequest = call.receiveText()
                val isHentIdenter = pdlRequest.contains("hentIdenter")
                if (isHentIdenter) {
                    val request: PdlHentIdenterRequest = objectMapper.readValue(pdlRequest)
                    if (request.variables.ident == UserConstants.TREDJE_ARBEIDSTAKER_FNR.value) {
                        call.respond(generatePdlIdenter("enAnnenIdent"))
                    } else {
                        call.respond(generatePdlIdenter(request.variables.ident))
                    }
                } else {
                    call.respond(generatePdlPersonResponse())
                }
            }
        }
    }
}

fun generatePdlIdenter(
    personident: String,
) = PdlIdenterResponse(
    data = PdlHentIdenter(
        hentIdenter = PdlIdenter(
            identer = listOf(
                PdlIdent(
                    ident = personident,
                    historisk = false,
                    gruppe = IdentType.FOLKEREGISTERIDENT,
                ),
                PdlIdent(
                    ident = personident.toFakeOldIdent(),
                    historisk = true,
                    gruppe = IdentType.FOLKEREGISTERIDENT,
                ),
            ),
        ),
    ),
    errors = null,
)

private fun String.toFakeOldIdent(): String {
    val substring = this.drop(1)
    return "9$substring"
}
