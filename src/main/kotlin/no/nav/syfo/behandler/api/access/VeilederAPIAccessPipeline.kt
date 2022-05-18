package no.nav.syfo.behandler.api.access

import io.ktor.server.application.*
import io.ktor.util.pipeline.*
import no.nav.syfo.client.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.domain.Personident
import no.nav.syfo.util.getBearerHeader
import no.nav.syfo.util.getCallId

suspend fun PipelineContext<out Unit, ApplicationCall>.validateVeilederAccess(
    action: String,
    personidentToAccess: Personident,
    veilederTilgangskontrollClient: VeilederTilgangskontrollClient,
    requestBlock: suspend () -> Unit,
) {
    val callId = getCallId()

    val token = getBearerHeader()
        ?: throw IllegalArgumentException("Failed to complete the following action: $action. No Authorization header supplied")

    val hasVeilederAccess = veilederTilgangskontrollClient.hasAccess(
        callId = callId,
        personident = personidentToAccess,
        token = token,
    )
    if (hasVeilederAccess) {
        requestBlock()
    } else {
        throw ForbiddenAccessVeilederException(
            action = action,
        )
    }
}

class ForbiddenAccessVeilederException(
    action: String,
    message: String = "Denied NAVIdent access to personident: $action",
) : RuntimeException(message)
