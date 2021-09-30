package no.nav.syfo.util

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.util.pipeline.*

const val NAV_CALL_ID_HEADER = "Nav-Call-Id"
const val NAV_PERSONIDENT_HEADER = "nav-personident"

fun PipelineContext<out Unit, ApplicationCall>.getCallId(): String {
    return this.call.request.headers[NAV_CALL_ID_HEADER].toString()
}
fun PipelineContext<out Unit, ApplicationCall>.getBearerHeader(): String? {
    return this.call.request.headers[HttpHeaders.Authorization]?.removePrefix("Bearer ")
}

fun PipelineContext<out Unit, ApplicationCall>.getPersonIdentHeader(): String? {
    return this.call.request.headers[NAV_PERSONIDENT_HEADER]
}

fun bearerHeader(token: String) = "Bearer $token"
