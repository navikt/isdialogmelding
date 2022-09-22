package no.nav.syfo.oppfolgingsplan

import io.mockk.*
import no.nav.syfo.application.mq.MQSender
import no.nav.syfo.behandler.BehandlerService
import no.nav.syfo.behandler.DialogmeldingToBehandlerService
import no.nav.syfo.testhelper.generator.defaultFellesformatMessageXmlRegex
import no.nav.syfo.testhelper.generator.generateRSHodemelding
import org.junit.Assert.assertTrue
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object OppfolgingsplanServiceSpek : Spek({

    describe("OppfolgingsplanService") {
        it("Sends correct message on MQ") {
            val mqSender = mockk<MQSender>()
            val messageSlot = slot<String>()
            justRun { mqSender.sendMessageToEmottak(capture(messageSlot)) }

            val oppfolgingsplanService = OppfolgingsplanService(
                mqSender = mqSender,
                behandlerService = mockk<BehandlerService>(),
                dialogmeldingToBehandlerService = mockk<DialogmeldingToBehandlerService>(),
            )
            val rsHodemelding = generateRSHodemelding()

            oppfolgingsplanService.sendMelding(rsHodemelding)

            verify(exactly = 1) { mqSender.sendMessageToEmottak(any()) }

            val expectedFellesformatMessageAsRegex = defaultFellesformatMessageXmlRegex()
            val actualFellesformatMessage = messageSlot.captured

            assertTrue(
                expectedFellesformatMessageAsRegex.matches(actualFellesformatMessage),
            )
        }
    }
})
