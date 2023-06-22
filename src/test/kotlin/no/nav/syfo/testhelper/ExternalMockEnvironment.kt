package no.nav.syfo.testhelper

import no.nav.syfo.application.ApplicationState
import no.nav.syfo.testhelper.mocks.*

class ExternalMockEnvironment private constructor() {
    val applicationState: ApplicationState = testAppState()
    val database = TestDatabase()

    val environment = testEnvironment()
    val mockHttpClient = mockHttpClient(environment = environment)
    val wellKnownInternalAzureAD = wellKnownInternalAzureAD()
    val wellKnownInternalIdportenTokenX = wellKnownInternalIdportenTokenX()

    companion object {
        val instance: ExternalMockEnvironment = ExternalMockEnvironment()
    }
}
