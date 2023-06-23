package no.nav.syfo.testhelper.mocks

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import no.nav.syfo.application.Environment
import no.nav.syfo.client.commonConfig

fun mockHttpClient(environment: Environment) = HttpClient(MockEngine) {
    commonConfig()
    engine {
        addHandler { request ->
            val requestUrl = request.url.encodedPath
            when {
                requestUrl == "/${environment.azureOpenidConfigTokenEndpoint}" -> azureAdMockResponse()
                requestUrl.startsWith("/${environment.syfotilgangskontrollUrl}") -> syfoTilgangskontrollResponse(
                    request
                )

                requestUrl.startsWith("/${environment.fastlegeRestUrl}") -> fastlegeRestMockResponse(request)
                requestUrl.startsWith("/${environment.pdlUrl}") -> pdlMockResponse(request)
                requestUrl.startsWith("/${environment.syfoPartnerinfoUrl}") -> syfoPartnerInfoMockResponse(
                    request
                )

                else -> error("Unhandled ${request.url.encodedPath}")
            }
        }
    }
}
