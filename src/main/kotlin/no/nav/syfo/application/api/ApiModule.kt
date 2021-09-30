package no.nav.syfo.application.api

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.routing.*
import no.nav.syfo.api.registerDialogmeldingApi
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.Environment
import no.nav.syfo.application.api.authentication.*
import no.nav.syfo.application.mq.MQSender
import no.nav.syfo.behandler.BehandlerService
import no.nav.syfo.behandler.api.registerBehandlerApi
import no.nav.syfo.behandler.fastlege.FastlegeClient
import no.nav.syfo.behandler.partnerinfo.PartnerinfoClient
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.oppfolgingsplan.OppfolgingsplanService

fun Application.apiModule(
    applicationState: ApplicationState,
    environment: Environment,
    mqSender: MQSender,
    wellKnownInternalAzureAD: WellKnown,
) {
    installMetrics()
    installContentNegotiation()
    installStatusPages()
    installJwtAuthentication(
        jwtIssuerList = listOf(
            JwtIssuer(
                acceptedAudienceList = listOf(environment.aadAppClient),
                jwtIssuerType = JwtIssuerType.AZUREAD_V2,
                wellKnown = wellKnownInternalAzureAD,
            ),
        ),
    )

    val azureAdClient = AzureAdClient(
        azureAppClientId = environment.aadAppClient,
        azureAppClientSecret = environment.azureAppClientSecret,
        azureOpenidConfigTokenEndpoint = environment.azureOpenidConfigTokenEndpoint,
    )
    val veilederTilgangskontrollClient = VeilederTilgangskontrollClient(
        azureAdClient = azureAdClient,
        syfotilgangskontrollClientId = environment.syfotilgangskontrollClientId,
        tilgangskontrollBaseUrl = environment.syfotilgangskontrollUrl,
    )

    val fastlegeClient = FastlegeClient(
        azureAdClient = azureAdClient,
        fastlegeRestClientId = environment.fastlegeRestClientId,
        fastlegeRestUrl = environment.fastlegeRestUrl,
    )
    val partnerinfoClient = PartnerinfoClient(
        azureAdClient = azureAdClient,
        syfoPartnerinfoClientId = environment.syfoPartnerinfoClientId,
        syfoPartnerinfoUrl = environment.syfoPartnerinfoUrl,
    )
    val behandlerService = BehandlerService(
        fastlegeClient = fastlegeClient,
        partnerinfoClient = partnerinfoClient
    )
    val oppfolgingsplanService = OppfolgingsplanService(
        mqSender = mqSender
    )

    routing {
        registerPodApi(applicationState)
        registerPrometheusApi()

        authenticate(JwtIssuerType.AZUREAD_V2.name) {
            registerDialogmeldingApi(
                oppfolgingsplanService = oppfolgingsplanService,
            )
            registerBehandlerApi(
                behandlerService = behandlerService,
                veilederTilgangskontrollClient = veilederTilgangskontrollClient,
            )
        }
    }
}
