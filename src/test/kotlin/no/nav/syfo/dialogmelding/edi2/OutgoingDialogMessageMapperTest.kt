package no.nav.syfo.dialogmelding.edi2

import no.nav.syfo.dialogmelding.bestilling.kafka.toDialogmeldingToBehandlerBestilling
import no.nav.syfo.domain.PartnerId
import no.nav.syfo.testhelper.generator.generateBehandler
import no.nav.syfo.testhelper.generator.generateDialogmeldingToBehandlerBestillingDTO
import no.nav.syfo.testhelper.generator.generateDialogmeldingToBehandlerBestillingOppfolgingsplanDTO
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.UUID

class OutgoingDialogMessageMapperTest {
    @Test
    fun `Maps foresporsel to outgoing dialog message`() {
        val behandler = generateBehandler(
            behandlerRef = UUID.randomUUID(),
            partnerId = PartnerId(1),
        )
        val melding = generateDialogmeldingToBehandlerBestillingDTO(
            behandlerRef = behandler.behandlerRef,
            uuid = UUID.randomUUID(),
        ).toDialogmeldingToBehandlerBestilling(
            behandler = behandler,
        )

        val outgoing = melding.toOutgoingDialogMessage()

        assertEquals(1, outgoing.version)
        assertEquals(melding.uuid.toString(), outgoing.id)
        assertEquals(melding.arbeidstakerPersonident.value, outgoing.patientIdent)
        assertEquals(behandler.behandlerRef.toString(), outgoing.providerId)
        assertEquals("MEETING_INVITATION_2", outgoing.type)
        assertEquals("En tekst", outgoing.message)
        assertNull(outgoing.attachment)
        assertNotNull(outgoing.conversationReference)
    }

    @Test
    fun `Maps oppfolgingsplan to outgoing dialog message with fixed text and no conversation reference`() {
        val behandler = generateBehandler(
            behandlerRef = UUID.randomUUID(),
            partnerId = PartnerId(1),
        )
        val melding = generateDialogmeldingToBehandlerBestillingOppfolgingsplanDTO(
            behandlerRef = behandler.behandlerRef,
            uuid = UUID.randomUUID(),
        ).toDialogmeldingToBehandlerBestilling(
            behandler = behandler,
        )

        val outgoing = melding.toOutgoingDialogMessage()

        assertEquals("FOLLOW_UP_PLAN", outgoing.type)
        assertEquals("Åpne PDF-vedlegg", outgoing.message)
        assertNotNull(outgoing.attachment)
        assertNull(outgoing.conversationReference)
    }
}
