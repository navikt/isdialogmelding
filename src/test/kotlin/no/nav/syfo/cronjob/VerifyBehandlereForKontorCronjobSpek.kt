package no.nav.syfo.cronjob

import io.ktor.server.testing.*
import io.mockk.clearAllMocks
import kotlinx.coroutines.runBlocking
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
import no.nav.syfo.testhelper.*
import no.nav.syfo.testhelper.UserConstants.BEHANDLER_ETTERNAVN
import no.nav.syfo.testhelper.UserConstants.BEHANDLER_FORNAVN
import no.nav.syfo.testhelper.UserConstants.FASTLEGE_ANNEN_FNR
import no.nav.syfo.testhelper.UserConstants.FASTLEGE_DNR
import no.nav.syfo.testhelper.UserConstants.FASTLEGE_FNR
import no.nav.syfo.testhelper.UserConstants.FASTLEGE_TREDJE_FNR
import no.nav.syfo.testhelper.UserConstants.FASTLEGE_UTEN_KATEGORI_FNR
import no.nav.syfo.testhelper.UserConstants.HERID
import no.nav.syfo.testhelper.UserConstants.HERID_KONTOR_OK
import no.nav.syfo.testhelper.UserConstants.HERID_NOT_ACTIVE
import no.nav.syfo.testhelper.UserConstants.HPRID
import no.nav.syfo.testhelper.UserConstants.HPRID_INACTIVE
import no.nav.syfo.testhelper.UserConstants.HPRID_UNKNOWN
import no.nav.syfo.testhelper.UserConstants.HPRID_UTEN_KATEGORI
import no.nav.syfo.testhelper.UserConstants.KONTOR_NAVN
import no.nav.syfo.testhelper.UserConstants.PARTNERID
import org.amshove.kluent.*
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.OffsetDateTime
import java.util.*

class VerifyBehandlereForKontorCronjobSpek : Spek({
    describe(VerifyBehandlereForKontorCronjobSpek::class.java.simpleName) {
        with(TestApplicationEngine()) {
            start()
            val externalMockEnvironment = ExternalMockEnvironment.instance
            val database = externalMockEnvironment.database
            val environment = externalMockEnvironment.environment
            val azureAdClient = AzureAdClient(
                azureAppClientId = environment.aadAppClient,
                azureAppClientSecret = environment.azureAppClientSecret,
                azureOpenidConfigTokenEndpoint = environment.azureOpenidConfigTokenEndpoint,
                httpClient = externalMockEnvironment.mockHttpClient,
            )
            val syfohelsenettproxyClient = SyfohelsenettproxyClient(
                azureAdClient = azureAdClient,
                endpointUrl = environment.syfohelsenettproxyUrl,
                endpointClientId = environment.syfohelsenettproxyClientId,
                httpClient = externalMockEnvironment.mockHttpClient,
            )
            val fastlegeClient = FastlegeClient(
                azureAdClient = azureAdClient,
                fastlegeRestClientId = environment.fastlegeRestClientId,
                fastlegeRestUrl = environment.fastlegeRestUrl,
                httpClient = externalMockEnvironment.mockHttpClient,
            )
            val behandlerService = BehandlerService(
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
            val cronJob = VerifyBehandlereForKontorCronjob(
                behandlerService = behandlerService,
                fastlegeClient = fastlegeClient,
                syfohelsenettproxyClient = syfohelsenettproxyClient,
            )
            beforeEachTest {
                database.dropData()
                clearAllMocks()
            }

            describe("Cronjob sjekker eksisterende behandlerkontor") {
                it("Cronjob virker selv om ingen kontor") {
                    runBlocking {
                        cronJob.verifyBehandlereForKontorJob()
                    }
                }
                it("Cronjob disabler kontor som ikke lengre er aktivt") {
                    val kontorId = createKontor(HERID_NOT_ACTIVE)
                    val kontorBefore = database.getBehandlerKontorById(kontorId)
                    kontorBefore.dialogmeldingEnabled shouldNotBeEqualTo null
                    runBlocking {
                        cronJob.verifyBehandlereForKontorJob()
                    }
                    val kontorAfter = database.getBehandlerKontorById(kontorId)
                    kontorAfter.dialogmeldingEnabled shouldBeEqualTo null
                }
                it("Cronjob endrer ikke kontorets aktivflagg hvis fortsatt aktivt ") {
                    val kontorId = createKontor(HERID_KONTOR_OK)
                    runBlocking {
                        cronJob.verifyBehandlereForKontorJob()
                    }
                    val kontorAfter = database.getBehandlerKontorById(kontorId)
                    kontorAfter.dialogmeldingEnabled shouldNotBeEqualTo null
                }
                it("Cronjob setter hprId på behandlere som mangler det") {
                    val kontorId = createKontor(HERID_KONTOR_OK)
                    val pBehandler = createBehandler(
                        kontorId = kontorId,
                        hprId = null,
                        personident = FASTLEGE_FNR,
                    )
                    runBlocking {
                        cronJob.verifyBehandlereForKontorJob()
                    }
                    val behandlerAfter = database.getBehandlerById(pBehandler.id)
                    behandlerAfter!!.hprId shouldBeEqualTo HPRID.toString()
                }
                it("Cronjob invaliderer behandler som ikke finnes for kontor i Adresseregisteret") {
                    val kontorId = createKontor(HERID_KONTOR_OK)
                    val pBehandler = createBehandler(kontorId, HPRID_UNKNOWN)
                    val behandlerBefore = database.getBehandlerById(pBehandler.id)
                    behandlerBefore!!.invalidated shouldBe null
                    runBlocking {
                        cronJob.verifyBehandlereForKontorJob()
                    }
                    val behandlerAfter = database.getBehandlerById(pBehandler.id)
                    behandlerAfter!!.invalidated shouldNotBe null
                }
                it("Cronjob invaliderer ikke behandler knyttet til Aleris") {
                    val kontorId = createKontor(ALERIS_HER_ID.toInt())
                    val pBehandler = createBehandler(kontorId, HPRID_UNKNOWN)
                    val behandlerBefore = database.getBehandlerById(pBehandler.id)
                    behandlerBefore!!.invalidated shouldBe null
                    runBlocking {
                        cronJob.verifyBehandlereForKontorJob()
                    }
                    val behandlerAfter = database.getBehandlerById(pBehandler.id)
                    behandlerAfter!!.invalidated shouldBe null
                }
                it("Cronjob invaliderer behandler som er inaktiv i Adresseregisteret") {
                    val kontorId = createKontor(HERID_KONTOR_OK)
                    val pBehandler = createBehandler(kontorId)
                    val behandlerBefore = database.getBehandlerById(pBehandler.id)
                    behandlerBefore!!.invalidated shouldBe null
                    runBlocking {
                        cronJob.verifyBehandlereForKontorJob()
                    }
                    val behandlerAfter = database.getBehandlerById(pBehandler.id)
                    behandlerAfter!!.invalidated shouldNotBe null
                }
                it("Cronjob revaliderer behandler som er aktiv i Adresseregisteret") {
                    val kontorId = createKontor(HERID_KONTOR_OK)
                    val pBehandler = createBehandler(kontorId, HPRID)
                    database.invalidateBehandler(pBehandler.behandlerRef)
                    runBlocking {
                        cronJob.verifyBehandlereForKontorJob()
                    }
                    val behandlerAfter = database.getBehandlerById(pBehandler.id)
                    behandlerAfter!!.invalidated shouldBe null
                }
                it("Cronjob revaliderer ikke duplikat") {
                    val kontorId = createKontor(HERID_KONTOR_OK)
                    val pBehandlerInaktiv = createBehandler(kontorId, HPRID)
                    database.invalidateBehandler(pBehandlerInaktiv.behandlerRef)
                    val pBehandlerAktiv = createBehandler(kontorId, HPRID)
                    runBlocking {
                        cronJob.verifyBehandlereForKontorJob()
                    }
                    val behandlerInaktivAfter = database.getBehandlerById(pBehandlerInaktiv.id)
                    behandlerInaktivAfter!!.invalidated shouldNotBe null
                    val behandlerAktivAfter = database.getBehandlerById(pBehandlerAktiv.id)
                    behandlerAktivAfter!!.invalidated shouldBe null
                }
                it("Cronjob legger til ny behandlere") {
                    val kontorId = createKontor(HERID_KONTOR_OK)
                    val behandlerBefore = database.getBehandlereForKontor(kontorId)
                    behandlerBefore.size shouldBeEqualTo 0
                    runBlocking {
                        cronJob.verifyBehandlereForKontorJob()
                    }
                    val behandlerAfter = database.getBehandlereForKontor(kontorId)
                    behandlerAfter.size shouldBeEqualTo 2
                }
                it("Cronjob oppdaterer eksisterende behandler") {
                    val kontorId = createKontor(HERID_KONTOR_OK)
                    val pBehandler = createBehandler(
                        kontorId = kontorId,
                        hprId = HPRID,
                        personident = FASTLEGE_FNR,
                        fornavn = "for",
                        etternavn = "etter",
                        kategori = BehandlerKategori.TANNLEGE,
                    )
                    runBlocking {
                        cronJob.verifyBehandlereForKontorJob()
                    }
                    val behandlerAfter = database.getBehandlereForKontor(kontorId)
                    behandlerAfter.size shouldBeEqualTo 2
                    val pBehandlerAfter = behandlerAfter.first { it.hprId == HPRID.toString() }
                    pBehandlerAfter.id shouldBeEqualTo pBehandler.id
                    pBehandlerAfter.behandlerRef shouldBeEqualTo pBehandler.behandlerRef
                    pBehandlerAfter.fornavn shouldBeEqualTo BEHANDLER_FORNAVN
                    pBehandlerAfter.etternavn shouldBeEqualTo BEHANDLER_ETTERNAVN
                    pBehandlerAfter.herId shouldBeEqualTo HERID.toString()
                    pBehandlerAfter.kategori shouldBeEqualTo BehandlerKategori.LEGE.name
                }
                it("Cronjob oppdaterer eksisterende behandler selv om kategori mangler i Adresseregisteret") {
                    val kontorId = createKontor(HERID_KONTOR_OK)
                    val pBehandler = createBehandler(
                        kontorId = kontorId,
                        hprId = HPRID_UTEN_KATEGORI,
                        personident = FASTLEGE_UTEN_KATEGORI_FNR,
                        fornavn = "for",
                        etternavn = "etter",
                        kategori = BehandlerKategori.TANNLEGE,
                    )
                    runBlocking {
                        cronJob.verifyBehandlereForKontorJob()
                    }
                    val behandlerAfter = database.getBehandlereForKontor(kontorId)
                    behandlerAfter.size shouldBeEqualTo 2
                    val pBehandlerAfter = behandlerAfter.first { it.hprId == HPRID_UTEN_KATEGORI.toString() }
                    pBehandlerAfter.id shouldBeEqualTo pBehandler.id
                    pBehandlerAfter.behandlerRef shouldBeEqualTo pBehandler.behandlerRef
                    pBehandlerAfter.fornavn shouldBeEqualTo BEHANDLER_FORNAVN
                    pBehandlerAfter.etternavn shouldBeEqualTo BEHANDLER_ETTERNAVN
                    pBehandlerAfter.herId shouldBeEqualTo HERID.toString()
                    pBehandlerAfter.kategori shouldBeEqualTo BehandlerKategori.TANNLEGE.name
                }
                it("Cronjob oppdaterer eksisterende behandler med DNR der forekomsten i Adresseregisteret har FNR") {
                    val kontorId = createKontor(HERID_KONTOR_OK)
                    val pBehandler = createBehandler(
                        kontorId = kontorId,
                        hprId = HPRID,
                        personident = FASTLEGE_DNR,
                        fornavn = "for",
                        etternavn = "etter",
                        kategori = BehandlerKategori.LEGE,
                    )
                    runBlocking {
                        cronJob.verifyBehandlereForKontorJob()
                    }
                    val behandlerAfter = database.getBehandlereForKontor(kontorId)
                    behandlerAfter.size shouldBeEqualTo 2
                    val pBehandlerAfter = behandlerAfter.first { it.hprId == HPRID.toString() }
                    pBehandlerAfter.id shouldBeEqualTo pBehandler.id
                    pBehandlerAfter.personident shouldBeEqualTo FASTLEGE_FNR.value
                    pBehandlerAfter.behandlerRef shouldBeEqualTo pBehandler.behandlerRef
                    pBehandlerAfter.fornavn shouldBeEqualTo BEHANDLER_FORNAVN
                    pBehandlerAfter.etternavn shouldBeEqualTo BEHANDLER_ETTERNAVN
                    pBehandlerAfter.herId shouldBeEqualTo HERID.toString()
                    pBehandlerAfter.kategori shouldBeEqualTo BehandlerKategori.LEGE.name
                }
                it("Cronjob oppdaterer spesifikk behandler") {
                    val kontorId = createKontor(HERID_KONTOR_OK)
                    val pBehandler = createBehandler(
                        behandlerRef = UUID.fromString("3f5c938d-b16a-4474-a294-9e121e7efd17"),
                        kontorId = kontorId,
                        hprId = HPRID,
                        personident = FASTLEGE_TREDJE_FNR,
                        fornavn = "for",
                        etternavn = "etter",
                        kategori = BehandlerKategori.LEGE,
                    )
                    runBlocking {
                        cronJob.verifyBehandlereForKontorJob()
                    }
                    val behandlerAfter = database.getBehandlereForKontor(kontorId)
                    behandlerAfter.size shouldBeEqualTo 2
                    val pBehandlerAfter = behandlerAfter.first { it.hprId == HPRID.toString() }
                    pBehandlerAfter.id shouldBeEqualTo pBehandler.id
                    pBehandlerAfter.personident shouldBeEqualTo FASTLEGE_FNR.value
                    pBehandlerAfter.behandlerRef shouldBeEqualTo pBehandler.behandlerRef
                    pBehandlerAfter.fornavn shouldBeEqualTo BEHANDLER_FORNAVN
                    pBehandlerAfter.etternavn shouldBeEqualTo BEHANDLER_ETTERNAVN
                    pBehandlerAfter.herId shouldBeEqualTo HERID.toString()
                    pBehandlerAfter.kategori shouldBeEqualTo BehandlerKategori.LEGE.name
                }
                it("Cronjob legger til ny behandler og invaliderer eksisterende") {
                    val kontorId = createKontor(HERID_KONTOR_OK)
                    val pBehandler = createBehandler(kontorId)
                    runBlocking {
                        cronJob.verifyBehandlereForKontorJob()
                    }
                    val behandlerForKontorAfter = database.getBehandlereForKontor(kontorId)
                    behandlerForKontorAfter.size shouldBeEqualTo 3
                    val behandlerAfter = database.getBehandlerById(pBehandler.id)
                    behandlerAfter!!.invalidated shouldNotBe null
                }
                it("Cronjob invaliderer duplikater") {
                    val kontorId = createKontor(HERID_KONTOR_OK)
                    val pBehandler1 = createBehandler(kontorId = kontorId, hprId = HPRID, personident = FASTLEGE_FNR)
                    val pBehandler2 = createBehandler(kontorId, HPRID)
                    runBlocking {
                        cronJob.verifyBehandlereForKontorJob()
                    }
                    val behandlerForKontorAfter = database.getBehandlereForKontor(kontorId)
                    behandlerForKontorAfter.size shouldBeEqualTo 3
                    val behandler1After = database.getBehandlerById(pBehandler1.id)
                    behandler1After!!.invalidated shouldBe null
                    val behandler2After = database.getBehandlerById(pBehandler2.id)
                    behandler2After!!.invalidated shouldNotBe null
                }
                it("Cronjob invaliderer duplikat med D-nummer") {
                    val kontorId = createKontor(HERID_KONTOR_OK)
                    val pBehandler1 = createBehandler(kontorId = kontorId, hprId = HPRID, personident = FASTLEGE_DNR)
                    val pBehandler2 = createBehandler(kontorId = kontorId, hprId = HPRID, personident = FASTLEGE_FNR)
                    runBlocking {
                        cronJob.verifyBehandlereForKontorJob()
                    }
                    val behandlerForKontorAfter = database.getBehandlereForKontor(kontorId)
                    behandlerForKontorAfter.size shouldBeEqualTo 3
                    val behandler1After = database.getBehandlerById(pBehandler1.id)
                    behandler1After!!.invalidated shouldNotBe null
                    val behandler2After = database.getBehandlerById(pBehandler2.id)
                    behandler2After!!.invalidated shouldBe null
                }
            }
        }
    }
})

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
