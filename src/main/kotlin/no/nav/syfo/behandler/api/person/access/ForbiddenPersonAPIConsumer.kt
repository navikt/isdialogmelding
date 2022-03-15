package no.nav.syfo.behandler.api.person.access

class ForbiddenPersonAPIConsumer(
    consumerClientId: String,
    message: String = "Consumer with clientId=$consumerClientId is denied access to Person API",
) : RuntimeException(message)
