package no.nav.syfo.application.api

class ForbiddenAPIConsumer(
    consumerClientId: String,
    message: String = "Consumer with clientId=$consumerClientId is denied access to API",
) : RuntimeException(message)
