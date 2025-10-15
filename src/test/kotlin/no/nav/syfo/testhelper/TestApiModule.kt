package no.nav.syfo.testhelper

import io.ktor.server.application.*
import no.nav.syfo.application.api.apiModule
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.behandler.BehandlerService
import no.nav.syfo.dialogmelding.bestilling.DialogmeldingToBehandlerService
import no.nav.syfo.behandler.fastlege.FastlegeClient
import no.nav.syfo.behandler.partnerinfo.PartnerinfoClient
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.pdl.PdlClient
import no.nav.syfo.client.veiledertilgang.VeilederTilgangskontrollClient

fun Application.testApiModule(
    externalMockEnvironment: ExternalMockEnvironment,
    database: DatabaseInterface = externalMockEnvironment.database,
) {
    val mockHttpClient = externalMockEnvironment.mockHttpClient
    val azureAdClient = AzureAdClient(
        azureAppClientId = externalMockEnvironment.environment.aadAppClient,
        azureAppClientSecret = externalMockEnvironment.environment.azureAppClientSecret,
        azureOpenidConfigTokenEndpoint = externalMockEnvironment.environment.azureOpenidConfigTokenEndpoint,
        httpClient = mockHttpClient,
    )
    val veilederTilgangskontrollClient = VeilederTilgangskontrollClient(
        azureAdClient = azureAdClient,
        istilgangskontrollClientId = externalMockEnvironment.environment.istilgangskontrollClientId,
        tilgangskontrollBaseUrl = externalMockEnvironment.environment.istilgangskontrollUrl,
        httpClient = mockHttpClient,
    )
    this.apiModule(
        applicationState = externalMockEnvironment.applicationState,
        database = database,
        environment = externalMockEnvironment.environment,
        wellKnownInternalAzureAD = externalMockEnvironment.wellKnownInternalAzureAD,
        wellKnownInternalIdportenTokenX = externalMockEnvironment.wellKnownInternalIdportenTokenX,
        veilederTilgangskontrollClient = veilederTilgangskontrollClient,
        behandlerService = BehandlerService(
            fastlegeClient = FastlegeClient(
                azureAdClient = azureAdClient,
                fastlegeRestClientId = externalMockEnvironment.environment.fastlegeRestClientId,
                fastlegeRestUrl = externalMockEnvironment.environment.fastlegeRestUrl,
                httpClient = mockHttpClient,
            ),
            partnerinfoClient = PartnerinfoClient(
                azureAdClient = azureAdClient,
                syfoPartnerinfoClientId = externalMockEnvironment.environment.syfoPartnerinfoClientId,
                syfoPartnerinfoUrl = externalMockEnvironment.environment.syfoPartnerinfoUrl,
                httpClient = mockHttpClient,
            ),
            database = database,
        ),
        dialogmeldingToBehandlerService = DialogmeldingToBehandlerService(
            database = database,
            pdlClient = PdlClient(
                azureAdClient = azureAdClient,
                pdlClientId = externalMockEnvironment.environment.pdlClientId,
                pdlUrl = externalMockEnvironment.environment.pdlUrl,
                httpClient = mockHttpClient,
            ),
        ),
    )
}
