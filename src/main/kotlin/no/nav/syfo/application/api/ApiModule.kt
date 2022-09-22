package no.nav.syfo.application.api

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import no.nav.syfo.api.registerDialogmeldingApi
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.Environment
import no.nav.syfo.application.api.authentication.*
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.application.mq.MQSender
import no.nav.syfo.behandler.BehandlerService
import no.nav.syfo.behandler.DialogmeldingToBehandlerService
import no.nav.syfo.behandler.api.person.access.PersonAPIConsumerAccessService
import no.nav.syfo.behandler.api.person.registerPersonBehandlerApi
import no.nav.syfo.behandler.api.person.registerPersonOppfolgingsplanApi
import no.nav.syfo.behandler.api.registerBehandlerApi
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.veiledertilgang.VeilederTilgangskontrollClient
import no.nav.syfo.oppfolgingsplan.OppfolgingsplanService

fun Application.apiModule(
    applicationState: ApplicationState,
    database: DatabaseInterface,
    environment: Environment,
    mqSender: MQSender,
    wellKnownInternalAzureAD: WellKnown,
    wellKnownInternalIdportenTokenX: WellKnown,
    azureAdClient: AzureAdClient,
    behandlerService: BehandlerService,
    dialogmeldingToBehandlerService: DialogmeldingToBehandlerService,
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

    val veilederTilgangskontrollClient = VeilederTilgangskontrollClient(
        azureAdClient = azureAdClient,
        syfotilgangskontrollClientId = environment.syfotilgangskontrollClientId,
        tilgangskontrollBaseUrl = environment.syfotilgangskontrollUrl,
    )

    val oppfolgingsplanService = OppfolgingsplanService(
        mqSender = mqSender
    )

    val personAPIConsumerAccessService = PersonAPIConsumerAccessService(
        authorizedConsumerApplicationClientIdList = environment.personAPIAuthorizedConsumerClientIdList,
    )
    val oppfolgingsplanAPIConsumerAccessService = PersonAPIConsumerAccessService(
        authorizedConsumerApplicationClientIdList = environment.oppfolgingsplanAPIAuthorizedConsumerClientIdList,
    )

    routing {
        registerPodApi(
            applicationState = applicationState,
            database = database,
        )
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
        authenticate(JwtIssuerType.IDPORTEN_TOKENX.name) {
            registerPersonBehandlerApi(
                behandlerService = behandlerService,
                personAPIConsumerAccessService = personAPIConsumerAccessService,
            )
            registerPersonOppfolgingsplanApi(
                behandlerService = behandlerService,
                dialogmeldingToBehandlerService = dialogmeldingToBehandlerService,
                oppfolgingsplanService = oppfolgingsplanService,
                oppfolgingsplanAPIConsumerAccessService = oppfolgingsplanAPIConsumerAccessService,
            )
        }
    }
}
