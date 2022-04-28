package no.nav.syfo.util

import com.auth0.jwt.JWT
import io.ktor.server.application.*
import io.ktor.http.*
import io.ktor.util.pipeline.*
import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.domain.PersonIdentNumber

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

fun PipelineContext<out Unit, ApplicationCall>.getPersonIdentHeader(): String? {
    return this.call.request.headers[NAV_PERSONIDENT_HEADER]
}

fun ApplicationCall.personIdentFromToken(): PersonIdentNumber? {
    val token = this.request.headers[HttpHeaders.Authorization]?.removePrefix("Bearer ")
    val decodedJWT = JWT.decode(token)
    return decodedJWT.claims["pid"]?.asString()?.let { PersonIdentNumber(it) }
}

fun bearerHeader(token: String) = "Bearer $token"
