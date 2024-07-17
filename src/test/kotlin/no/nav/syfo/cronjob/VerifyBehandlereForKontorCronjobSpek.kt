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
import no.nav.syfo.testhelper.*
import no.nav.syfo.testhelper.UserConstants.BEHANDLER_ETTERNAVN
import no.nav.syfo.testhelper.UserConstants.BEHANDLER_FORNAVN
import no.nav.syfo.testhelper.UserConstants.HERID
import no.nav.syfo.testhelper.UserConstants.HERID_KONTOR_OK
import no.nav.syfo.testhelper.UserConstants.HERID_NOT_ACTIVE
import no.nav.syfo.testhelper.UserConstants.HPRID
import no.nav.syfo.testhelper.UserConstants.HPRID_INACTVE
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
                it("Cronjob legger til ny behandler") {
                    val kontorId = createKontor(HERID_KONTOR_OK)
                    val behandlerBefore = database.getBehandlereForKontor(kontorId)
                    behandlerBefore.size shouldBeEqualTo 0
                    runBlocking {
                        cronJob.verifyBehandlereForKontorJob()
                    }
                    val behandlerAfter = database.getBehandlereForKontor(kontorId)
                    behandlerAfter.size shouldBeEqualTo 1
                }
                it("Cronjob legger til ny behandler og invaliderer eksisterende") {
                    val kontorId = createKontor(HERID_KONTOR_OK)
                    val pBehandler = createBehandler(kontorId)
                    runBlocking {
                        cronJob.verifyBehandlereForKontorJob()
                    }
                    val behandlerForKontorAfter = database.getBehandlereForKontor(kontorId)
                    behandlerForKontorAfter.size shouldBeEqualTo 2
                    val behandlerAfter = database.getBehandlerById(pBehandler.id)
                    behandlerAfter!!.invalidated shouldNotBe null
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
    hprId: Int = HPRID_INACTVE,
) = database.createBehandler(
    behandler = Behandler(
        behandlerRef = UUID.randomUUID(),
        fornavn = BEHANDLER_FORNAVN,
        mellomnavn = null,
        etternavn = BEHANDLER_ETTERNAVN,
        telefon = null,
        personident = null,
        herId = HERID,
        hprId = hprId,
        kontor = database.getBehandlerKontorById(kontorId).toBehandlerKontor(),
        kategori = BehandlerKategori.LEGE,
        mottatt = OffsetDateTime.now(),
        suspendert = false,
    ),
    kontorId = kontorId,
)
