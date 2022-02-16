package no.nav.syfo.behandler.api.access

import io.ktor.application.*
import io.ktor.util.pipeline.*
import no.nav.syfo.client.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.domain.PersonIdentNumber
import no.nav.syfo.util.getBearerHeader
import no.nav.syfo.util.getCallId

suspend fun PipelineContext<out Unit, ApplicationCall>.validateVeilederAccess(
    action: String,
    personIdentToAccess: PersonIdentNumber,
    veilederTilgangskontrollClient: VeilederTilgangskontrollClient,
    requestBlock: suspend () -> Unit,
) {
    val callId = getCallId()

    val token = getBearerHeader()
        ?: throw IllegalArgumentException("Failed to complete the following action: $action. No Authorization header supplied")

    val hasVeilederAccess = veilederTilgangskontrollClient.hasAccess(
        callId = callId,
        personIdentNumber = personIdentToAccess,
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
    message: String = "Denied NAVIdent access to personIdent: $action",
) : RuntimeException(message)
