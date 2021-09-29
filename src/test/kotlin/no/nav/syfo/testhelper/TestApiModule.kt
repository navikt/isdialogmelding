package no.nav.syfo.testhelper

import io.ktor.application.*
import io.mockk.mockk
import no.nav.syfo.application.api.apiModule

fun Application.testApiModule(
    externalMockEnvironment: ExternalMockEnvironment,
) {
    this.apiModule(
        applicationState = externalMockEnvironment.applicationState,
        environment = externalMockEnvironment.environment,
        mqSender = mockk(),
        wellKnownInternalAzureAD = externalMockEnvironment.wellKnownInternalAzureAD,
    )
}
