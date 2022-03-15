package no.nav.syfo.testhelper.mocks

import no.nav.syfo.application.api.authentication.WellKnown
import java.nio.file.Paths

fun wellKnownInternalIdportenTokenX(): WellKnown {
    val path = "src/test/resources/jwkset.json"
    val uri = Paths.get(path).toUri().toURL()
    return WellKnown(
        issuer = "https://idporten.issuer.net/tokenx/v1",
        jwks_uri = uri.toString(),
        authorization_endpoint = "authorizationendpoint-idporten",
        token_endpoint = "tokenendpoint-idporten",
    )
}
