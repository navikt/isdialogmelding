package no.nav.syfo.testhelper

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.jackson.*
import io.ktor.server.testing.*
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.util.configure

fun ApplicationTestBuilder.setupApiAndClient(database: DatabaseInterface? = null): HttpClient {
    application {
        testApiModule(
            externalMockEnvironment = ExternalMockEnvironment.instance,
            database = database ?: ExternalMockEnvironment.instance.database,
        )
    }
    val client = createClient {
        install(ContentNegotiation) {
            jackson { configure() }
        }
    }
    return client
}
