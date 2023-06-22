package no.nav.syfo.testhelper.mocks

import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import no.nav.syfo.behandler.partnerinfo.PartnerinfoResponse
import no.nav.syfo.testhelper.UserConstants

fun MockRequestHandleScope.syfoPartnerInfoMockResponse(request: HttpRequestData): HttpResponseData =
    when (request.url.parameters["herid"]) {
        UserConstants.HERID_UTEN_PARTNERINFO.toString() -> respondOk(
            emptyList<PartnerinfoResponse>(),
        )

        UserConstants.HERID_MED_FLERE_PARTNERINFO.toString() -> respondOk(
            listOf(
                PartnerinfoResponse(UserConstants.PARTNERID.value),
                PartnerinfoResponse(UserConstants.OTHER_PARTNERID.value),
            )
        )

        UserConstants.OTHER_HERID.toString() -> respondOk(
            listOf(PartnerinfoResponse(UserConstants.OTHER_PARTNERID.value)),
        )

        else -> respondOk(
            listOf(PartnerinfoResponse(UserConstants.PARTNERID.value)),
        )
    }
