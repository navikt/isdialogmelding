package no.nav.syfo.cronjob

import io.ktor.server.testing.*
import io.mockk.clearAllMocks
import kotlinx.coroutines.runBlocking
import no.nav.syfo.behandler.BehandlerService
import no.nav.syfo.behandler.database.createBehandler
import no.nav.syfo.behandler.database.domain.toBehandlerKontor
import no.nav.syfo.behandler.database.getBehandlerById
import no.nav.syfo.behandler.database.getBehandlerKontorById
import no.nav.syfo.behandler.domain.Behandler
import no.nav.syfo.behandler.domain.BehandlerKategori
import no.nav.syfo.behandler.fastlege.FastlegeClient
import no.nav.syfo.behandler.partnerinfo.PartnerinfoClient
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.testhelper.*
import no.nav.syfo.testhelper.UserConstants.BEHANDLER_ETTERNAVN
import no.nav.syfo.testhelper.UserConstants.BEHANDLER_FORNAVN
import no.nav.syfo.testhelper.UserConstants.HERID
import no.nav.syfo.testhelper.UserConstants.HERID_NOT_ACTIVE
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
                    val kontorId = database.createKontor(
                        partnerId = PARTNERID,
                        herId = HERID_NOT_ACTIVE,
                        navn = KONTOR_NAVN,
                    )
                    val kontorBefore = database.getBehandlerKontorById(kontorId)
                    kontorBefore.dialogmeldingEnabled shouldNotBeEqualTo null
                    runBlocking {
                        cronJob.verifyBehandlereForKontorJob()
                    }
                    val kontorAfter = database.getBehandlerKontorById(kontorId)
                    kontorAfter.dialogmeldingEnabled shouldBeEqualTo null
                }
                it("Cronjob endrer ikke kontorets aktivflagg hvis fortsatt aktivt ") {
                    val kontorId = database.createKontor(
                        partnerId = PARTNERID,
                        herId = HERID,
                        navn = KONTOR_NAVN,
                    )
                    runBlocking {
                        cronJob.verifyBehandlereForKontorJob()
                    }
                    val kontorAfter = database.getBehandlerKontorById(kontorId)
                    kontorAfter.dialogmeldingEnabled shouldNotBeEqualTo null
                }
                it("Cronjob invaliderer eksisterende behandler hvis inaktiv i Adresseregisteret") {
                    val kontorId = database.createKontor(
                        partnerId = PARTNERID,
                        herId = HERID,
                        navn = KONTOR_NAVN,
                    )
                    val pBehandler = database.createBehandler(
                        behandler = Behandler(
                            behandlerRef = UUID.randomUUID(),
                            fornavn = BEHANDLER_FORNAVN,
                            mellomnavn = null,
                            etternavn = BEHANDLER_ETTERNAVN,
                            telefon = null,
                            personident = null,
                            herId = HERID,
                            hprId = HPRID_INACTVE,
                            kontor = database.getBehandlerKontorById(kontorId).toBehandlerKontor(),
                            kategori = BehandlerKategori.LEGE,
                            mottatt = OffsetDateTime.now(),
                            suspendert = false,
                        ),
                        kontorId = kontorId,
                    )
                    runBlocking {
                        cronJob.verifyBehandlereForKontorJob()
                    }
                    val behandlerAfter = database.getBehandlerById(pBehandler.id)
                    behandlerAfter!!.invalidated shouldNotBeEqualTo null
                }
            }
        }
    }
})
