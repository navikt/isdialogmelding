package no.nav.syfo.dialogmelding.apprec

import io.ktor.server.testing.*
import io.mockk.*
import kotlinx.coroutines.runBlocking
import no.nav.syfo.dialogmelding.bestilling.DialogmeldingToBehandlerService
import no.nav.syfo.behandler.database.*
import no.nav.syfo.dialogmelding.apprec.consumer.ApprecConsumer
import no.nav.syfo.dialogmelding.apprec.database.getApprec
import no.nav.syfo.dialogmelding.status.DialogmeldingStatusService
import no.nav.syfo.dialogmelding.status.database.getDialogmeldingStatusNotPublished
import no.nav.syfo.dialogmelding.status.domain.DialogmeldingStatusType
import no.nav.syfo.testhelper.*
import no.nav.syfo.testhelper.testdata.*
import org.amshove.kluent.*
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.UUID
import javax.jms.*

val externalMockEnvironment = ExternalMockEnvironment.instance
val database = externalMockEnvironment.database

class ApprecConsumerSpek : Spek({
    describe(ApprecConsumerSpek::class.java.simpleName) {
        with(TestApplicationEngine()) {
            start()
            val incomingMessage = mockk<TextMessage>(relaxed = true)

            val dialogmeldingStatusService = DialogmeldingStatusService(database = database)
            val apprecService = ApprecService(
                database = database,
                dialogmeldingStatusService = dialogmeldingStatusService
            )
            val dialogmeldingToBehandlerService =
                DialogmeldingToBehandlerService(
                    database = database,
                    pdlClient = mockk(),
                    dialogmeldingStatusService = dialogmeldingStatusService
                )
            val apprecConsumer = ApprecConsumer(
                applicationState = externalMockEnvironment.applicationState,
                database = database,
                inputconsumer = mockk(),
                apprecService = apprecService,
                dialogmeldingToBehandlerService = dialogmeldingToBehandlerService
            )

            describe("Prosesserer innkommet melding") {

                beforeEachTest {
                    database.dropData()
                    clearAllMocks()
                }
                it("Prosesserer innkommet melding (melding ok)") {
                    val apprecId = UUID.randomUUID()
                    val dialogmeldingBestillingUuid = UUID.randomUUID()
                    val (bestillingId) = lagreDialogmeldingBestillingOgBehandler(database, dialogmeldingBestillingUuid)
                    val apprecXml =
                        getFileAsString("src/test/resources/apprecOK.xml")
                            .replace("FiktivTestdata0001", apprecId.toString())
                            .replace("b62016eb-6c2d-417a-8ecc-157b3c5ee2ca", dialogmeldingBestillingUuid.toString())
                    every { incomingMessage.text } returns (apprecXml)
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
                    lagreDialogmeldingBestillingOgBehandler(database, dialogmeldingBestillingUuid)
                    val apprecXml =
                        getFileAsString("src/test/resources/apprecOK.xml")
                            .replace("FiktivTestdata0001", apprecId.toString())
                            .replace("b62016eb-6c2d-417a-8ecc-157b3c5ee2ca", dialogmeldingBestillingUuid.toString())
                    every { incomingMessage.text } returns (apprecXml)
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
                    lagreDialogmeldingBestillingOgBehandler(database, dialogmeldingBestillingUuid)
                    val ukjentDialogmeldingBestillingUuid = UUID.randomUUID()
                    val apprecXml =
                        getFileAsString("src/test/resources/apprecOK.xml")
                            .replace("FiktivTestdata0001", apprecId.toString())
                            .replace(
                                "b62016eb-6c2d-417a-8ecc-157b3c5ee2ca",
                                ukjentDialogmeldingBestillingUuid.toString()
                            )
                    every { incomingMessage.text } returns (apprecXml)
                    runBlocking {
                        apprecConsumer.processApprecMessage(incomingMessage)
                    }

                    val pApprec = database.getApprec(apprecId)

                    pApprec shouldBe null
                }
                it("Prosesserer innkommet melding (melding avvist)") {
                    val apprecId = UUID.randomUUID()
                    val dialogmeldingBestillingUuid = UUID.randomUUID()
                    val (bestillingId, behandler) = lagreDialogmeldingBestillingOgBehandler(database, dialogmeldingBestillingUuid)
                    val apprecXml =
                        getFileAsString("src/test/resources/apprecError.xml")
                            .replace("FiktivTestdata0001", apprecId.toString())
                            .replace("b62016eb-6c2d-417a-8ecc-157b3c5ee2ca", dialogmeldingBestillingUuid.toString())
                    every { incomingMessage.text } returns (apprecXml)
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

                    val oppdatertBehandler = database.getBehandlerByBehandlerRef(behandler.behandlerRef)!!
                    oppdatertBehandler.invalidated shouldBe null
                }
                it("Prosesserer innkommet melding (melding avvist, ukjent mottaker)") {
                    val apprecId = UUID.randomUUID()
                    val dialogmeldingBestillingUuid = UUID.randomUUID()
                    val (bestillingId, behandler) = lagreDialogmeldingBestillingOgBehandler(database, dialogmeldingBestillingUuid)
                    val apprecXml =
                        getFileAsString("src/test/resources/apprecErrorUkjentMottaker.xml")
                            .replace("FiktivTestdata0001", apprecId.toString())
                            .replace("b62016eb-6c2d-417a-8ecc-157b3c5ee2ca", dialogmeldingBestillingUuid.toString())
                    every { incomingMessage.text } returns (apprecXml)
                    runBlocking {
                        apprecConsumer.processApprecMessage(incomingMessage)
                    }

                    val pApprec = database.getApprec(apprecId)

                    pApprec shouldNotBe null
                    pApprec!!.uuid shouldBeEqualTo apprecId
                    pApprec.bestillingId shouldBeEqualTo bestillingId
                    pApprec.statusKode shouldBeEqualTo "2"
                    pApprec.statusTekst shouldBeEqualTo "Avvist"
                    pApprec.feilKode shouldBeEqualTo "E21"
                    pApprec.feilTekst shouldBeEqualTo "Mottaker finnes ikke"

                    val oppdatertBehandler = database.getBehandlerByBehandlerRef(behandler.behandlerRef)!!
                    oppdatertBehandler.invalidated shouldNotBe null
                }
                it("Prosesserer innkommet feilformattert melding") {
                    val apprecXml = "Ikke noen apprec"
                    every { incomingMessage.text } returns (apprecXml)
                    runBlocking {
                        apprecConsumer.processApprecMessage(incomingMessage)
                    }
                    // should not get an exception
                }
                it("Prosessering av innkommet melding OK lagrer dialogmelding-status OK") {
                    val dialogmeldingBestillingUuid = UUID.randomUUID()
                    val (bestillingId) = lagreDialogmeldingBestillingOgBehandler(database, dialogmeldingBestillingUuid)
                    val apprecXml =
                        getFileAsString("src/test/resources/apprecOK.xml")
                            .replace("FiktivTestdata0001", UUID.randomUUID().toString())
                            .replace("b62016eb-6c2d-417a-8ecc-157b3c5ee2ca", dialogmeldingBestillingUuid.toString())
                    every { incomingMessage.text } returns (apprecXml)
                    runBlocking {
                        apprecConsumer.processApprecMessage(incomingMessage)
                    }

                    val dialogmeldingStatusNotPublished = database.getDialogmeldingStatusNotPublished()
                    dialogmeldingStatusNotPublished.size shouldBeEqualTo 1

                    val pDialogmeldingStatus = dialogmeldingStatusNotPublished.first()
                    pDialogmeldingStatus.status shouldBeEqualTo DialogmeldingStatusType.OK.name
                    pDialogmeldingStatus.tekst?.shouldBeEmpty()
                    pDialogmeldingStatus.bestillingId shouldBeEqualTo bestillingId
                    pDialogmeldingStatus.createdAt.shouldNotBeNull()
                    pDialogmeldingStatus.updatedAt.shouldNotBeNull()
                    pDialogmeldingStatus.publishedAt.shouldBeNull()
                }
                it("Prosessering av innkommet melding Avvist lagrer dialogmelding-status Avvist") {
                    val dialogmeldingBestillingUuid = UUID.randomUUID()
                    val (bestillingId) = lagreDialogmeldingBestillingOgBehandler(database, dialogmeldingBestillingUuid)
                    val apprecXml =
                        getFileAsString("src/test/resources/apprecError.xml")
                            .replace("FiktivTestdata0001", UUID.randomUUID().toString())
                            .replace("b62016eb-6c2d-417a-8ecc-157b3c5ee2ca", dialogmeldingBestillingUuid.toString())
                    every { incomingMessage.text } returns (apprecXml)
                    runBlocking {
                        apprecConsumer.processApprecMessage(incomingMessage)
                    }

                    val dialogmeldingStatusNotPublished = database.getDialogmeldingStatusNotPublished()
                    dialogmeldingStatusNotPublished.size shouldBeEqualTo 1

                    val pDialogmeldingStatus = dialogmeldingStatusNotPublished.first()
                    pDialogmeldingStatus.status shouldBeEqualTo DialogmeldingStatusType.AVVIST.name
                    pDialogmeldingStatus.tekst shouldBeEqualTo "Annen feil"
                    pDialogmeldingStatus.bestillingId shouldBeEqualTo bestillingId
                    pDialogmeldingStatus.createdAt.shouldNotBeNull()
                    pDialogmeldingStatus.updatedAt.shouldNotBeNull()
                    pDialogmeldingStatus.publishedAt.shouldBeNull()
                }
            }
        }
    }
})
