package no.nav.syfo.testhelper.mocks

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.pipeline.*
import no.nav.syfo.application.api.authentication.installContentNegotiation
import no.nav.syfo.behandler.fastlege.FastlegeClient.Companion.FASTLEGE_PATH
import no.nav.syfo.behandler.fastlege.FastlegeClient.Companion.FASTLEGE_SYSTEM_PATH
import no.nav.syfo.testhelper.UserConstants
import no.nav.syfo.testhelper.generator.generateFastlegeResponse
import no.nav.syfo.testhelper.getRandomPort
import no.nav.syfo.util.getPersonIdentHeader

private suspend fun PipelineContext<out Unit, ApplicationCall>.fastlegerestResponse() {
    when (getPersonIdentHeader()) {
        UserConstants.ARBEIDSTAKER_UTEN_FASTLEGE_FNR.value -> call.respond(
            HttpStatusCode.NotFound,
        )
        UserConstants.ARBEIDSTAKER_MED_FASTLEGE_UTEN_FORELDREENHET.value -> call.respond(
            HttpStatusCode.OK,
            generateFastlegeResponse(null)
        )
        UserConstants.ARBEIDSTAKER_MED_FASTLEGE_UTEN_PARTNERINFO.value -> call.respond(
            HttpStatusCode.OK,
            generateFastlegeResponse(UserConstants.HERID_UTEN_PARTNERINFO)
        )
        UserConstants.ARBEIDSTAKER_MED_FASTLEGE_UTEN_FNR_HPRID_HERID.value -> call.respond(
            HttpStatusCode.OK,
            generateFastlegeResponse(null, null, null)
        )

        else -> call.respond(
            HttpStatusCode.OK,
            generateFastlegeResponse(UserConstants.HERID)
        )
    }
}

class FastlegeRestMock {
    private val port = getRandomPort()
    val url = "http://localhost:$port"

    val name = "fastlegerest"
    val server = embeddedServer(
        factory = Netty,
        port = port,
    ) {
        installContentNegotiation()
        routing {
            get(FASTLEGE_PATH) {
                this.fastlegerestResponse()
            }
            get(FASTLEGE_SYSTEM_PATH) {
                this.fastlegerestResponse()
            }
        }
    }
}
