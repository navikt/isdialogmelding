package no.nav.syfo.cronjob

import io.mockk.clearAllMocks
import kotlinx.coroutines.test.runTest
import no.nav.syfo.behandler.BehandlerService
import no.nav.syfo.behandler.database.*
import no.nav.syfo.behandler.database.domain.toBehandlerKontor
import no.nav.syfo.behandler.domain.Behandler
import no.nav.syfo.behandler.domain.BehandlerKategori
import no.nav.syfo.behandler.fastlege.FastlegeClient
import no.nav.syfo.behandler.partnerinfo.PartnerinfoClient
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.syfohelsenettproxy.SyfohelsenettproxyClient
import no.nav.syfo.dialogmelding.apprec.database
import no.nav.syfo.domain.Personident
import no.nav.syfo.testhelper.ExternalMockEnvironment
import no.nav.syfo.testhelper.UserConstants.BEHANDLER_ETTERNAVN
import no.nav.syfo.testhelper.UserConstants.BEHANDLER_FORNAVN
import no.nav.syfo.testhelper.UserConstants.FASTLEGE_DNR
import no.nav.syfo.testhelper.UserConstants.FASTLEGE_FNR
import no.nav.syfo.testhelper.UserConstants.FASTLEGE_TREDJE_FNR
import no.nav.syfo.testhelper.UserConstants.FASTLEGE_UTEN_KATEGORI_FNR
import no.nav.syfo.testhelper.UserConstants.HERID
import no.nav.syfo.testhelper.UserConstants.HERID_KONTOR_OK
import no.nav.syfo.testhelper.UserConstants.HERID_NOT_ACTIVE
import no.nav.syfo.testhelper.UserConstants.HERID_PSYKOLOG_KONTOR_OK
import no.nav.syfo.testhelper.UserConstants.HPRID
import no.nav.syfo.testhelper.UserConstants.HPRID_INACTIVE
import no.nav.syfo.testhelper.UserConstants.HPRID_UNKNOWN
import no.nav.syfo.testhelper.UserConstants.HPRID_UTEN_KATEGORI
import no.nav.syfo.testhelper.UserConstants.KONTOR_NAVN
import no.nav.syfo.testhelper.UserConstants.PARTNERID
import no.nav.syfo.testhelper.createKontor
import no.nav.syfo.testhelper.dropData
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.*

class VerifyBehandlereForKontorCronjobTest {
    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val database = externalMockEnvironment.database
    private val environment = externalMockEnvironment.environment
    private val azureAdClient = AzureAdClient(
        azureAppClientId = environment.aadAppClient,
        azureAppClientSecret = environment.azureAppClientSecret,
        azureOpenidConfigTokenEndpoint = environment.azureOpenidConfigTokenEndpoint,
        httpClient = externalMockEnvironment.mockHttpClient,
    )
    private val syfohelsenettproxyClient = SyfohelsenettproxyClient(
        azureAdClient = azureAdClient,
        endpointUrl = environment.syfohelsenettproxyUrl,
        endpointClientId = environment.syfohelsenettproxyClientId,
        httpClient = externalMockEnvironment.mockHttpClient,
    )
    private val fastlegeClient = FastlegeClient(
        azureAdClient = azureAdClient,
        fastlegeRestClientId = environment.fastlegeRestClientId,
        fastlegeRestUrl = environment.fastlegeRestUrl,
        httpClient = externalMockEnvironment.mockHttpClient,
    )
    private val behandlerService = BehandlerService(
        fastlegeClient = FastlegeClient(
            azureAdClient = azureAdClient,
            fastlegeRestClientId = environment.fastlegeRestClientId,
            fastlegeRestUrl = environment.fastlegeRestUrl,
            httpClient = externalMockEnvironment.mockHttpClient,
        ),
        partnerinfoClient = PartnerinfoClient(
            azureAdClient = azureAdClient,
            syfoPartnerinfoClientId = environment.syfoPartnerinfoClientId,
            syfoPartnerinfoUrl = environment.syfoPartnerinfoUrl,
            httpClient = externalMockEnvironment.mockHttpClient,
        ),
        database = database,
    )
    private val behandlerToBeUpdated = UUID.randomUUID()
    private val cronJob = VerifyBehandlereForKontorCronjob(
        behandlerService = behandlerService,
        fastlegeClient = fastlegeClient,
        syfohelsenettproxyClient = syfohelsenettproxyClient,
        behandlerToBeUpdated = listOf(behandlerToBeUpdated),
    )

    @BeforeEach
    fun beforeEach() {
        database.dropData()
        clearAllMocks()
    }

    @Test
    fun `Cronjob virker selv om ingen kontor`() = runTest {
        cronJob.verifyBehandlereForKontorJob()
    }

    @Test
    fun `Cronjob disabler kontor som ikke lengre er aktivt`() = runTest {
        val kontorId = createKontor(HERID_NOT_ACTIVE)
        val kontorBefore = database.getBehandlerKontorById(kontorId)
        assertNotNull(kontorBefore.dialogmeldingEnabled)
        cronJob.verifyBehandlereForKontorJob()
        val kontorAfter = database.getBehandlerKontorById(kontorId)
        assertNull(kontorAfter.dialogmeldingEnabled)
    }

    @Test
    fun `Cronjob endrer ikke kontorets aktivflagg hvis fortsatt aktivt`() = runTest {
        val kontorId = createKontor(HERID_KONTOR_OK)
        cronJob.verifyBehandlereForKontorJob()
        val kontorAfter = database.getBehandlerKontorById(kontorId)
        assertNotNull(kontorAfter.dialogmeldingEnabled)
    }

    @Test
    fun `Cronjob oppdaterer adresse`() = runTest {
        val kontorId = createKontor(HERID_KONTOR_OK)
        val kontorBefore = database.getBehandlerKontorById(kontorId)
        assertNull(kontorBefore.adresse)
        assertNull(kontorBefore.postnummer)
        assertNull(kontorBefore.poststed)
        cronJob.verifyBehandlereForKontorJob()
        val kontorAfter = database.getBehandlerKontorById(kontorId)
        assertNotNull(kontorAfter.adresse)
        assertNotNull(kontorAfter.postnummer)
        assertNotNull(kontorAfter.poststed)
    }

    @Test
    fun `Cronjob setter hprId på behandlere som mangler det`() = runTest {
        val kontorId = createKontor(HERID_KONTOR_OK)
        val pBehandler = createBehandler(
            kontorId = kontorId,
            hprId = null,
            personident = FASTLEGE_FNR,
        )
        cronJob.verifyBehandlereForKontorJob()
        val behandlerAfter = database.getBehandlerById(pBehandler.id)
        assertEquals(HPRID.toString(), behandlerAfter!!.hprId)
    }

    @Test
    fun `Cronjob invaliderer behandler som ikke finnes for kontor i Adresseregisteret`() = runTest {
        val kontorId = createKontor(HERID_KONTOR_OK)
        val pBehandler = createBehandler(kontorId, HPRID_UNKNOWN)
        val behandlerBefore = database.getBehandlerById(pBehandler.id)
        assertNull(behandlerBefore!!.invalidated)
        cronJob.verifyBehandlereForKontorJob()
        val behandlerAfter = database.getBehandlerById(pBehandler.id)
        assertNotNull(behandlerAfter!!.invalidated)
    }

    @Test
    fun `Cronjob invaliderer ikke behandler knyttet til Aleris`() = runTest {
        val kontorId = createKontor(ALERIS_HER_ID.toInt())
        val pBehandler = createBehandler(kontorId, HPRID_UNKNOWN)
        val behandlerBefore = database.getBehandlerById(pBehandler.id)
        assertNull(behandlerBefore!!.invalidated)
        cronJob.verifyBehandlereForKontorJob()
        val behandlerAfter = database.getBehandlerById(pBehandler.id)
        assertNull(behandlerAfter!!.invalidated)
    }

    @Test
    fun `Cronjob invaliderer behandler som er inaktiv i Adresseregisteret`() = runTest {
        val kontorId = createKontor(HERID_KONTOR_OK)
        val pBehandler = createBehandler(kontorId)
        val behandlerBefore = database.getBehandlerById(pBehandler.id)
        assertNull(behandlerBefore!!.invalidated)
        cronJob.verifyBehandlereForKontorJob()
        val behandlerAfter = database.getBehandlerById(pBehandler.id)
        assertNotNull(behandlerAfter!!.invalidated)
    }

    @Test
    fun `Cronjob revaliderer behandler som er aktiv i Adresseregisteret`() = runTest {
        val kontorId = createKontor(HERID_KONTOR_OK)
        val pBehandler = createBehandler(kontorId, HPRID)
        database.invalidateBehandler(pBehandler.behandlerRef)
        cronJob.verifyBehandlereForKontorJob()
        val behandlerAfter = database.getBehandlerById(pBehandler.id)
        assertNull(behandlerAfter!!.invalidated)
    }

    @Test
    fun `Cronjob revaliderer ikke duplikat`() = runTest {
        val kontorId = createKontor(HERID_KONTOR_OK)
        val pBehandlerInaktiv = createBehandler(kontorId, HPRID)
        database.invalidateBehandler(pBehandlerInaktiv.behandlerRef)
        val pBehandlerAktiv = createBehandler(kontorId, HPRID)
        cronJob.verifyBehandlereForKontorJob()
        val behandlerInaktivAfter = database.getBehandlerById(pBehandlerInaktiv.id)
        assertNotNull(behandlerInaktivAfter!!.invalidated)
        val behandlerAktivAfter = database.getBehandlerById(pBehandlerAktiv.id)
        assertNull(behandlerAktivAfter!!.invalidated)
    }

    @Test
    fun `Cronjob legger til ny behandlere`() = runTest {
        val kontorId = createKontor(HERID_KONTOR_OK)
        val behandlerBefore = database.getBehandlereForKontor(kontorId)
        assertEquals(0, behandlerBefore.size)
        cronJob.verifyBehandlereForKontorJob()
        val behandlerAfter = database.getBehandlereForKontor(kontorId)
        assertEquals(2, behandlerAfter.size)
    }

    @Test
    fun `Cronjob legger til ny behandlere (psykolog)`() = runTest {
        val kontorId = createKontor(HERID_PSYKOLOG_KONTOR_OK)
        val behandlerBefore = database.getBehandlereForKontor(kontorId)
        assertEquals(0, behandlerBefore.size)
        cronJob.verifyBehandlereForKontorJob()
        val behandlerAfter = database.getBehandlereForKontor(kontorId)
        assertEquals(2, behandlerAfter.size)
    }

    @Test
    fun `Cronjob oppdaterer eksisterende behandler`() = runTest {
        val kontorId = createKontor(HERID_KONTOR_OK)
        val pBehandler = createBehandler(
            kontorId = kontorId,
            hprId = HPRID,
            personident = FASTLEGE_FNR,
            fornavn = "for",
            etternavn = "etter",
            kategori = BehandlerKategori.TANNLEGE,
        )
        cronJob.verifyBehandlereForKontorJob()
        val behandlerAfter = database.getBehandlereForKontor(kontorId)
        assertEquals(2, behandlerAfter.size)
        val pBehandlerAfter = behandlerAfter.first { it.hprId == HPRID.toString() }
        assertEquals(pBehandler.id, pBehandlerAfter.id)
        assertEquals(pBehandler.behandlerRef, pBehandlerAfter.behandlerRef)
        assertEquals(BEHANDLER_FORNAVN, pBehandlerAfter.fornavn)
        assertEquals(BEHANDLER_ETTERNAVN, pBehandlerAfter.etternavn)
        assertEquals(HERID.toString(), pBehandlerAfter.herId)
        assertEquals(BehandlerKategori.LEGE.name, pBehandlerAfter.kategori)
    }

    @Test
    fun `Cronjob oppdaterer eksisterende behandler selv om kategori mangler i Adresseregisteret`() = runTest {
        val kontorId = createKontor(HERID_KONTOR_OK)
        val pBehandler = createBehandler(
            kontorId = kontorId,
            hprId = HPRID_UTEN_KATEGORI,
            personident = FASTLEGE_UTEN_KATEGORI_FNR,
            fornavn = "for",
            etternavn = "etter",
            kategori = BehandlerKategori.TANNLEGE,
        )
        cronJob.verifyBehandlereForKontorJob()
        val behandlerAfter = database.getBehandlereForKontor(kontorId)
        assertEquals(2, behandlerAfter.size)
        val pBehandlerAfter = behandlerAfter.first { it.hprId == HPRID_UTEN_KATEGORI.toString() }
        assertEquals(pBehandler.id, pBehandlerAfter.id)
        assertEquals(pBehandler.behandlerRef, pBehandlerAfter.behandlerRef)
        assertEquals(BEHANDLER_FORNAVN, pBehandlerAfter.fornavn)
        assertEquals(BEHANDLER_ETTERNAVN, pBehandlerAfter.etternavn)
        assertEquals(HERID.toString(), pBehandlerAfter.herId)
        assertEquals(BehandlerKategori.TANNLEGE.name, pBehandlerAfter.kategori)
    }

    @Test
    fun `Cronjob oppdaterer eksisterende behandler med DNR der forekomsten i Adresseregisteret har FNR`() = runTest {
        val kontorId = createKontor(HERID_KONTOR_OK)
        val pBehandler = createBehandler(
            kontorId = kontorId,
            hprId = HPRID,
            personident = FASTLEGE_DNR,
            fornavn = "for",
            etternavn = "etter",
            kategori = BehandlerKategori.LEGE,
        )
        cronJob.verifyBehandlereForKontorJob()
        val behandlerAfter = database.getBehandlereForKontor(kontorId)
        assertEquals(2, behandlerAfter.size)
        val pBehandlerAfter = behandlerAfter.first { it.hprId == HPRID.toString() }
        assertEquals(pBehandler.id, pBehandlerAfter.id)
        assertEquals(FASTLEGE_FNR.value, pBehandlerAfter.personident)
        assertEquals(pBehandler.behandlerRef, pBehandlerAfter.behandlerRef)
        assertEquals(BEHANDLER_FORNAVN, pBehandlerAfter.fornavn)
        assertEquals(BEHANDLER_ETTERNAVN, pBehandlerAfter.etternavn)
        assertEquals(HERID.toString(), pBehandlerAfter.herId)
        assertEquals(BehandlerKategori.LEGE.name, pBehandlerAfter.kategori)
    }

    @Test
    fun `Cronjob oppdaterer spesifikk behandler`() = runTest {
        val kontorId = createKontor(HERID_KONTOR_OK)
        val pBehandler = createBehandler(
            behandlerRef = behandlerToBeUpdated,
            kontorId = kontorId,
            hprId = HPRID,
            personident = FASTLEGE_TREDJE_FNR,
            fornavn = "for",
            etternavn = "etter",
            kategori = BehandlerKategori.LEGE,
        )
        cronJob.verifyBehandlereForKontorJob()
        val behandlerAfter = database.getBehandlereForKontor(kontorId)
        assertEquals(2, behandlerAfter.size)
        val pBehandlerAfter = behandlerAfter.first { it.hprId == HPRID.toString() }
        assertEquals(pBehandler.id, pBehandlerAfter.id)
        assertEquals(FASTLEGE_FNR.value, pBehandlerAfter.personident)
        assertEquals(pBehandler.behandlerRef, pBehandlerAfter.behandlerRef)
        assertEquals(BEHANDLER_FORNAVN, pBehandlerAfter.fornavn)
        assertEquals(BEHANDLER_ETTERNAVN, pBehandlerAfter.etternavn)
        assertEquals(HERID.toString(), pBehandlerAfter.herId)
        assertEquals(BehandlerKategori.LEGE.name, pBehandlerAfter.kategori)
    }

    @Test
    fun `Cronjob legger til ny behandler og invaliderer eksisterende`() = runTest {
        val kontorId = createKontor(HERID_KONTOR_OK)
        val pBehandler = createBehandler(kontorId)
        cronJob.verifyBehandlereForKontorJob()
        val behandlerForKontorAfter = database.getBehandlereForKontor(kontorId)
        assertEquals(3, behandlerForKontorAfter.size)
        val behandlerAfter = database.getBehandlerById(pBehandler.id)
        assertNotNull(behandlerAfter!!.invalidated)
    }

    @Test
    fun `Cronjob invaliderer duplikater`() = runTest {
        val kontorId = createKontor(HERID_KONTOR_OK)
        val pBehandler1 = createBehandler(kontorId = kontorId, hprId = HPRID, personident = FASTLEGE_FNR)
        val pBehandler2 = createBehandler(kontorId, HPRID)
        cronJob.verifyBehandlereForKontorJob()
        val behandlerForKontorAfter = database.getBehandlereForKontor(kontorId)
        assertEquals(3, behandlerForKontorAfter.size)
        val behandler1After = database.getBehandlerById(pBehandler1.id)
        assertNull(behandler1After!!.invalidated)
        val behandler2After = database.getBehandlerById(pBehandler2.id)
        assertNotNull(behandler2After!!.invalidated)
    }

    @Test
    fun `Cronjob invaliderer duplikat med D-nummer`() = runTest {
        val kontorId = createKontor(HERID_KONTOR_OK)
        val pBehandler1 = createBehandler(kontorId = kontorId, hprId = HPRID, personident = FASTLEGE_DNR)
        val pBehandler2 = createBehandler(kontorId = kontorId, hprId = HPRID, personident = FASTLEGE_FNR)
        cronJob.verifyBehandlereForKontorJob()
        val behandlerForKontorAfter = database.getBehandlereForKontor(kontorId)
        assertEquals(3, behandlerForKontorAfter.size)
        val behandler1After = database.getBehandlerById(pBehandler1.id)
        assertNotNull(behandler1After!!.invalidated)
        val behandler2After = database.getBehandlerById(pBehandler2.id)
        assertNull(behandler2After!!.invalidated)
    }
}

private fun createKontor(herId: Int) = database.createKontor(
    partnerId = PARTNERID,
    herId = herId,
    navn = KONTOR_NAVN,
)

private fun createBehandler(
    kontorId: Int,
    hprId: Int? = HPRID_INACTIVE,
    herId: Int? = HERID,
    personident: Personident? = null,
    fornavn: String = BEHANDLER_FORNAVN,
    etternavn: String = BEHANDLER_ETTERNAVN,
    kategori: BehandlerKategori = BehandlerKategori.LEGE,
    behandlerRef: UUID = UUID.randomUUID(),
) = database.createBehandler(
    behandler = Behandler(
        behandlerRef = behandlerRef,
        fornavn = fornavn,
        mellomnavn = null,
        etternavn = etternavn,
        telefon = null,
        personident = personident,
        herId = herId,
        hprId = hprId,
        kontor = database.getBehandlerKontorById(kontorId).toBehandlerKontor(),
        kategori = kategori,
        mottatt = OffsetDateTime.now(),
        suspendert = false,
    ),
    kontorId = kontorId,
)
