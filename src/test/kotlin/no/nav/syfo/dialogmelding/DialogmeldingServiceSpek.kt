package no.nav.syfo.dialogmelding

import io.mockk.*
import kotlinx.coroutines.runBlocking
import no.nav.syfo.application.mq.MQSender
import no.nav.syfo.behandler.DialogmeldingToBehandlerService
import no.nav.syfo.behandler.kafka.dialogmeldingtobehandlerbestilling.toDialogmeldingToBehandlerBestilling
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.pdl.PdlClient
import no.nav.syfo.domain.PersonIdentNumber
import no.nav.syfo.testhelper.*
import no.nav.syfo.testhelper.generator.*
import org.junit.Assert.assertTrue
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.UUID

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
    val dialogmeldingToBehandlerService = DialogmeldingToBehandlerService(
        database = database,
        pdlClient = pdlClient,
    )
    val mqSender = mockk<MQSender>()

    val dialogmeldingService = DialogmeldingService(
        mqSender = mqSender,
        dialogmeldingToBehandlerService = dialogmeldingToBehandlerService,
    )

    val arbeidstakerPersonIdent = PersonIdentNumber("01010112345")
    val uuid = UUID.randomUUID()
    val behandlerRef = UUID.randomUUID()
    val behandler = generateBehandler(behandlerRef, 1)

    beforeEachTest {
        database.dropData()
        database.createBehandlerForArbeidstaker(
            behandler = behandler,
            arbeidstakerPersonIdent = arbeidstakerPersonIdent,
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
                arbeidstakerPersonIdent = arbeidstakerPersonIdent,
            ).toDialogmeldingToBehandlerBestilling(
                behandler = generateBehandler(
                    behandlerRef = behandlerRef,
                    partnerId = 1,
                ),
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
        it("Sends correct message on MQ when foresporsel endre tid-sted") {
            clearAllMocks()
            val messageSlot = slot<String>()
            justRun { mqSender.sendMessageToEmottak(capture(messageSlot)) }

            val melding = generateDialogmeldingToBehandlerBestillingEndreTidStedDTO(
                behandlerRef = behandlerRef,
                uuid = uuid,
                arbeidstakerPersonIdent = arbeidstakerPersonIdent,
            ).toDialogmeldingToBehandlerBestilling(
                behandler = generateBehandler(
                    behandlerRef = behandlerRef,
                    partnerId = 1,
                ),
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
                arbeidstakerPersonIdent = arbeidstakerPersonIdent,
            ).toDialogmeldingToBehandlerBestilling(
                behandler = generateBehandler(
                    behandlerRef = behandlerRef,
                    partnerId = 1,
                ),
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
                arbeidstakerPersonIdent = arbeidstakerPersonIdent,
            ).toDialogmeldingToBehandlerBestilling(
                behandler = generateBehandler(
                    behandlerRef = behandlerRef,
                    partnerId = 1,
                ),
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
    }
})
