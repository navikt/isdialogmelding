package no.nav.syfo.application.api

import com.auth0.jwt.JWT
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.syfo.behandler.api.person.access.PreAuthorizedClient
import no.nav.syfo.behandler.api.person.access.toNamespaceAndApplicationName
import no.nav.syfo.util.configuredJacksonMapper

const val JWT_CLAIM_AZP = "azp"

fun getSystemConsumerClientId(token: String): String? {
    val decodedJWT = JWT.decode(token)
    return decodedJWT.claims[JWT_CLAIM_AZP]?.asString()
}

class APISystemConsumerAccessService(
    azureAppPreAuthorizedApps: String,
) {
    private val preAuthorizedClientList: List<PreAuthorizedClient> = configuredJacksonMapper()
        .readValue(azureAppPreAuthorizedApps)

    fun validateSystemConsumerApplicationClientId(
        authorizedApplicationNameList: List<String>,
        token: String,
    ) {
        val consumerClientIdAzp = getSystemConsumerClientId(token = token)
            ?: throw IllegalArgumentException("Claim $JWT_CLAIM_AZP was not found in token")

        val clientIdList = preAuthorizedClientList
            .filter {
                authorizedApplicationNameList.contains(
                    it.toNamespaceAndApplicationName().applicationName
                )
            }
            .map { it.clientId }

        if (!clientIdList.contains(consumerClientIdAzp)) {
            throw ForbiddenAPIConsumer(consumerClientId = consumerClientIdAzp)
        }
    }
}
