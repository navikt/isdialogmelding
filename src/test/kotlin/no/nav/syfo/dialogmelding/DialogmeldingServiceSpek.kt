package no.nav.syfo.dialogmelding

import io.mockk.*
import no.nav.syfo.application.mq.MQSender
import no.nav.syfo.behandler.BehandlerDialogmeldingService
import no.nav.syfo.behandler.kafka.toBehandlerDialogmeldingBestilling
import no.nav.syfo.testhelper.ExternalMockEnvironment
import no.nav.syfo.testhelper.generator.*
import org.junit.Assert.assertTrue
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.UUID

object DialogmeldingServiceSpek : Spek({

    describe("DialogmeldingService") {
        it("Sends correct message on MQ") {
            val externalMockEnvironment = ExternalMockEnvironment.instance
            val database = externalMockEnvironment.database
            val behandlerDialogmeldingService = BehandlerDialogmeldingService(database)
            val mqSender = mockk<MQSender>()
            val messageSlot = slot<String>()
            justRun { mqSender.sendMessageToEmottak(capture(messageSlot)) }

            val dialogmeldingService = DialogmeldingService(
                mqSender = mqSender,
                behandlerDialogmeldingService = behandlerDialogmeldingService,
            )
            val uuid = UUID.randomUUID()
            val behandlerRef = UUID.randomUUID()
            val melding = generateBehandlerDialogmeldingBestillingDTO(
                behandlerRef = behandlerRef,
                uuid = uuid,
            ).toBehandlerDialogmeldingBestilling(
                behandler = generateBehandler(
                    behandlerRef = behandlerRef,
                    partnerId = 1,
                ),
            )

            dialogmeldingService.sendMelding(melding)

            verify(exactly = 1) { mqSender.sendMessageToEmottak(any()) }

            val expectedFellesformatMessageAsRegex = defaultFellesformatDialogmeldingXmlRegex()
            val actualFellesformatMessage = messageSlot.captured

            assertTrue(
                expectedFellesformatMessageAsRegex.matches(actualFellesformatMessage),
            )
        }
    }
})
