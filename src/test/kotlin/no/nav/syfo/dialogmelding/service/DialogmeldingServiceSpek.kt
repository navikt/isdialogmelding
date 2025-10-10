package no.nav.syfo.dialogmelding.service

import io.mockk.clearAllMocks
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import no.nav.syfo.application.mq.MQSender
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.pdl.PdlClient
import no.nav.syfo.dialogmelding.DialogmeldingService
import no.nav.syfo.dialogmelding.bestilling.kafka.toDialogmeldingToBehandlerBestilling
import no.nav.syfo.domain.PartnerId
import no.nav.syfo.domain.Personident
import no.nav.syfo.testhelper.ExternalMockEnvironment
import no.nav.syfo.testhelper.UserConstants
import no.nav.syfo.testhelper.createBehandlerForArbeidstaker
import no.nav.syfo.testhelper.dropData
import no.nav.syfo.testhelper.generator.*
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import no.nav.syfo.dialogmelding.bestilling.domain.DialogmeldingToBehandlerBestilling
import no.nav.syfo.dialogmelding.bestilling.domain.DialogmeldingType
import no.nav.syfo.dialogmelding.bestilling.domain.DialogmeldingKode
import no.nav.syfo.dialogmelding.bestilling.domain.DialogmeldingKodeverk

object DialogmeldingServiceSpek : Spek({

    val externalMockEnvironment = ExternalMockEnvironment.instance
    val database = externalMockEnvironment.database
    val environment = ExternalMockEnvironment.instance.environment
    val pdlClient = PdlClient(
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
    val mqSender = mockk<MQSender>()

    val dialogmeldingService = DialogmeldingService(
        pdlClient = pdlClient,
        mqSender = mqSender,
    )

    val arbeidstakerPersonident = Personident("01010112345")
    val arbeidstakerPersonidentDNR = Personident("41010112345")
    val uuid = UUID.randomUUID()
    val behandlerRef = UUID.randomUUID()
    val behandler = generateBehandler(behandlerRef, PartnerId(1))
    val behandlerRefWithoutOrgnr = UUID.randomUUID()
    val behandlerWithoutOrgnr = generateBehandler(behandlerRefWithoutOrgnr, PartnerId(2), orgnummer = null)
    val behandlerRefWithDNR = UUID.randomUUID()
    val behandlerWithDNR =
        generateBehandler(behandlerRefWithDNR, PartnerId(1), personident = UserConstants.FASTLEGE_DNR)

    beforeEachTest {
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
    afterEachTest {
        database.dropData()
    }
    describe("DialogmeldingService") {
        it("Sends correct message on MQ when foresporsel dialogmote-innkalling") {
            val messageSlot = slot<String>()
            justRun { mqSender.sendMessageToEmottak(capture(messageSlot)) }

            val melding = generateDialogmeldingToBehandlerBestillingDTO(
                behandlerRef = behandlerRef,
                uuid = uuid,
                arbeidstakerPersonident = arbeidstakerPersonident,
            ).toDialogmeldingToBehandlerBestilling(
                behandler = behandler,
            )

            runBlocking {
                dialogmeldingService.sendMelding(melding)
            }
            verify(exactly = 1) { mqSender.sendMessageToEmottak(any()) }

            val expectedFellesformatMessageAsRegex = defaultFellesformatDialogmeldingXmlRegex()
            val actualFellesformatMessage = messageSlot.captured

            assertTrue(
                expectedFellesformatMessageAsRegex.matches(actualFellesformatMessage),
            )
        }
        it("Sends correct message on MQ when foresporsel dialogmote-innkalling for behandler and arbeidstaker with dnr") {
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

            runBlocking {
                dialogmeldingService.sendMelding(melding)
            }
            verify(exactly = 1) { mqSender.sendMessageToEmottak(any()) }

            val expectedFellesformatMessageAsRegex = defaultFellesformatDialogmeldingXmlDNRRegex()
            val actualFellesformatMessage = messageSlot.captured

            assertTrue(
                expectedFellesformatMessageAsRegex.matches(actualFellesformatMessage),
            )
        }
        it("Sends correct message on MQ when foresporsel dialogmote-innkalling to behandler without orgnr") {
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

            runBlocking {
                dialogmeldingService.sendMelding(melding)
            }
            verify(exactly = 1) { mqSender.sendMessageToEmottak(any()) }
        }
        it("Sends correct message on MQ when foresporsel endre tid-sted") {
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

            runBlocking {
                dialogmeldingService.sendMelding(melding)
            }
            verify(exactly = 1) { mqSender.sendMessageToEmottak(any()) }

            val expectedFellesformatMessageAsRegex = defaultFellesformatDialogmeldingEndreTidStedXmlRegex()
            val actualFellesformatMessage = messageSlot.captured

            assertTrue(
                expectedFellesformatMessageAsRegex.matches(actualFellesformatMessage),
            )
        }
        it("Sends correct message on MQ when referat") {
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

            runBlocking {
                dialogmeldingService.sendMelding(melding)
            }
            verify(exactly = 1) { mqSender.sendMessageToEmottak(any()) }

            val expectedFellesformatMessageAsRegex = defaultFellesformatDialogmeldingReferatXmlRegex()
            val actualFellesformatMessage = messageSlot.captured

            assertTrue(
                expectedFellesformatMessageAsRegex.matches(actualFellesformatMessage),
            )
        }
        it("Sends correct message on MQ when avlysning") {
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

            runBlocking {
                dialogmeldingService.sendMelding(melding)
            }
            verify(exactly = 1) { mqSender.sendMessageToEmottak(any()) }

            val expectedFellesformatMessageAsRegex = defaultFellesformatDialogmeldingAvlysningXmlRegex()
            val actualFellesformatMessage = messageSlot.captured

            assertTrue(
                expectedFellesformatMessageAsRegex.matches(actualFellesformatMessage),
            )
        }
        it("Sends correct message on MQ when foresporsel") {
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

            runBlocking {
                dialogmeldingService.sendMelding(melding)
            }
            verify(exactly = 1) { mqSender.sendMessageToEmottak(any()) }

            val expectedFellesformatMessageAsRegex = defaultFellesformatDialogmeldingForesporselXmlRegex()
            val actualFellesformatMessage = messageSlot.captured
            assertTrue(
                expectedFellesformatMessageAsRegex.matches(actualFellesformatMessage),
            )
        }
        it("Sends correct message on MQ when purring foresporsel") {
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

            runBlocking {
                dialogmeldingService.sendMelding(melding)
            }
            verify(exactly = 1) { mqSender.sendMessageToEmottak(any()) }

            val expectedFellesformatMessageAsRegex = defaultFellesformatDialogmeldingForesporselPurringXmlRegex()
            val actualFellesformatMessage = messageSlot.captured
            assertTrue(
                expectedFellesformatMessageAsRegex.matches(actualFellesformatMessage),
            )
        }
        it("Sends correct message on MQ when foresporsel om legeerklæring") {
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

            runBlocking {
                dialogmeldingService.sendMelding(melding)
            }
            verify(exactly = 1) { mqSender.sendMessageToEmottak(any()) }

            val expectedFellesformatMessageAsRegex = defaultFellesformatDialogmeldingForesporselLegeerklaringXmlRegex()
            val actualFellesformatMessage = messageSlot.captured
            assertTrue(
                expectedFellesformatMessageAsRegex.matches(actualFellesformatMessage),
            )
        }
        it("Sends correct message on MQ when retur av legeerklæring") {
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

            runBlocking {
                dialogmeldingService.sendMelding(melding)
            }
            verify(exactly = 1) { mqSender.sendMessageToEmottak(any()) }

            val expectedFellesformatMessageAsRegex = defaultFellesformatDialogmeldingReturLegeerklaringXmlRegex()
            val actualFellesformatMessage = messageSlot.captured
            assertTrue(
                expectedFellesformatMessageAsRegex.matches(actualFellesformatMessage),
            )
        }
        it("Sends correct message on MQ when friskmelding til arbeidsformidling") {
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

            runBlocking {
                dialogmeldingService.sendMelding(melding)
            }
            verify(exactly = 1) { mqSender.sendMessageToEmottak(any()) }

            val expectedFellesformatMessageAsRegex =
                defaultFellesformatDialogmeldingFriskmeldingTilArbeidsformidlingXmlRegex()
            val actualFellesformatMessage = messageSlot.captured
            assertTrue(
                expectedFellesformatMessageAsRegex.matches(actualFellesformatMessage),
            )
        }
        it("Sends correct message on MQ when melding fra NAV") {
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

            runBlocking {
                dialogmeldingService.sendMelding(melding)
            }
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
        it("Sends correct message on MQ when melding fra NAV after removing non ascii characters") {
            clearAllMocks()
            val messageSlot = slot<String>()
            justRun { mqSender.sendMessageToEmottak(capture(messageSlot)) }

            val meldingsTekstMedMystiskTegn = "Dette er en generell henvendelse med tekst med Æ per e\u0002post fra NAV \u0AAEsom ikke utløser takst"
            val meldingsTekstVasket = "Dette er en generell henvendelse med tekst med Æ per epost fra NAV som ikke utløser takst"
            val melding = generateDialogmeldingToBehandlerBestillingHenvendelseMeldingFraNavDTO(
                behandlerRef = behandlerRef,
                uuid = uuid,
                arbeidstakerPersonident = arbeidstakerPersonident,
                tekst = meldingsTekstMedMystiskTegn,
            ).toDialogmeldingToBehandlerBestilling(
                behandler = behandler,
            )

            runBlocking {
                dialogmeldingService.sendMelding(melding)
            }
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
        it("keeps tab, CR and LF and removes other control characters in tekst sanitization") {
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
        it("returns null when tekst is null in sanitization") {
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
            assertEquals(null, sanitized)
        }
        it("sanitization removes C1 controls, converts NBSP to space and removes soft hyphen") {
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
})
