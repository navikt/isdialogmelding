package no.nav.syfo.client.pdl.domain

data class PdlPersonResponse(
    val errors: List<PdlError>?,
    val data: PdlHentPerson?,
)

data class PdlError(
    val message: String,
    val locations: List<PdlErrorLocation>,
    val path: List<String>?,
    val extensions: PdlErrorExtension,
) {
    fun isNotFound() = this.extensions.code == "not_found"
}

data class PdlErrorLocation(
    val line: Int?,
    val column: Int?,
)

data class PdlErrorExtension(
    val code: String?,
    val classification: String,
)

data class PdlHentPerson(
    val hentPerson: PdlPerson?,
)

data class PdlPerson(
    val navn: List<PdlPersonNavn>,
)

data class PdlPersonNavn(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
)

fun PdlError.errorMessage(): String {
    return "${this.message} with code: ${extensions.code} and classification: ${extensions.classification}"
}
