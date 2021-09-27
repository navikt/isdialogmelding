package no.nav.syfo.application.api

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.routing.*
import no.nav.syfo.api.registerDialogmeldingApi
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.Environment
import no.nav.syfo.application.api.authentication.*
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.fastlege.FastlegeClient
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

    val azureAdClient = AzureAdClient(
        azureAppClientId = environment.aadAppClient,
        azureAppClientSecret = environment.azureAppClientSecret,
        azureOpenidConfigTokenEndpoint = environment.azureOpenidConfigTokenEndpoint
    )

    // TODO: Ta i bruk fra nytt api for å hente behandlere som man kan motta dialogmelding
    val fastlegeClient = FastlegeClient(
        azureAdClient = azureAdClient,
        fastlegeRestClientId = environment.fastlegeRestClientId,
        fastlegeRestUrl = environment.fastlegeRestUrl
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
