package no.nav.syfo.application.api

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import no.nav.syfo.api.registerOppfolgingsplanApi
import no.nav.syfo.application.*
import no.nav.syfo.application.api.authentication.*
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.behandler.BehandlerService
import no.nav.syfo.dialogmelding.bestilling.DialogmeldingToBehandlerService
import no.nav.syfo.behandler.api.person.registerPersonBehandlerApi
import no.nav.syfo.behandler.api.person.registerPersonOppfolgingsplanApi
import no.nav.syfo.behandler.api.registerBehandlerApi
import no.nav.syfo.behandler.api.registerBehandlerSystemApi
import no.nav.syfo.client.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.oppfolgingsplan.OppfolgingsplanService

fun Application.apiModule(
    applicationState: ApplicationState,
    database: DatabaseInterface,
    environment: Environment,
    wellKnownInternalAzureAD: WellKnown,
    wellKnownInternalIdportenTokenX: WellKnown,
    behandlerService: BehandlerService,
    dialogmeldingToBehandlerService: DialogmeldingToBehandlerService,
    veilederTilgangskontrollClient: VeilederTilgangskontrollClient,
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
            JwtIssuer(
                acceptedAudienceList = listOf(environment.idportenTokenXClientId),
                jwtIssuerType = JwtIssuerType.IDPORTEN_TOKENX,
                wellKnown = wellKnownInternalIdportenTokenX,
            ),
        ),
    )
    val oppfolgingsplanService = OppfolgingsplanService(
        behandlerService = behandlerService,
        dialogmeldingToBehandlerService = dialogmeldingToBehandlerService,
    )

    val personAPIConsumerAccessService = APIConsumerAccessService(
        authorizedConsumerApplicationClientIdList = environment.personAPIAuthorizedConsumerClientIdList,
    )
    val oppfolgingsplanAPIConsumerAccessService = APIConsumerAccessService(
        authorizedConsumerApplicationClientIdList = environment.oppfolgingsplanAPIAuthorizedConsumerClientIdList,
    )
    val systemAPIConsumerAccessService = APISystemConsumerAccessService(
        azureAppPreAuthorizedApps = environment.azureAppPreAuthorizedApps,
    )

    routing {
        registerPodApi(
            applicationState = applicationState,
            database = database,
        )
        registerPrometheusApi()

        authenticate(JwtIssuerType.AZUREAD_V2.name) {
            registerOppfolgingsplanApi(
                oppfolgingsplanService = oppfolgingsplanService,
                apiConsumerAccessService = systemAPIConsumerAccessService,
                authorizedApplicationNameList = environment.oppfolgingsplanSystemAPIAuthorizedConsumerApplicationNameList,
            )
            registerBehandlerApi(
                behandlerService = behandlerService,
                veilederTilgangskontrollClient = veilederTilgangskontrollClient,
            )
            registerBehandlerSystemApi(
                behandlerService = behandlerService,
                apiConsumerAccessService = systemAPIConsumerAccessService,
                authorizedApplicationNameList = environment.behandlerSystemAPIAuthorizedConsumerApplicationNameList
            )
        }
        authenticate(JwtIssuerType.IDPORTEN_TOKENX.name) {
            registerPersonBehandlerApi(
                behandlerService = behandlerService,
                apiConsumerAccessService = personAPIConsumerAccessService,
            )
            registerPersonOppfolgingsplanApi(
                oppfolgingsplanService = oppfolgingsplanService,
                apiConsumerAccessService = oppfolgingsplanAPIConsumerAccessService,
            )
        }
    }
}
