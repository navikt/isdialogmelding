package no.nav.syfo.cronjob

import io.mockk.clearAllMocks
import kotlinx.coroutines.runBlocking
import no.nav.syfo.behandler.BehandlerService
import no.nav.syfo.behandler.database.getBehandlerKontorById
import no.nav.syfo.behandler.fastlege.FastlegeClient
import no.nav.syfo.behandler.partnerinfo.PartnerinfoClient
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.testhelper.ExternalMockEnvironment
import no.nav.syfo.testhelper.UserConstants.KONTOR_NAVN
import no.nav.syfo.testhelper.UserConstants.OTHER_HERID
import no.nav.syfo.testhelper.UserConstants.OTHER_PARTNERID
import no.nav.syfo.testhelper.UserConstants.PARTNERID
import no.nav.syfo.testhelper.createKontor
import no.nav.syfo.testhelper.dropData
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBe
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class VerifyPartnerIdCronjobSpek : Spek({
    describe(VerifyPartnerIdCronjobSpek::class.java.simpleName) {
        val externalMockEnvironment = ExternalMockEnvironment.instance
        val database = externalMockEnvironment.database
        val environment = externalMockEnvironment.environment
        val azureAdClient = AzureAdClient(
            azureAppClientId = environment.aadAppClient,
            azureAppClientSecret = environment.azureAppClientSecret,
            azureOpenidConfigTokenEndpoint = environment.azureOpenidConfigTokenEndpoint,
            httpClient = externalMockEnvironment.mockHttpClient,
        )
        val partnerinfoClient = PartnerinfoClient(
            azureAdClient = azureAdClient,
            syfoPartnerinfoClientId = environment.syfoPartnerinfoClientId,
            syfoPartnerinfoUrl = environment.syfoPartnerinfoUrl,
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
        val cronJob = VerifyPartnerIdCronjob(
            behandlerService = behandlerService,
            partnerinfoClient = partnerinfoClient,
        )
        beforeEachTest {
            database.dropData()
            clearAllMocks()
        }

        describe("Cronjob sjekker eksisterende behandlerkontor") {
            it("Cronjob virker selv om ingen kontor") {
                runBlocking {
                    cronJob.verifyPartnerIdJob()
                }
            }
            it("Cronjob disabler duplikat kontor med udatert partnerID") {
                val kontorId1 = database.createKontor(
                    partnerId = PARTNERID,
                    herId = OTHER_HERID,
                    navn = KONTOR_NAVN,
                )
                val kontorId2 = database.createKontor(
                    partnerId = OTHER_PARTNERID,
                    herId = OTHER_HERID,
                    navn = KONTOR_NAVN,
                )
                runBlocking {
                    cronJob.verifyPartnerIdJob()
                }
                val kontor1 = database.getBehandlerKontorById(kontorId1)
                kontor1.dialogmeldingEnabled shouldBeEqualTo null
                val kontor2 = database.getBehandlerKontorById(kontorId2)
                kontor2.dialogmeldingEnabled shouldNotBe null
            }
            it("Cronjob gjør ingenting med duplikat kontor med udatert partnerID hvis allerede disabled") {
                val kontorId1 = database.createKontor(
                    partnerId = PARTNERID,
                    herId = OTHER_HERID,
                    navn = KONTOR_NAVN,
                    dialogmeldingEnabled = false,
                )
                val kontorId2 = database.createKontor(
                    partnerId = OTHER_PARTNERID,
                    herId = OTHER_HERID,
                    navn = KONTOR_NAVN,
                )
                runBlocking {
                    cronJob.verifyPartnerIdJob()
                }
                val kontor1 = database.getBehandlerKontorById(kontorId1)
                kontor1.dialogmeldingEnabled shouldBeEqualTo null
                val kontor2 = database.getBehandlerKontorById(kontorId2)
                kontor2.dialogmeldingEnabled shouldNotBe null
            }
            it("Cronjob gjør ingenting med kontor med udatert partnerID hvis det ikke finnes et annet kontor med samme herId") {
                val kontorId = database.createKontor(
                    partnerId = PARTNERID,
                    herId = OTHER_HERID,
                    navn = KONTOR_NAVN,
                )
                runBlocking {
                    cronJob.verifyPartnerIdJob()
                }
                val kontor = database.getBehandlerKontorById(kontorId)
                kontor.dialogmeldingEnabled shouldNotBe null
            }
        }
    }
})
