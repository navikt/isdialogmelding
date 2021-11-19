package no.nav.syfo.testhelper.mocks

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.syfo.application.api.authentication.installContentNegotiation
import no.nav.syfo.behandler.fastlege.FastlegeClient.Companion.FASTLEGE_PATH
import no.nav.syfo.domain.PersonIdentNumber
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
                    when (val personIdent = PersonIdentNumber(getPersonIdentHeader()!!)) {
                        UserConstants.ARBEIDSTAKER_UTEN_FASTLEGE_FNR -> call.respond(
                            HttpStatusCode.NotFound,
                        )
                        UserConstants.ARBEIDSTAKER_FASTLEGE_UTEN_FORELDREENHET_FNR -> call.respond(
                            HttpStatusCode.OK,
                            generateFastlegeResponse(null, UserConstants.ARBEIDSTAKER_FASTLEGE_UTEN_FORELDREENHET_FNR)
                        )
                        UserConstants.ARBEIDSTAKER_FASTLEGE_UTEN_PARTNERINFO_FNR -> call.respond(
                            HttpStatusCode.OK,
                            generateFastlegeResponse(UserConstants.HERID_UTEN_PARTNERINFO, UserConstants.ARBEIDSTAKER_FASTLEGE_UTEN_PARTNERINFO_FNR)
                        )
                        UserConstants.ARBEIDSTAKER_ANNEN_FASTLEGE_HERID_FNR -> call.respond(
                            HttpStatusCode.OK,
                            generateFastlegeResponse(UserConstants.OTHER_HERID, UserConstants.ARBEIDSTAKER_ANNEN_FASTLEGE_HERID_FNR)
                        )
                        else -> call.respond(
                            HttpStatusCode.OK,
                            generateFastlegeResponse(
                                foreldreEnhetHerId = UserConstants.HERID,
                                pasientFnr = personIdent,
                                pasientFornavn = if (invocationCount % 2 == 0) UserConstants.ARBEIDSTAKER_FORNAVN else UserConstants.ARBEIDSTAKER_FORNAVN_UPDATED,
                                pasientEtternavn = if (invocationCount % 2 == 0) UserConstants.ARBEIDSTAKER_ETTERNAVN else UserConstants.ARBEIDSTAKER_ETTERNAVN_UPDATED,
                            )
                        )
                    }
                    invocationCount++
                }
            }
        }
    }
    companion object {
        var invocationCount = 0
    }
}
