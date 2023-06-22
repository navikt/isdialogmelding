package no.nav.syfo.testhelper.mocks

import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import no.nav.syfo.client.pdl.domain.*
import no.nav.syfo.testhelper.UserConstants
import no.nav.syfo.testhelper.generator.generatePdlPersonResponse

suspend fun MockRequestHandleScope.pdlMockResponse(request: HttpRequestData): HttpResponseData {
    val isHentIdenterRequest = request.receiveBody<Any>().toString().contains("hentIdenter")
    return if (isHentIdenterRequest) {
        val pdlRequest = request.receiveBody<PdlHentIdenterRequest>()
        return when (pdlRequest.variables.ident) {
            UserConstants.TREDJE_ARBEIDSTAKER_FNR.value -> respondOk(generatePdlIdenter("enAnnenIdent"))
            UserConstants.ARBEIDSTAKER_FNR_WITH_ERROR.value -> respondOk(
                generatePdlIdenter(pdlRequest.variables.ident).copy(
                    errors = generatePdlError(
                        code = "not_found"
                    )
                )
            )

            else -> {
                respondOk(generatePdlIdenter(pdlRequest.variables.ident))
            }
        }
    } else {
        respondOk(generatePdlPersonResponse())
    }
}

fun generatePdlIdenter(
    personident: String,
) = PdlIdenterResponse(
    data = PdlHentIdenter(
        hentIdenter = PdlIdenter(
            identer = listOf(
                PdlIdent(
                    ident = personident,
                    historisk = false,
                    gruppe = IdentType.FOLKEREGISTERIDENT,
                ),
                PdlIdent(
                    ident = personident.toFakeOldIdent(),
                    historisk = true,
                    gruppe = IdentType.FOLKEREGISTERIDENT,
                ),
            ),
        ),
    ),
    errors = null,
)

fun generatePdlError(code: String? = null) = listOf(
    PdlError(
        message = "Error",
        locations = emptyList(),
        path = emptyList(),
        extensions = PdlErrorExtension(
            code = code,
            classification = "Classification",
        )
    )
)

private fun String.toFakeOldIdent(): String {
    val substring = this.drop(1)
    return "9$substring"
}
