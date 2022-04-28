package no.nav.syfo.testhelper.mocks

import io.ktor.server.application.*
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.syfo.behandler.partnerinfo.PartnerinfoClient.Companion.BEHANDLER_PATH
import no.nav.syfo.behandler.partnerinfo.PartnerinfoResponse
import no.nav.syfo.testhelper.UserConstants
import no.nav.syfo.testhelper.generator.generatePartnerinfoResponse

class SyfopartnerInfoMock : MockServer() {
    override val name = "syfopartnerinfo"
    override val routingConfiguration: Routing.() -> Unit = {
        get(BEHANDLER_PATH) {
            when (call.parameters["herid"]) {
                UserConstants.HERID_UTEN_PARTNERINFO.toString() -> call.respond(
                    HttpStatusCode.OK,
                    emptyList<PartnerinfoResponse>()
                )
                UserConstants.HERID_MED_FLERE_PARTNERINFO.toString() -> call.respond(
                    HttpStatusCode.OK,
                    listOf(
                        generatePartnerinfoResponse(UserConstants.PARTNERID.value),
                        generatePartnerinfoResponse(UserConstants.OTHER_PARTNERID.value)
                    )
                )
                UserConstants.OTHER_HERID.toString() -> call.respond(
                    HttpStatusCode.OK, listOf(generatePartnerinfoResponse(UserConstants.OTHER_PARTNERID.value))
                )
                else -> call.respond(
                    HttpStatusCode.OK, listOf(generatePartnerinfoResponse(UserConstants.PARTNERID.value))
                )
            }
        }
    }
}
