package no.nav.syfo.testhelper.mocks

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.syfo.application.api.authentication.installContentNegotiation
import no.nav.syfo.behandler.fastlege.FastlegeClient.Companion.FASTLEGE_PATH
import no.nav.syfo.testhelper.UserConstants
import no.nav.syfo.testhelper.generator.generateFastlegeResponse
import no.nav.syfo.testhelper.getRandomPort
import no.nav.syfo.util.getPersonIdentHeader

class FastlegeRestMock {
    private val port = getRandomPort()
    val url = "http://localhost:$port"

    val name = "fastlegerest"
    val server = mockFastlegeRestServer(port)

    private fun mockFastlegeRestServer(port: Int): NettyApplicationEngine {
        return embeddedServer(
            factory = Netty,
            port = port,
        ) {
            installContentNegotiation()
            routing {
                get(FASTLEGE_PATH) {
                    when (getPersonIdentHeader()) {
                        UserConstants.ARBEIDSTAKER_UTEN_FASTLEGE.value -> call.respond(
                            HttpStatusCode.NotFound,
                        )
                        UserConstants.ARBEIDSTAKER_MED_FASTLEGE_UTEN_FORELDREENHET.value -> call.respond(
                            HttpStatusCode.OK,
                            generateFastlegeResponse(UserConstants.FASTLEGE_FNR)
                        )
                        UserConstants.ARBEIDSTAKER_MED_FASTLEGE_UTEN_PARTNERINFO.value -> call.respond(
                            HttpStatusCode.OK,
                            generateFastlegeResponse(UserConstants.FASTLEGE_FNR, UserConstants.HERID_UTEN_PARTNERINFO)
                        )
                        UserConstants.ARBEIDSTAKER_MED_FASTLEGE_MED_ANNEN_HERID.value -> call.respond(
                            HttpStatusCode.OK,
                            generateFastlegeResponse(UserConstants.FASTLEGE_FNR, UserConstants.OTHER_HERID)
                        )
                        UserConstants.ARBEIDSTAKER_MED_ANNEN_FASTLEGE_SAMME_PARTNERINFO.value -> call.respond(
                            HttpStatusCode.OK,
                            generateFastlegeResponse(UserConstants.FASTLEGE_ANNEN_FNR, UserConstants.HERID)
                        )
                        UserConstants.ARBEIDSTAKER_MED_FASTLEGE_UTEN_FNR_HPRID_HERID.value -> call.respond(
                            HttpStatusCode.OK,
                            generateFastlegeResponse(null, null, null)
                        )
                        UserConstants.ARBEIDSTAKER_MED_FASTLEGE_UTEN_FNR.value -> call.respond(
                            HttpStatusCode.OK,
                            generateFastlegeResponse(null, 1337, 1234)
                        )
                        UserConstants.ARBEIDSTAKER_MED_FASTLEGE_UTEN_HERID.value -> call.respond(
                            HttpStatusCode.OK,
                            generateFastlegeResponse(UserConstants.FASTLEGE_FNR.value, null, 1234)
                        )
                        UserConstants.ARBEIDSTAKER_MED_FASTLEGE_UTEN_HPRID.value -> call.respond(
                            HttpStatusCode.OK,
                            generateFastlegeResponse(UserConstants.FASTLEGE_FNR.value, 1337, null)
                        )

                        else -> call.respond(
                            HttpStatusCode.OK,
                            generateFastlegeResponse(UserConstants.FASTLEGE_FNR, UserConstants.HERID)
                        )
                    }
                }
            }
        }
    }
}
