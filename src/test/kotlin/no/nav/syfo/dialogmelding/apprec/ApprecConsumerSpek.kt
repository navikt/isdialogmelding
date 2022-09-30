package no.nav.syfo.dialogmelding.apprec

import io.ktor.server.testing.*
import io.mockk.*
import kotlinx.coroutines.runBlocking
import no.nav.syfo.behandler.database.*
import no.nav.syfo.behandler.kafka.dialogmeldingtobehandlerbestilling.toDialogmeldingToBehandlerBestilling
import no.nav.syfo.dialogmelding.apprec.consumer.ApprecConsumer
import no.nav.syfo.dialogmelding.apprec.database.getApprec
import no.nav.syfo.domain.PartnerId
import no.nav.syfo.testhelper.*
import no.nav.syfo.testhelper.generator.generateBehandler
import no.nav.syfo.testhelper.generator.generateDialogmeldingToBehandlerBestillingDTO
import org.amshove.kluent.*
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.Random
import java.util.UUID
import javax.jms.*

val externalMockEnvironment = ExternalMockEnvironment.instance
val database = externalMockEnvironment.database

class ApprecConsumerSpek : Spek({
    describe(ApprecConsumerSpek::class.java.simpleName) {
        with(TestApplicationEngine()) {
            start()
            val incomingMessage = mockk<TextMessage>(relaxed = true)

            val apprecConsumer = ApprecConsumer(
                applicationState = externalMockEnvironment.applicationState,
                database = database,
                inputconsumer = mockk(),
            )

            describe("Prosesserer innkommet melding") {

                beforeEachTest {
                    database.dropData()
                    clearAllMocks()
                }
                it("Prosesserer innkommet melding (melding ok)") {
                    val apprecId = UUID.randomUUID()
                    val dialogmeldingBestillingUuid = UUID.randomUUID()
                    val bestillingId = lagDialogmeldingBestilling(dialogmeldingBestillingUuid)
                    val apprecXml =
                        getFileAsString("src/test/resources/apprecOK.xml")
                            .replace("FiktivTestdata0001", apprecId.toString())
                            .replace("b62016eb-6c2d-417a-8ecc-157b3c5ee2ca", dialogmeldingBestillingUuid.toString())
                    every { incomingMessage.text } returns(apprecXml)
                    runBlocking {
                        apprecConsumer.processApprecMessage(incomingMessage)
                    }

                    val pApprec = database.getApprec(apprecId)

                    pApprec shouldNotBe null
                    pApprec!!.uuid shouldBeEqualTo apprecId
                    pApprec.bestillingId shouldBeEqualTo bestillingId
                    pApprec.statusKode shouldBeEqualTo "1"
                    pApprec.statusTekst shouldBeEqualTo "OK"
                    pApprec.feilKode shouldBe null
                    pApprec.feilTekst shouldBe null
                }
                it("Prosesserer innkommet melding (melding ok, med duplikat)") {
                    val apprecId = UUID.randomUUID()
                    val dialogmeldingBestillingUuid = UUID.randomUUID()
                    lagDialogmeldingBestilling(dialogmeldingBestillingUuid)
                    val apprecXml =
                        getFileAsString("src/test/resources/apprecOK.xml")
                            .replace("FiktivTestdata0001", apprecId.toString())
                            .replace("b62016eb-6c2d-417a-8ecc-157b3c5ee2ca", dialogmeldingBestillingUuid.toString())
                    every { incomingMessage.text } returns(apprecXml)
                    runBlocking {
                        apprecConsumer.processApprecMessage(incomingMessage)
                    }

                    val pApprec = database.getApprec(apprecId)

                    pApprec shouldNotBe null
                    pApprec!!.uuid shouldBeEqualTo apprecId

                    runBlocking {
                        apprecConsumer.processApprecMessage(incomingMessage)
                    }
                }
                it("Prosesserer innkommet melding (melding ok, men ikke knyttet til kjent dialogmelding)") {
                    val apprecId = UUID.randomUUID()
                    val dialogmeldingBestillingUuid = UUID.randomUUID()
                    lagDialogmeldingBestilling(dialogmeldingBestillingUuid)
                    val ukjentDialogmeldingBestillingUuid = UUID.randomUUID()
                    val apprecXml =
                        getFileAsString("src/test/resources/apprecOK.xml")
                            .replace("FiktivTestdata0001", apprecId.toString())
                            .replace("b62016eb-6c2d-417a-8ecc-157b3c5ee2ca", ukjentDialogmeldingBestillingUuid.toString())
                    every { incomingMessage.text } returns(apprecXml)
                    runBlocking {
                        apprecConsumer.processApprecMessage(incomingMessage)
                    }

                    val pApprec = database.getApprec(apprecId)

                    pApprec shouldBe null
                }
                it("Prosesserer innkommet melding (melding avvist)") {
                    val apprecId = UUID.randomUUID()
                    val dialogmeldingBestillingUuid = UUID.randomUUID()
                    val bestillingId = lagDialogmeldingBestilling(dialogmeldingBestillingUuid)
                    val apprecXml =
                        getFileAsString("src/test/resources/apprecError.xml")
                            .replace("FiktivTestdata0001", apprecId.toString())
                            .replace("b62016eb-6c2d-417a-8ecc-157b3c5ee2ca", dialogmeldingBestillingUuid.toString())
                    every { incomingMessage.text } returns(apprecXml)
                    runBlocking {
                        apprecConsumer.processApprecMessage(incomingMessage)
                    }

                    val pApprec = database.getApprec(apprecId)

                    pApprec shouldNotBe null
                    pApprec!!.uuid shouldBeEqualTo apprecId
                    pApprec.bestillingId shouldBeEqualTo bestillingId
                    pApprec.statusKode shouldBeEqualTo "2"
                    pApprec.statusTekst shouldBeEqualTo "Avvist"
                    pApprec.feilKode shouldBeEqualTo "X99"
                    pApprec.feilTekst shouldBeEqualTo "Annen feil"
                }
                it("Prosesserer innkommet feilformattert melding") {
                    val apprecXml = "Ikke noen apprec"
                    every { incomingMessage.text } returns(apprecXml)
                    runBlocking {
                        apprecConsumer.processApprecMessage(incomingMessage)
                    }
                    // should not get an exception
                }
            }
        }
    }
})

fun lagDialogmeldingBestilling(dialogmeldingBestillingUuid: UUID): Int {
    val random = Random()
    val behandlerRef = UUID.randomUUID()
    val partnerId = PartnerId(random.nextInt())
    val behandler = generateBehandler(behandlerRef, partnerId)
    val behandlerId = database.connection.use { connection ->
        val kontorId = connection.createBehandlerKontor(behandler.kontor)
        connection.createBehandler(behandler, kontorId).id.also {
            connection.commit()
        }
    }

    val dialogmeldingToBehandlerBestillingDTO = generateDialogmeldingToBehandlerBestillingDTO(
        uuid = dialogmeldingBestillingUuid,
        behandlerRef = behandlerRef,
    )
    val dialogmeldingToBehandlerBestilling = dialogmeldingToBehandlerBestillingDTO.toDialogmeldingToBehandlerBestilling(behandler)

    val bestillingId = database.connection.use { connection ->
        connection.createBehandlerDialogmeldingBestilling(
            dialogmeldingToBehandlerBestilling = dialogmeldingToBehandlerBestilling,
            behandlerId = behandlerId,
        ).also {
            connection.commit()
        }
    }
    return bestillingId
}
