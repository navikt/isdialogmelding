package no.nav.syfo.testhelper.mocks

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.syfo.application.api.authentication.installContentNegotiation
import no.nav.syfo.behandler.partnerinfo.PartnerinfoClient.Companion.BEHANDLER_PATH
import no.nav.syfo.behandler.partnerinfo.PartnerinfoResponse
import no.nav.syfo.testhelper.UserConstants
import no.nav.syfo.testhelper.generator.generatePartnerinfoResponse
import no.nav.syfo.testhelper.getRandomPort

class SyfopartnerInfoMock {
    private val port = getRandomPort()
    val url = "http://localhost:$port"

    val name = "syfopartnerinfo"
    val server = mockSyfopartnerInfoServer(port)

    private fun mockSyfopartnerInfoServer(port: Int): NettyApplicationEngine {
        return embeddedServer(
            factory = Netty,
            port = port,
        ) {
            installContentNegotiation()
            routing {
                get(BEHANDLER_PATH) {
                    when (call.parameters["herid"]) {
                        UserConstants.HERID_UTEN_PARTNERINFO.toString() -> call.respond(
                            HttpStatusCode.OK,
                            emptyList<PartnerinfoResponse>()
                        )
                        UserConstants.OTHER_HERID.toString() -> call.respond(
                            HttpStatusCode.OK, listOf(generatePartnerinfoResponse(UserConstants.OTHER_PARTNERID))
                        )
                        else -> call.respond(
                            HttpStatusCode.OK, listOf(generatePartnerinfoResponse(UserConstants.PARTNERID))
                        )
                    }
                }
            }
        }
    }
}
