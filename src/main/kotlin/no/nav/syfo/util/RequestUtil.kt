package no.nav.syfo.util

import io.ktor.server.application.*
import io.ktor.http.*
import io.ktor.util.pipeline.*
import net.logstash.logback.argument.StructuredArguments

const val NAV_CALL_ID_HEADER = "Nav-Call-Id"
const val NAV_PERSONIDENT_HEADER = "nav-personident"

fun PipelineContext<out Unit, ApplicationCall>.getCallId(): String {
    return this.call.getCallId()
}
fun ApplicationCall.getCallId(): String {
    return this.request.headers[NAV_CALL_ID_HEADER].toString()
}

fun callIdArgument(callId: String) = StructuredArguments.keyValue("callId", callId)!!

fun PipelineContext<out Unit, ApplicationCall>.getBearerHeader(): String? {
    return this.call.request.headers[HttpHeaders.Authorization]?.removePrefix("Bearer ")
}

fun PipelineContext<out Unit, ApplicationCall>.getPersonidentHeader(): String? {
    return this.call.request.headers[NAV_PERSONIDENT_HEADER]
}

suspend fun PipelineContext<out Unit, ApplicationCall>.withValidToken(
    block: suspend PipelineContext<out Unit, ApplicationCall>.() -> Unit
) {
    if (getBearerHeader() != null) {
        block()
    } else {
        throw IllegalArgumentException("No Authorization header supplied")
    }
}

fun bearerHeader(token: String) = "Bearer $token"
