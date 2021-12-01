package no.nav.syfo.testhelper

import io.ktor.application.*
import io.mockk.mockk
import no.nav.syfo.application.api.apiModule
import no.nav.syfo.client.azuread.AzureAdClient

fun Application.testApiModule(
    externalMockEnvironment: ExternalMockEnvironment,
) {
    this.apiModule(
        applicationState = externalMockEnvironment.applicationState,
        database = externalMockEnvironment.database,
        environment = externalMockEnvironment.environment,
        mqSender = mockk(),
        wellKnownInternalAzureAD = externalMockEnvironment.wellKnownInternalAzureAD,
        azureAdClient = AzureAdClient(
            azureAppClientId = externalMockEnvironment.environment.aadAppClient,
            azureAppClientSecret = externalMockEnvironment.environment.azureAppClientSecret,
            azureOpenidConfigTokenEndpoint = externalMockEnvironment.environment.azureOpenidConfigTokenEndpoint,
        ),
    )
}
