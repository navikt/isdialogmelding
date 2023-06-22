package no.nav.syfo.testhelper.mocks

import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import no.nav.syfo.application.api.authentication.WellKnown
import no.nav.syfo.client.azuread.AzureAdTokenResponse
import java.nio.file.Paths

fun wellKnownInternalAzureAD(): WellKnown {
    val path = "src/test/resources/jwkset.json"
    val uri = Paths.get(path).toUri().toURL()
    return WellKnown(
        issuer = "https://sts.issuer.net/veileder/v2",
        jwks_uri = uri.toString(),
        authorization_endpoint = "authorizationendpoint",
        token_endpoint = "tokenendpoint",
    )
}

fun MockRequestHandleScope.azureAdMockResponse(): HttpResponseData = respondOk(
    AzureAdTokenResponse(
        access_token = "token",
        expires_in = 3600,
        token_type = "type",
    )
)
