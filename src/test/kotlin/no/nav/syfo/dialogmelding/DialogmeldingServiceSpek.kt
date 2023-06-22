package no.nav.syfo.dialogmelding

import io.mockk.*
import kotlinx.coroutines.runBlocking
import no.nav.syfo.application.mq.MQSender
import no.nav.syfo.dialogmelding.bestilling.kafka.toDialogmeldingToBehandlerBestilling
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.pdl.PdlClient
import no.nav.syfo.domain.PartnerId
import no.nav.syfo.domain.Personident
import no.nav.syfo.testhelper.*
import no.nav.syfo.testhelper.generator.*
import org.junit.Assert.assertTrue
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.*

object DialogmeldingServiceSpek : Spek({

    val externalMockEnvironment = ExternalMockEnvironment.instance
    val database = externalMockEnvironment.database
    val environment = ExternalMockEnvironment.instance.environment
    val pdlClient = PdlClient(
        azureAdClient = AzureAdClient(
            azureAppClientId = environment.aadAppClient,
            azureAppClientSecret = environment.azureAppClientSecret,
            azureOpenidConfigTokenEndpoint = environment.azureOpenidConfigTokenEndpoint,
        ),
        pdlClientId = environment.pdlClientId,
        pdlUrl = environment.pdlUrl,
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
    val behandlerWithDNR = generateBehandler(behandlerRefWithDNR, PartnerId(1), personident = UserConstants.FASTLEGE_DNR)

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

            val melding = generateDialogmeldingToBehandlerBestillingNotatReturLegeerklæringDTO(
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
    }
})
