package no.nav.syfo.dialogmelding.converter

import no.nav.syfo.behandler.domain.DialogmeldingToBehandlerBestilling
import no.nav.syfo.behandler.kafka.dialogmeldingtobehandlerbestilling.toDialogmeldingToBehandlerBestilling
import no.nav.syfo.domain.PartnerId
import no.nav.syfo.domain.Personident
import no.nav.syfo.fellesformat.Fellesformat
import no.nav.syfo.testhelper.generator.*
import no.nav.syfo.util.JAXB
import org.junit.Assert.assertTrue
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.*

class FellesformatConverterSpek : Spek({
    val arbeidstakerPersonident = Personident("01010112345")

    describe("FellesformatConverter for dialogmelding creates melding to behandler, with arbeidstaker as patient") {
        it("create XMLEIFellesformat without herID as ident for behandler") {
            val melding = createDialogmeldingToBehandlerBestilling(
                arbeidstakerPersonident = arbeidstakerPersonident,
                herId = null,
            )
            val arbeidstaker = generateArbeidstaker(arbeidstakerPersonident)
            val expectedFellesformatMessageAsRegex = defaultFellesformatDialogmeldingNoHerIdXmlRegex()

            val actualXMLEIFellesformat = createFellesformat(
                melding,
                arbeidstaker,
            )
            val actualFellesformat = Fellesformat(actualXMLEIFellesformat, JAXB::marshallDialogmelding1_0)

            assertTrue(
                expectedFellesformatMessageAsRegex.matches(actualFellesformat.message!!),
            )
        }
    }
})

fun createDialogmeldingToBehandlerBestilling(
    arbeidstakerPersonident: Personident,
    herId: Int? = 77,
): DialogmeldingToBehandlerBestilling {
    val behandlerRef = UUID.randomUUID()

    return generateDialogmeldingToBehandlerBestillingDTO(
        behandlerRef = behandlerRef,
        uuid = UUID.randomUUID(),
        arbeidstakerPersonident = arbeidstakerPersonident,
    ).toDialogmeldingToBehandlerBestilling(
        behandler = generateBehandler(
            behandlerRef = behandlerRef,
            partnerId = PartnerId(1),
            herId = herId,
        ),
    )
}
