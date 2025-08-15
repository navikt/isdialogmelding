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
import no.nav.syfo.testhelper.generator.defaultFellesformatDialogmeldingAvlysningXmlRegex
import no.nav.syfo.testhelper.generator.defaultFellesformatDialogmeldingEndreTidStedXmlRegex
import no.nav.syfo.testhelper.generator.defaultFellesformatDialogmeldingForesporselLegeerklaringXmlRegex
import no.nav.syfo.testhelper.generator.defaultFellesformatDialogmeldingForesporselPurringXmlRegex
import no.nav.syfo.testhelper.generator.defaultFellesformatDialogmeldingForesporselXmlRegex
import no.nav.syfo.testhelper.generator.defaultFellesformatDialogmeldingFriskmeldingTilArbeidsformidlingXmlRegex
import no.nav.syfo.testhelper.generator.defaultFellesformatDialogmeldingHenvendelseMeldingFraNavXmlRegex
import no.nav.syfo.testhelper.generator.defaultFellesformatDialogmeldingReferatXmlRegex
import no.nav.syfo.testhelper.generator.defaultFellesformatDialogmeldingReturLegeerklaringXmlRegex
import no.nav.syfo.testhelper.generator.defaultFellesformatDialogmeldingXmlDNRRegex
import no.nav.syfo.testhelper.generator.defaultFellesformatDialogmeldingXmlRegex
import no.nav.syfo.testhelper.generator.generateBehandler
import no.nav.syfo.testhelper.generator.generateDialogmeldingToBehandlerBestillingAvlysningDTO
import no.nav.syfo.testhelper.generator.generateDialogmeldingToBehandlerBestillingDTO
import no.nav.syfo.testhelper.generator.generateDialogmeldingToBehandlerBestillingEndreTidStedDTO
import no.nav.syfo.testhelper.generator.generateDialogmeldingToBehandlerBestillingForesporselDTO
import no.nav.syfo.testhelper.generator.generateDialogmeldingToBehandlerBestillingForesporselLegeerklaringDTO
import no.nav.syfo.testhelper.generator.generateDialogmeldingToBehandlerBestillingForesporselPurringDTO
import no.nav.syfo.testhelper.generator.generateDialogmeldingToBehandlerBestillingHenvendelseMeldingFraNavDTO
import no.nav.syfo.testhelper.generator.generateDialogmeldingToBehandlerBestillingNotatFriskmeldingTilArbeidsformidlingDTO
import no.nav.syfo.testhelper.generator.generateDialogmeldingToBehandlerBestillingNotatReturLegeerklaringDTO
import no.nav.syfo.testhelper.generator.generateDialogmeldingToBehandlerBestillingReferatDTO
import org.slf4j.LoggerFactory
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.UUID
import kotlin.test.assertTrue

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

            val meldingsTekst = "Dette er en generell henvendelse per epost fra NAV som ikke utløser takst"
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

            val meldingsTekstMedMystiskTegn = "Dette er en generell henvendelse per e\u0002post fra NAV \u0AAEsom ikke utløser takst"
            val meldingsTekstVasket = "Dette er en generell henvendelse per epost fra NAV som ikke utløser takst"
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
    }
})
