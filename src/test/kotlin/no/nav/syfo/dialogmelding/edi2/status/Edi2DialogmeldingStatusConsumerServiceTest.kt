package no.nav.syfo.dialogmelding.edi2.status

import io.mockk.clearAllMocks
import io.mockk.mockk
import no.nav.syfo.behandler.BehandlerService
import no.nav.syfo.behandler.database.getBehandlerByBehandlerRef
import no.nav.syfo.behandler.fastlege.FastlegeClient
import no.nav.syfo.behandler.partnerinfo.PartnerinfoClient
import no.nav.syfo.dialogmelding.apprec.ApprecService
import no.nav.syfo.dialogmelding.apprec.database.getApprec
import no.nav.syfo.dialogmelding.bestilling.DialogmeldingToBehandlerService
import no.nav.syfo.dialogmelding.status.database.getDialogmeldingStatusNotPublished
import no.nav.syfo.dialogmelding.status.domain.DialogmeldingStatusType
import no.nav.syfo.testhelper.ExternalMockEnvironment
import no.nav.syfo.testhelper.dropData
import no.nav.syfo.testhelper.testdata.lagreDialogmeldingBestillingOgBehandler
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets
import java.util.UUID

class Edi2DialogmeldingStatusConsumerServiceTest {
    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val database = externalMockEnvironment.database

    private val apprecService = ApprecService(
        database = database,
    )
    private val dialogmeldingToBehandlerService = DialogmeldingToBehandlerService(
        database = database,
        pdlClient = mockk(),
    )
    private val behandlerService = BehandlerService(
        database = database,
        fastlegeClient = mockk<FastlegeClient>(relaxed = true),
        partnerinfoClient = mockk<PartnerinfoClient>(relaxed = true),
    )
    private val edi2DialogmeldingStatusConsumerService = Edi2DialogmeldingStatusConsumerService(
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
    fun `COMPLETED status is handled as OK apprec and enables kontor`() {
        val dialogmeldingBestillingUuid = UUID.randomUUID()
        val (bestillingId, behandler) = lagreDialogmeldingBestillingOgBehandler(
            database = database,
            dialogmeldingBestillingUuid = dialogmeldingBestillingUuid,
            behandlerKontorEnabled = false,
            behandlerKontorLocked = false,
        )

        edi2DialogmeldingStatusConsumerService.handleIncomingStatus(
            KafkaDialogmeldingStatusFromEdi2DTO(
                messageId = dialogmeldingBestillingUuid.toString(),
                timestamp = null,
                status = "COMPLETED",
                apprec = KafkaDialogmeldingStatusFromEdi2ApprecDTO(
                    receiverHerId = null,
                    status = "OK",
                    errorList = emptyList(),
                ),
                error = null,
            )
        )

        val pApprec = database.getApprec(
            UUID.nameUUIDFromBytes("edi2:$dialogmeldingBestillingUuid:COMPLETED".toByteArray(StandardCharsets.UTF_8))
        )
        assertNotNull(pApprec)
        assertEquals(bestillingId, pApprec!!.bestillingId)
        assertEquals("1", pApprec.statusKode)
        assertEquals("OK", pApprec.statusTekst)

        val dialogmeldingStatusNotPublished = database.getDialogmeldingStatusNotPublished()
        assertEquals(1, dialogmeldingStatusNotPublished.size)
        assertEquals(DialogmeldingStatusType.OK.name, dialogmeldingStatusNotPublished.first().status)

        val oppdatertBehandler = database.getBehandlerByBehandlerRef(behandler.behandlerRef)!!
        assertNotNull(oppdatertBehandler)
        assertTrue(oppdatertBehandler.kontorId > 0)
        val behandlerDomain = behandlerService.getBehandler(behandler.behandlerRef)!!
        assertTrue(behandlerDomain.kontor.dialogmeldingEnabled)
    }

    @Test
    fun `REJECTED_APPREC status is handled as AVVIST apprec`() {
        val dialogmeldingBestillingUuid = UUID.randomUUID()
        val (bestillingId, _) = lagreDialogmeldingBestillingOgBehandler(database, dialogmeldingBestillingUuid)

        edi2DialogmeldingStatusConsumerService.handleIncomingStatus(
            KafkaDialogmeldingStatusFromEdi2DTO(
                messageId = dialogmeldingBestillingUuid.toString(),
                timestamp = null,
                status = "REJECTED_APPREC",
                apprec = KafkaDialogmeldingStatusFromEdi2ApprecDTO(
                    receiverHerId = null,
                    status = "REJECTED",
                    errorList = listOf(
                        KafkaDialogmeldingStatusFromEdi2ApprecErrorDTO(
                            code = "E21",
                            description = "Mottaker finnes ikke",
                            oid = null,
                        )
                    ),
                ),
                error = null,
            )
        )

        val pApprec = database.getApprec(
            UUID.nameUUIDFromBytes("edi2:$dialogmeldingBestillingUuid:REJECTED_APPREC".toByteArray(StandardCharsets.UTF_8))
        )
        assertNotNull(pApprec)
        assertEquals(bestillingId, pApprec!!.bestillingId)
        assertEquals("2", pApprec.statusKode)
        assertEquals("E21", pApprec.feilKode)
        assertEquals("Mottaker finnes ikke", pApprec.feilTekst)

        val dialogmeldingStatusNotPublished = database.getDialogmeldingStatusNotPublished()
        assertEquals(1, dialogmeldingStatusNotPublished.size)
        assertEquals(DialogmeldingStatusType.AVVIST.name, dialogmeldingStatusNotPublished.first().status)
        assertEquals("Mottaker finnes ikke", dialogmeldingStatusNotPublished.first().tekst)
    }

    @Test
    fun `Non-terminal status is ignored`() {
        val dialogmeldingBestillingUuid = UUID.randomUUID()
        lagreDialogmeldingBestillingOgBehandler(database, dialogmeldingBestillingUuid)

        edi2DialogmeldingStatusConsumerService.handleIncomingStatus(
            KafkaDialogmeldingStatusFromEdi2DTO(
                messageId = dialogmeldingBestillingUuid.toString(),
                timestamp = null,
                status = "PENDING_APPREC",
                apprec = null,
                error = null,
            )
        )

        assertNull(
            database.getApprec(
                UUID.nameUUIDFromBytes("edi2:$dialogmeldingBestillingUuid:PENDING_APPREC".toByteArray(StandardCharsets.UTF_8))
            )
        )
        assertTrue(database.getDialogmeldingStatusNotPublished().isEmpty())
    }
}
