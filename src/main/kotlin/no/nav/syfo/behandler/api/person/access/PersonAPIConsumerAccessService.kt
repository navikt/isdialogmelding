package no.nav.syfo.behandler.api.person.access

import com.auth0.jwt.JWT

const val JWT_CLAIM_CLIENT_ID = "client_id"

fun getConsumerClientId(token: String): String? {
    val decodedJWT = JWT.decode(token)
    return decodedJWT.claims[JWT_CLAIM_CLIENT_ID]?.asString()
}

class PersonAPIConsumerAccessService(
    private val authorizedConsumerApplicationClientIdList: List<String>,
) {
    fun validateConsumerApplicationClientId(
        token: String,
    ) {
        val consumerClientId = getConsumerClientId(token = token)
            ?: throw IllegalArgumentException("Claim $JWT_CLAIM_CLIENT_ID was not found in token")

        if (!authorizedConsumerApplicationClientIdList.contains(consumerClientId)) {
            throw ForbiddenPersonAPIConsumer(
                consumerClientId = consumerClientId,
            )
        }
    }
}
