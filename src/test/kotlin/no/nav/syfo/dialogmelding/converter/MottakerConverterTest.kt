package no.nav.syfo.dialogmelding.converter

import no.nav.syfo.behandler.domain.BehandlerKategori
import no.nav.syfo.dialogmelding.bestilling.domain.DialogmeldingToBehandlerBestilling
import no.nav.syfo.domain.Personident
import no.nav.syfo.fellesformat.Fellesformat
import no.nav.syfo.testhelper.UserConstants
import no.nav.syfo.testhelper.generator.generateArbeidstaker
import no.nav.syfo.util.JAXB
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MottakerConverterTest {
    private val arbeidstakerPersonident = Personident("01010112345")

    private fun tjenesteBestilling(): DialogmeldingToBehandlerBestilling {
        val melding = createDialogmeldingToBehandlerBestilling(
            arbeidstakerPersonident = arbeidstakerPersonident,
            herId = UserConstants.TJENESTE_HERID,
        )
        return melding.copy(
            behandler = melding.behandler.copy(
                kategori = BehandlerKategori.TJENESTE,
                fornavn = "",
                mellomnavn = null,
                etternavn = UserConstants.TJENESTE_NAVN,
                personident = null,
                hprId = null,
            )
        )
    }

    @Test
    fun `tjeneste is addressed as sub-organisation of kontor without healthcare professional`() {
        val melding = tjenesteBestilling()

        val kontor = createReceiver(melding).organisation

        assertEquals(melding.behandler.kontor.navn, kontor.organisationName)
        assertTrue(kontor.healthcareProfessional == null)

        val tjeneste = kontor.organisation
        assertEquals(UserConstants.TJENESTE_NAVN, tjeneste.organisationName)
        assertEquals(1, tjeneste.ident.size)
        assertEquals(UserConstants.TJENESTE_HERID.toString(), tjeneste.ident.first().id)
        assertEquals("HER", tjeneste.ident.first().typeId.v)
    }

    @Test
    fun `tjeneste receiver keeps kontor HER-id`() {
        val melding = tjenesteBestilling()

        val kontor = createReceiver(melding).organisation

        assertTrue(
            kontor.ident.any { it.id == melding.behandler.kontor.herId.toString() && it.typeId.v == "HER" }
        )
    }

    @Test
    fun `lege is addressed with healthcare professional and no sub-organisation`() {
        val melding = createDialogmeldingToBehandlerBestilling(
            arbeidstakerPersonident = arbeidstakerPersonident,
        )

        val kontor = createReceiver(melding).organisation

        assertNull(kontor.organisation)
        assertEquals(melding.behandler.etternavn, kontor.healthcareProfessional.familyName)
        assertTrue(kontor.healthcareProfessional.ident.any { it.typeId.v == "HPR" })
    }

    @Test
    fun `fellesformat for tjeneste marshals without healthcare professional`() {
        val melding = tjenesteBestilling()
        val fellesformat = createFellesformat(melding, generateArbeidstaker(arbeidstakerPersonident))

        val xml = Fellesformat(fellesformat, JAXB::marshallDialogmelding1_0).message!!

        assertTrue(xml.contains("<ns2:OrganisationName>${UserConstants.TJENESTE_NAVN}</ns2:OrganisationName>"))
        assertTrue(xml.contains("<ns2:Id>${UserConstants.TJENESTE_HERID}</ns2:Id>"))
        assertFalse(xml.contains("HealthcareProfessional"))
    }
}
