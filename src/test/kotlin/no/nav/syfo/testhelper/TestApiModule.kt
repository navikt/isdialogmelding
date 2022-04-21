package no.nav.syfo.testhelper

import io.ktor.application.*
import io.mockk.mockk
import no.nav.syfo.application.api.apiModule
import no.nav.syfo.behandler.BehandlerService
import no.nav.syfo.behandler.fastlege.FastlegeClient
import no.nav.syfo.behandler.partnerinfo.PartnerinfoClient
import no.nav.syfo.client.azuread.AzureAdClient

fun Application.testApiModule(
    externalMockEnvironment: ExternalMockEnvironment,
) {
    val azureAdClient = AzureAdClient(
        azureAppClientId = externalMockEnvironment.environment.aadAppClient,
        azureAppClientSecret = externalMockEnvironment.environment.azureAppClientSecret,
        azureOpenidConfigTokenEndpoint = externalMockEnvironment.environment.azureOpenidConfigTokenEndpoint,
    )
    this.apiModule(
        applicationState = externalMockEnvironment.applicationState,
        database = externalMockEnvironment.database,
        environment = externalMockEnvironment.environment,
        mqSender = mockk(),
        wellKnownInternalAzureAD = externalMockEnvironment.wellKnownInternalAzureAD,
        wellKnownInternalIdportenTokenX = externalMockEnvironment.wellKnownInternalIdportenTokenX,
        azureAdClient = azureAdClient,
        behandlerService = BehandlerService(
            fastlegeClient = FastlegeClient(
                azureAdClient = azureAdClient,
                fastlegeRestClientId = externalMockEnvironment.environment.fastlegeRestClientId,
                fastlegeRestUrl = externalMockEnvironment.environment.fastlegeRestUrl,
            ),
            partnerinfoClient = PartnerinfoClient(
                azureAdClient = azureAdClient,
                syfoPartnerinfoClientId = externalMockEnvironment.environment.syfoPartnerinfoClientId,
                syfoPartnerinfoUrl = externalMockEnvironment.environment.syfoPartnerinfoUrl,
            ),
            database = externalMockEnvironment.database,
        )
    )
}
