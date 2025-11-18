package no.nav.syfo.dialogmelding.service

import io.mockk.*
import kotlinx.coroutines.test.runTest
import no.nav.syfo.application.mq.MQSender
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.pdl.PdlClient
import no.nav.syfo.dialogmelding.DialogmeldingService
import no.nav.syfo.dialogmelding.bestilling.domain.DialogmeldingKode
import no.nav.syfo.dialogmelding.bestilling.domain.DialogmeldingKodeverk
import no.nav.syfo.dialogmelding.bestilling.domain.DialogmeldingToBehandlerBestilling
import no.nav.syfo.dialogmelding.bestilling.domain.DialogmeldingType
import no.nav.syfo.dialogmelding.bestilling.kafka.toDialogmeldingToBehandlerBestilling
import no.nav.syfo.domain.PartnerId
import no.nav.syfo.domain.Personident
import no.nav.syfo.testhelper.ExternalMockEnvironment
import no.nav.syfo.testhelper.UserConstants
import no.nav.syfo.testhelper.createBehandlerForArbeidstaker
import no.nav.syfo.testhelper.dropData
import no.nav.syfo.testhelper.generator.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import java.util.*

class DialogmeldingServiceTest {
    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val database = externalMockEnvironment.database
    private val environment = ExternalMockEnvironment.instance.environment
    private val pdlClient = PdlClient(
        azureAdClient = AzureAdClient(
            azureAppClientId = environment.aadAppClient,
            azureAppClientSecret = environment.azureAppClientSecret,
            azureOpenidConfigTokenEndpoint = environment.azureOpenidConfigTokenEndpoint,
            httpClient = externalMockEnvironment.mockHttpClient,
        ),
        pdlClientId = environment.pdlClientId,
        pdlUrl = environment.pdlUrl,
        httpClient = externalMockEnvironment.mockHttpClient,
    )
    private val mqSender = mockk<MQSender>()

    private val dialogmeldingService = DialogmeldingService(
        pdlClient = pdlClient,
        mqSender = mqSender,
    )

    private val arbeidstakerPersonident = Personident("01010112345")
    private val arbeidstakerPersonidentDNR = Personident("41010112345")
    private val uuid = UUID.randomUUID()
    private val behandlerRef = UUID.randomUUID()
    private val behandler = generateBehandler(behandlerRef, PartnerId(1))
    private val behandlerRefWithoutOrgnr = UUID.randomUUID()
    private val behandlerWithoutOrgnr = generateBehandler(behandlerRefWithoutOrgnr, PartnerId(2), orgnummer = null)
    private val behandlerRefWithDNR = UUID.randomUUID()
    private val behandlerWithDNR =
        generateBehandler(behandlerRefWithDNR, PartnerId(1), personident = UserConstants.FASTLEGE_DNR)

    @BeforeEach
    fun beforeEach() {
        database.dropData()
        database.createBehandlerForArbeidstaker(
            behandler = behandler,
            arbeidstakerPersonident = arbeidstakerPersonident,
        )
        database.createBehandlerForArbeidstaker(
            behandler = behandlerWithDNR,
            arbeidstakerPersonident = arbeidstakerPersonidentDNR,
        )
        database.createBehandlerForArbeidstaker(
            behandler = behandlerWithoutOrgnr,
            arbeidstakerPersonident = arbeidstakerPersonident,
        )
    }

    @Test
    fun `Sends correct message on MQ when foresporsel dialogmote-innkalling`() = runTest {
        val messageSlot = slot<String>()
        justRun { mqSender.sendMessageToEmottak(capture(messageSlot)) }

        val melding = generateDialogmeldingToBehandlerBestillingDTO(
            behandlerRef = behandlerRef,
            uuid = uuid,
            arbeidstakerPersonident = arbeidstakerPersonident,
        ).toDialogmeldingToBehandlerBestilling(
            behandler = behandler,
        )

        dialogmeldingService.sendMelding(melding)
        verify(exactly = 1) { mqSender.sendMessageToEmottak(any()) }

        val expectedFellesformatMessageAsRegex = defaultFellesformatDialogmeldingXmlRegex()
        val actualFellesformatMessage = messageSlot.captured

        assertTrue(
            expectedFellesformatMessageAsRegex.matches(actualFellesformatMessage),
        )
    }

    @Test
    fun `Sends correct message on MQ when foresporsel dialogmote-innkalling for behandler and arbeidstaker with dnr`() =
        runTest {
            clearAllMocks()
            val messageSlot = slot<String>()
            justRun { mqSender.sendMessageToEmottak(capture(messageSlot)) }

            val melding = generateDialogmeldingToBehandlerBestillingDTO(
                behandlerRef = behandlerRefWithDNR,
                uuid = uuid,
                arbeidstakerPersonident = arbeidstakerPersonidentDNR,
            ).toDialogmeldingToBehandlerBestilling(
                behandler = behandlerWithDNR,
            )

            dialogmeldingService.sendMelding(melding)
            verify(exactly = 1) { mqSender.sendMessageToEmottak(any()) }

            val expectedFellesformatMessageAsRegex = defaultFellesformatDialogmeldingXmlDNRRegex()
            val actualFellesformatMessage = messageSlot.captured

            assertTrue(
                expectedFellesformatMessageAsRegex.matches(actualFellesformatMessage),
            )
        }

    @Test
    fun `Sends correct message on MQ when foresporsel dialogmote-innkalling to behandler without orgnr`() = runTest {
        clearAllMocks()
        val messageSlot = slot<String>()
        justRun { mqSender.sendMessageToEmottak(capture(messageSlot)) }

        val melding = generateDialogmeldingToBehandlerBestillingDTO(
            behandlerRef = behandlerRefWithoutOrgnr,
            uuid = uuid,
            arbeidstakerPersonident = arbeidstakerPersonident,
        ).toDialogmeldingToBehandlerBestilling(
            behandler = behandlerWithoutOrgnr,
        )

        dialogmeldingService.sendMelding(melding)
        verify(exactly = 1) { mqSender.sendMessageToEmottak(any()) }
    }

    @Test
    fun `Sends correct message on MQ when foresporsel endre tid-sted`() = runTest {
        clearAllMocks()
        val messageSlot = slot<String>()
        justRun { mqSender.sendMessageToEmottak(capture(messageSlot)) }

        val melding = generateDialogmeldingToBehandlerBestillingEndreTidStedDTO(
            behandlerRef = behandlerRef,
            uuid = uuid,
            arbeidstakerPersonident = arbeidstakerPersonident,
        ).toDialogmeldingToBehandlerBestilling(
            behandler = behandler,
        )

        dialogmeldingService.sendMelding(melding)
        verify(exactly = 1) { mqSender.sendMessageToEmottak(any()) }

        val expectedFellesformatMessageAsRegex = defaultFellesformatDialogmeldingEndreTidStedXmlRegex()
        val actualFellesformatMessage = messageSlot.captured

        assertTrue(
            expectedFellesformatMessageAsRegex.matches(actualFellesformatMessage),
        )
    }

    @Test
    fun `Sends correct message on MQ when referat`() = runTest {
        clearAllMocks()
        val messageSlot = slot<String>()
        justRun { mqSender.sendMessageToEmottak(capture(messageSlot)) }

        val melding = generateDialogmeldingToBehandlerBestillingReferatDTO(
            behandlerRef = behandlerRef,
            uuid = uuid,
            arbeidstakerPersonident = arbeidstakerPersonident,
        ).toDialogmeldingToBehandlerBestilling(
            behandler = behandler,
        )

        dialogmeldingService.sendMelding(melding)
        verify(exactly = 1) { mqSender.sendMessageToEmottak(any()) }

        val expectedFellesformatMessageAsRegex = defaultFellesformatDialogmeldingReferatXmlRegex()
        val actualFellesformatMessage = messageSlot.captured

        assertTrue(
            expectedFellesformatMessageAsRegex.matches(actualFellesformatMessage),
        )
    }

    @Test
    fun `Sends correct message on MQ when avlysning`() = runTest {
        clearAllMocks()
        val messageSlot = slot<String>()
        justRun { mqSender.sendMessageToEmottak(capture(messageSlot)) }

        val melding = generateDialogmeldingToBehandlerBestillingAvlysningDTO(
            behandlerRef = behandlerRef,
            uuid = uuid,
            arbeidstakerPersonident = arbeidstakerPersonident,
        ).toDialogmeldingToBehandlerBestilling(
            behandler = behandler,
        )

        dialogmeldingService.sendMelding(melding)
        verify(exactly = 1) { mqSender.sendMessageToEmottak(any()) }

        val expectedFellesformatMessageAsRegex = defaultFellesformatDialogmeldingAvlysningXmlRegex()
        val actualFellesformatMessage = messageSlot.captured

        assertTrue(
            expectedFellesformatMessageAsRegex.matches(actualFellesformatMessage),
        )
    }

    @Test
    fun `Sends correct message on MQ when foresporsel`() = runTest {
        clearAllMocks()
        val messageSlot = slot<String>()
        justRun { mqSender.sendMessageToEmottak(capture(messageSlot)) }

        val melding = generateDialogmeldingToBehandlerBestillingForesporselDTO(
            behandlerRef = behandlerRef,
            uuid = uuid,
            arbeidstakerPersonident = arbeidstakerPersonident,
        ).toDialogmeldingToBehandlerBestilling(
            behandler = behandler,
        )

        dialogmeldingService.sendMelding(melding)
        verify(exactly = 1) { mqSender.sendMessageToEmottak(any()) }

        val expectedFellesformatMessageAsRegex = defaultFellesformatDialogmeldingForesporselXmlRegex()
        val actualFellesformatMessage = messageSlot.captured
        assertTrue(
            expectedFellesformatMessageAsRegex.matches(actualFellesformatMessage),
        )
    }

    @Test
    fun `Sends correct message on MQ when purring foresporsel`() = runTest {
        clearAllMocks()
        val messageSlot = slot<String>()
        justRun { mqSender.sendMessageToEmottak(capture(messageSlot)) }

        val melding = generateDialogmeldingToBehandlerBestillingForesporselPurringDTO(
            behandlerRef = behandlerRef,
            uuid = uuid,
            arbeidstakerPersonident = arbeidstakerPersonident,
        ).toDialogmeldingToBehandlerBestilling(
            behandler = behandler,
        )

        dialogmeldingService.sendMelding(melding)
        verify(exactly = 1) { mqSender.sendMessageToEmottak(any()) }

        val expectedFellesformatMessageAsRegex = defaultFellesformatDialogmeldingForesporselPurringXmlRegex()
        val actualFellesformatMessage = messageSlot.captured
        assertTrue(
            expectedFellesformatMessageAsRegex.matches(actualFellesformatMessage),
        )
    }

    @Test
    fun `Sends correct message on MQ when foresporsel om legeerklæring`() = runTest {
        clearAllMocks()
        val messageSlot = slot<String>()
        justRun { mqSender.sendMessageToEmottak(capture(messageSlot)) }

        val melding = generateDialogmeldingToBehandlerBestillingForesporselLegeerklaringDTO(
            behandlerRef = behandlerRef,
            uuid = uuid,
            arbeidstakerPersonident = arbeidstakerPersonident,
        ).toDialogmeldingToBehandlerBestilling(
            behandler = behandler,
        )

        dialogmeldingService.sendMelding(melding)
        verify(exactly = 1) { mqSender.sendMessageToEmottak(any()) }

        val expectedFellesformatMessageAsRegex = defaultFellesformatDialogmeldingForesporselLegeerklaringXmlRegex()
        val actualFellesformatMessage = messageSlot.captured
        assertTrue(
            expectedFellesformatMessageAsRegex.matches(actualFellesformatMessage),
        )
    }

    @Test
    fun `Sends correct message on MQ when retur av legeerklæring`() = runTest {
        clearAllMocks()
        val messageSlot = slot<String>()
        justRun { mqSender.sendMessageToEmottak(capture(messageSlot)) }

        val melding = generateDialogmeldingToBehandlerBestillingNotatReturLegeerklaringDTO(
            behandlerRef = behandlerRef,
            uuid = uuid,
            arbeidstakerPersonident = arbeidstakerPersonident,
        ).toDialogmeldingToBehandlerBestilling(
            behandler = behandler,
        )

        dialogmeldingService.sendMelding(melding)
        verify(exactly = 1) { mqSender.sendMessageToEmottak(any()) }

        val expectedFellesformatMessageAsRegex = defaultFellesformatDialogmeldingReturLegeerklaringXmlRegex()
        val actualFellesformatMessage = messageSlot.captured
        assertTrue(
            expectedFellesformatMessageAsRegex.matches(actualFellesformatMessage),
        )
    }

    @Test
    fun `Sends correct message on MQ when friskmelding til arbeidsformidling`() = runTest {
        clearAllMocks()
        val messageSlot = slot<String>()
        justRun { mqSender.sendMessageToEmottak(capture(messageSlot)) }

        val melding = generateDialogmeldingToBehandlerBestillingNotatFriskmeldingTilArbeidsformidlingDTO(
            behandlerRef = behandlerRef,
            uuid = uuid,
            arbeidstakerPersonident = arbeidstakerPersonident,
        ).toDialogmeldingToBehandlerBestilling(
            behandler = behandler,
        )

        dialogmeldingService.sendMelding(melding)
        verify(exactly = 1) { mqSender.sendMessageToEmottak(any()) }

        val expectedFellesformatMessageAsRegex =
            defaultFellesformatDialogmeldingFriskmeldingTilArbeidsformidlingXmlRegex()
        val actualFellesformatMessage = messageSlot.captured
        assertTrue(
            expectedFellesformatMessageAsRegex.matches(actualFellesformatMessage),
        )
    }

    @Test
    fun `Sends correct message on MQ when melding fra NAV`() = runTest {
        clearAllMocks()
        val messageSlot = slot<String>()
        justRun { mqSender.sendMessageToEmottak(capture(messageSlot)) }

        val meldingsTekst = "Dette er en generell henvendelse med tekst med Æ per epost fra NAV som ikke utløser takst"
        val melding = generateDialogmeldingToBehandlerBestillingHenvendelseMeldingFraNavDTO(
            behandlerRef = behandlerRef,
            uuid = uuid,
            arbeidstakerPersonident = arbeidstakerPersonident,
            tekst = meldingsTekst,
        ).toDialogmeldingToBehandlerBestilling(
            behandler = behandler,
        )

        dialogmeldingService.sendMelding(melding)
        verify(exactly = 1) { mqSender.sendMessageToEmottak(any()) }

        val expectedFellesformatMessageAsRegex = defaultFellesformatDialogmeldingHenvendelseMeldingFraNavXmlRegex()
        val actualFellesformatMessage = messageSlot.captured
        assertTrue(
            expectedFellesformatMessageAsRegex.matches(actualFellesformatMessage),
        )
        assertTrue(
            actualFellesformatMessage.contains(meldingsTekst),
        )
    }

    @Test
    fun `Sends correct message on MQ when melding fra NAV after removing non ascii characters`() = runTest {
        clearAllMocks()
        val messageSlot = slot<String>()
        justRun { mqSender.sendMessageToEmottak(capture(messageSlot)) }

        val meldingsTekstMedMystiskTegn =
            "Dette er en generell henvendelse med tekst med Æ per e\u0002post fra NAV \u0AAEsom ikke utløser takst"
        val meldingsTekstVasket =
            "Dette er en generell henvendelse med tekst med Æ per epost fra NAV som ikke utløser takst"
        val melding = generateDialogmeldingToBehandlerBestillingHenvendelseMeldingFraNavDTO(
            behandlerRef = behandlerRef,
            uuid = uuid,
            arbeidstakerPersonident = arbeidstakerPersonident,
            tekst = meldingsTekstMedMystiskTegn,
        ).toDialogmeldingToBehandlerBestilling(
            behandler = behandler,
        )

        dialogmeldingService.sendMelding(melding)
        verify(exactly = 1) { mqSender.sendMessageToEmottak(any()) }

        val expectedFellesformatMessageAsRegex = defaultFellesformatDialogmeldingHenvendelseMeldingFraNavXmlRegex()
        val actualFellesformatMessage = messageSlot.captured
        assertTrue(
            expectedFellesformatMessageAsRegex.matches(actualFellesformatMessage),
        )
        assertTrue(
            actualFellesformatMessage.contains(meldingsTekstVasket),
        )
    }

    @Test
    fun `keeps tab, CR and LF and removes other control characters in tekst sanitization`() = runTest {
        val original = "Hello\tWorld\r\nLine2" + "\u0001" + "\u0002" + "End" + "\u0007"
        val melding = DialogmeldingToBehandlerBestilling(
            uuid = UUID.randomUUID(),
            behandler = behandler,
            arbeidstakerPersonident = arbeidstakerPersonident,
            parentRef = null,
            conversationUuid = UUID.randomUUID(),
            type = DialogmeldingType.DIALOG_NOTAT,
            kodeverk = DialogmeldingKodeverk.DIALOGMOTE,
            kode = DialogmeldingKode.KODE1,
            tekst = original,
            vedlegg = null,
            kilde = "test",
        )
        val sanitized = melding.getTekstRemoveInvalidCharacters()
        assertEquals("Hello\tWorld\r\nLine2End", sanitized)
    }

    @Test
    fun `returns null when tekst is null in sanitization`() = runTest {
        val melding = DialogmeldingToBehandlerBestilling(
            uuid = UUID.randomUUID(),
            behandler = behandler,
            arbeidstakerPersonident = arbeidstakerPersonident,
            parentRef = null,
            conversationUuid = UUID.randomUUID(),
            type = DialogmeldingType.DIALOG_NOTAT,
            kodeverk = DialogmeldingKodeverk.DIALOGMOTE,
            kode = DialogmeldingKode.KODE1,
            tekst = null,
            vedlegg = null,
            kilde = "test",
        )
        val sanitized = melding.getTekstRemoveInvalidCharacters()
        assertNull(sanitized)
    }

    @Test
    fun `sanitization removes C1 controls, converts NBSP to space and removes soft hyphen`() = runTest {
        val original = "Tekst\u00A0med\u00ADmystiske tegn"
        val melding = DialogmeldingToBehandlerBestilling(
            uuid = UUID.randomUUID(),
            behandler = behandler,
            arbeidstakerPersonident = arbeidstakerPersonident,
            parentRef = null,
            conversationUuid = UUID.randomUUID(),
            type = DialogmeldingType.DIALOG_NOTAT,
            kodeverk = DialogmeldingKodeverk.DIALOGMOTE,
            kode = DialogmeldingKode.KODE1,
            tekst = original,
            vedlegg = null,
            kilde = "test",
        )
        val sanitized = melding.getTekstRemoveInvalidCharacters()
        assertEquals("Tekst medmystiske tegn", sanitized)
    }
}
