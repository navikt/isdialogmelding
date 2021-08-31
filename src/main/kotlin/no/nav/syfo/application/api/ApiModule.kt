package no.nav.syfo.application.api

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.routing.*
import no.nav.syfo.api.registerDialogmeldingApi
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.Environment
import no.nav.syfo.application.api.authentication.*
import no.nav.syfo.oppfolgingsplan.OppfolgingsplanService

fun Application.apiModule(
    applicationState: ApplicationState,
    environment: Environment,
    oppfolgingsplanService: OppfolgingsplanService,
) {
    installMetrics()
    installContentNegotiation()
    installStatusPages()
    installJwtAuthentication(
        jwtIssuerList = listOf(
            JwtIssuer(
                acceptedAudienceList = listOf(environment.aadAppClient),
                jwtIssuerType = JwtIssuerType.AZUREAD_V2,
                wellKnown = getWellKnown(environment.azureAppWellKnownUrl),
            ),
        ),
    )

    routing {
        registerPodApi(applicationState)
        registerPrometheusApi()

        authenticate(JwtIssuerType.AZUREAD_V2.name) {
            registerDialogmeldingApi(
                oppfolgingsplanService = oppfolgingsplanService,
            )
        }
    }
}
