package no.nav.syfo.behandler.api

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import no.nav.syfo.application.api.APISystemConsumerAccessService
import no.nav.syfo.behandler.BehandlerService
import no.nav.syfo.behandler.domain.*
import no.nav.syfo.util.*
import java.util.*

const val behandlerSystemApiPath = "/api/system/v1/behandler"

fun Route.registerBehandlerSystemApi(
    behandlerService: BehandlerService,
    apiConsumerAccessService: APISystemConsumerAccessService,
    authorizedApplicationNameList: List<String>,
) {
    route(behandlerSystemApiPath) {
        get("/{$behandlerRefParam}") {
            val token = getBearerHeader()
                ?: throw IllegalArgumentException("No Authorization header supplied")
            apiConsumerAccessService.validateSystemConsumerApplicationClientId(
                authorizedApplicationNameList = authorizedApplicationNameList,
                token = token,
            )

            val behandlerRef = UUID.fromString(this.call.parameters[behandlerRefParam])
            val behandler = behandlerService.getBehandler(
                behandlerRef = behandlerRef
            )

            if (behandler != null) {
                call.respond(behandler.toBehandlerDTO(behandlerType = null))
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
    }
}
