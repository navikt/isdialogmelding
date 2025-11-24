package no.nav.syfo.dialogmelding.apprec

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.syfo.behandler.BehandlerService
import no.nav.syfo.behandler.database.getBehandlerByBehandlerRef
import no.nav.syfo.behandler.fastlege.FastlegeClient
import no.nav.syfo.behandler.partnerinfo.PartnerinfoClient
import no.nav.syfo.dialogmelding.apprec.consumer.ApprecConsumer
import no.nav.syfo.dialogmelding.apprec.database.getApprec
import no.nav.syfo.dialogmelding.bestilling.DialogmeldingToBehandlerService
import no.nav.syfo.dialogmelding.status.database.getDialogmeldingStatusNotPublished
import no.nav.syfo.dialogmelding.status.domain.DialogmeldingStatusType
import no.nav.syfo.testhelper.ExternalMockEnvironment
import no.nav.syfo.testhelper.dropData
import no.nav.syfo.testhelper.getFileAsString
import no.nav.syfo.testhelper.testdata.lagreDialogmeldingBestillingOgBehandler
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*
import javax.jms.TextMessage

val externalMockEnvironment = ExternalMockEnvironment.instance
val database = externalMockEnvironment.database

class ApprecConsumerTest {
    private val incomingMessage = mockk<TextMessage>(relaxed = true)

    private val apprecService = ApprecService(
        database = database,
    )
    private val dialogmeldingToBehandlerService =
        DialogmeldingToBehandlerService(
            database = database,
            pdlClient = mockk(),
        )
    private val behandlerService = BehandlerService(
        database = database,
        fastlegeClient = mockk<FastlegeClient>(relaxed = true),
        partnerinfoClient = mockk<PartnerinfoClient>(relaxed = true),
    )
    private val apprecConsumer = ApprecConsumer(
        applicationState = externalMockEnvironment.applicationState,
        database = database,
        inputconsumer = mockk(),
        apprecService = apprecService,
        dialogmeldingToBehandlerService = dialogmeldingToBehandlerService,
        behandlerService = behandlerService,
    )

    @BeforeEach
    fun beforeEach() {
        database.dropData()
        clearAllMocks()
    }

    @Test
    fun `Prosesserer innkommet melding (melding ok)`() {
        val apprecId = UUID.randomUUID()
        val dialogmeldingBestillingUuid = UUID.randomUUID()
        val (bestillingId) = lagreDialogmeldingBestillingOgBehandler(database, dialogmeldingBestillingUuid)
        val apprecXml =
            getFileAsString("src/test/resources/apprecOK.xml")
                .replace("FiktivTestdata0001", apprecId.toString())
                .replace("b62016eb-6c2d-417a-8ecc-157b3c5ee2ca", dialogmeldingBestillingUuid.toString())
        every { incomingMessage.text } returns (apprecXml)
        apprecConsumer.processApprecMessage(incomingMessage)

        val pApprec = database.getApprec(apprecId)

        assertNotNull(pApprec)
        assertEquals(apprecId, pApprec!!.uuid)
        assertEquals(bestillingId, pApprec.bestillingId)
        assertEquals("1", pApprec.statusKode)
        assertEquals("OK", pApprec.statusTekst)
        assertNull(pApprec.feilKode)
        assertNull(pApprec.feilTekst)
    }

    @Test
    fun `Prosesserer innkommet melding (melding ok, med duplikat)`() {
        val apprecId = UUID.randomUUID()
        val dialogmeldingBestillingUuid = UUID.randomUUID()
        lagreDialogmeldingBestillingOgBehandler(database, dialogmeldingBestillingUuid)
        val apprecXml =
            getFileAsString("src/test/resources/apprecOK.xml")
                .replace("FiktivTestdata0001", apprecId.toString())
                .replace("b62016eb-6c2d-417a-8ecc-157b3c5ee2ca", dialogmeldingBestillingUuid.toString())
        every { incomingMessage.text } returns (apprecXml)
        apprecConsumer.processApprecMessage(incomingMessage)

        val pApprec = database.getApprec(apprecId)

        assertNotNull(pApprec)
        assertEquals(apprecId, pApprec!!.uuid)

        apprecConsumer.processApprecMessage(incomingMessage)
    }

    @Test
    fun `Prosesserer innkommet melding (melding ok, men ikke knyttet til kjent dialogmelding)`() {
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
        apprecConsumer.processApprecMessage(incomingMessage)

        val pApprec = database.getApprec(apprecId)

        assertNull(pApprec)
    }

    @Test
    fun `Prosesserer innkommet melding (melding avvist)`() {
        val apprecId = UUID.randomUUID()
        val dialogmeldingBestillingUuid = UUID.randomUUID()
        val (bestillingId, behandler) = lagreDialogmeldingBestillingOgBehandler(database, dialogmeldingBestillingUuid)
        val apprecXml =
            getFileAsString("src/test/resources/apprecError.xml")
                .replace("FiktivTestdata0001", apprecId.toString())
                .replace("b62016eb-6c2d-417a-8ecc-157b3c5ee2ca", dialogmeldingBestillingUuid.toString())
        every { incomingMessage.text } returns (apprecXml)
        apprecConsumer.processApprecMessage(incomingMessage)

        val pApprec = database.getApprec(apprecId)

        assertNotNull(pApprec)
        assertEquals(apprecId, pApprec!!.uuid)
        assertEquals(bestillingId, pApprec.bestillingId)
        assertEquals("2", pApprec.statusKode)
        assertEquals("Avvist", pApprec.statusTekst)
        assertEquals("X99", pApprec.feilKode)
        assertEquals("Annen feil", pApprec.feilTekst)

        val oppdatertBehandler = database.getBehandlerByBehandlerRef(behandler.behandlerRef)!!
        assertNull(oppdatertBehandler.invalidated)
    }

    @Test
    fun `Prosesserer innkommet melding (melding avvist, ukjent mottaker)`() {
        val apprecId = UUID.randomUUID()
        val dialogmeldingBestillingUuid = UUID.randomUUID()
        val (bestillingId, behandler) = lagreDialogmeldingBestillingOgBehandler(database, dialogmeldingBestillingUuid)
        val apprecXml =
            getFileAsString("src/test/resources/apprecErrorUkjentMottaker.xml")
                .replace("FiktivTestdata0001", apprecId.toString())
                .replace("b62016eb-6c2d-417a-8ecc-157b3c5ee2ca", dialogmeldingBestillingUuid.toString())
        every { incomingMessage.text } returns (apprecXml)
        apprecConsumer.processApprecMessage(incomingMessage)

        val pApprec = database.getApprec(apprecId)

        assertNotNull(pApprec)
        assertEquals(apprecId, pApprec!!.uuid)
        assertEquals(bestillingId, pApprec.bestillingId)
        assertEquals("2", pApprec.statusKode)
        assertEquals("Avvist", pApprec.statusTekst)
        assertEquals("E21", pApprec.feilKode)
        assertEquals("Mottaker finnes ikke", pApprec.feilTekst)

        val oppdatertBehandler = database.getBehandlerByBehandlerRef(behandler.behandlerRef)!!
        assertNotNull(oppdatertBehandler.invalidated)
    }

    @Test
    fun `Prosesserer innkommet feilformattert melding`() {
        val apprecXml = "Ikke noen apprec"
        every { incomingMessage.text } returns (apprecXml)
        apprecConsumer.processApprecMessage(incomingMessage)
        // should not get an exception
    }

    @Test
    fun `Prosessering av innkommet melding OK lagrer dialogmelding-status OK`() {
        val dialogmeldingBestillingUuid = UUID.randomUUID()
        val (bestillingId) = lagreDialogmeldingBestillingOgBehandler(database, dialogmeldingBestillingUuid)
        val apprecXml =
            getFileAsString("src/test/resources/apprecOK.xml")
                .replace("FiktivTestdata0001", UUID.randomUUID().toString())
                .replace("b62016eb-6c2d-417a-8ecc-157b3c5ee2ca", dialogmeldingBestillingUuid.toString())
        every { incomingMessage.text } returns (apprecXml)
        apprecConsumer.processApprecMessage(incomingMessage)

        val dialogmeldingStatusNotPublished = database.getDialogmeldingStatusNotPublished()
        assertEquals(1, dialogmeldingStatusNotPublished.size)

        val pDialogmeldingStatus = dialogmeldingStatusNotPublished.first()
        assertEquals(DialogmeldingStatusType.OK.name, pDialogmeldingStatus.status)
        assertNull(pDialogmeldingStatus.tekst)
        assertEquals(bestillingId, pDialogmeldingStatus.bestillingId)
        assertNotNull(pDialogmeldingStatus.createdAt)
        assertNotNull(pDialogmeldingStatus.updatedAt)
        assertNull(pDialogmeldingStatus.publishedAt)
    }

    @Test
    fun `Prosessering av innkommet melding OK lagrer setter dialogmeldingEnabled for kontor`() {
        val dialogmeldingBestillingUuid = UUID.randomUUID()
        val (bestillingId, behandler) = lagreDialogmeldingBestillingOgBehandler(
            database = database,
            dialogmeldingBestillingUuid = dialogmeldingBestillingUuid,
            behandlerKontorEnabled = false,
            behandlerKontorLocked = false,
        )
        assertFalse(behandler.kontor.dialogmeldingEnabled)

        val apprecXml =
            getFileAsString("src/test/resources/apprecOK.xml")
                .replace("FiktivTestdata0001", UUID.randomUUID().toString())
                .replace("b62016eb-6c2d-417a-8ecc-157b3c5ee2ca", dialogmeldingBestillingUuid.toString())
        every { incomingMessage.text } returns (apprecXml)
        apprecConsumer.processApprecMessage(incomingMessage)

        val dialogmeldingStatusNotPublished = database.getDialogmeldingStatusNotPublished()
        assertEquals(1, dialogmeldingStatusNotPublished.size)

        val pDialogmeldingStatus = dialogmeldingStatusNotPublished.first()
        assertEquals(DialogmeldingStatusType.OK.name, pDialogmeldingStatus.status)

        val oppdatertBehandler = behandlerService.getBehandler(behandler.behandlerRef)
        assertTrue(oppdatertBehandler!!.kontor.dialogmeldingEnabled)
    }

    @Test
    fun `Prosessering av innkommet melding OK lagrer setter ikke dialogmeldingEnabled for kontor som er locked`() {
        val dialogmeldingBestillingUuid = UUID.randomUUID()
        val (bestillingId, behandler) = lagreDialogmeldingBestillingOgBehandler(
            database = database,
            dialogmeldingBestillingUuid = dialogmeldingBestillingUuid,
            behandlerKontorEnabled = false,
            behandlerKontorLocked = true,
        )
        assertFalse(behandler.kontor.dialogmeldingEnabled)
        assertTrue(behandler.kontor.dialogmeldingEnabledLocked)

        val apprecXml =
            getFileAsString("src/test/resources/apprecOK.xml")
                .replace("FiktivTestdata0001", UUID.randomUUID().toString())
                .replace("b62016eb-6c2d-417a-8ecc-157b3c5ee2ca", dialogmeldingBestillingUuid.toString())
        every { incomingMessage.text } returns (apprecXml)
        apprecConsumer.processApprecMessage(incomingMessage)

        val dialogmeldingStatusNotPublished = database.getDialogmeldingStatusNotPublished()
        assertEquals(1, dialogmeldingStatusNotPublished.size)

        val pDialogmeldingStatus = dialogmeldingStatusNotPublished.first()
        assertEquals(DialogmeldingStatusType.OK.name, pDialogmeldingStatus.status)

        val oppdatertBehandler = behandlerService.getBehandler(behandler.behandlerRef)
        assertFalse(oppdatertBehandler!!.kontor.dialogmeldingEnabled)
    }

    @Test
    fun `Prosessering av innkommet melding Avvist lagrer dialogmelding-status Avvist`() {
        val dialogmeldingBestillingUuid = UUID.randomUUID()
        val (bestillingId) = lagreDialogmeldingBestillingOgBehandler(database, dialogmeldingBestillingUuid)
        val apprecXml =
            getFileAsString("src/test/resources/apprecError.xml")
                .replace("FiktivTestdata0001", UUID.randomUUID().toString())
                .replace("b62016eb-6c2d-417a-8ecc-157b3c5ee2ca", dialogmeldingBestillingUuid.toString())
        every { incomingMessage.text } returns (apprecXml)
        apprecConsumer.processApprecMessage(incomingMessage)

        val dialogmeldingStatusNotPublished = database.getDialogmeldingStatusNotPublished()
        assertEquals(1, dialogmeldingStatusNotPublished.size)

        val pDialogmeldingStatus = dialogmeldingStatusNotPublished.first()
        assertEquals(DialogmeldingStatusType.AVVIST.name, pDialogmeldingStatus.status)
        assertEquals("Annen feil", pDialogmeldingStatus.tekst)
        assertEquals(bestillingId, pDialogmeldingStatus.bestillingId)
        assertNotNull(pDialogmeldingStatus.createdAt)
        assertNotNull(pDialogmeldingStatus.updatedAt)
        assertNull(pDialogmeldingStatus.publishedAt)
    }
}
