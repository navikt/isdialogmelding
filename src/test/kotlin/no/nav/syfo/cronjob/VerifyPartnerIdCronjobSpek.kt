package no.nav.syfo.cronjob

import io.ktor.server.testing.*
import io.mockk.clearAllMocks
import kotlinx.coroutines.runBlocking
import no.nav.syfo.behandler.BehandlerService
import no.nav.syfo.behandler.database.createBehandlerKontor
import no.nav.syfo.behandler.database.getBehandlerKontorById
import no.nav.syfo.behandler.domain.BehandlerKontor
import no.nav.syfo.behandler.fastlege.FastlegeClient
import no.nav.syfo.behandler.partnerinfo.PartnerinfoClient
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.testhelper.ExternalMockEnvironment
import no.nav.syfo.testhelper.UserConstants.KONTOR_NAVN
import no.nav.syfo.testhelper.UserConstants.OTHER_HERID
import no.nav.syfo.testhelper.UserConstants.OTHER_PARTNERID
import no.nav.syfo.testhelper.UserConstants.PARTNERID
import no.nav.syfo.testhelper.dropData
import org.amshove.kluent.*
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.OffsetDateTime

class VerifyPartnerIdCronjobSpek : Spek({
    describe(VerifyPartnerIdCronjobSpek::class.java.simpleName) {
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
                    var kontorId1: Int
                    var kontorId2: Int
                    database.connection.use { connection ->
                        kontorId1 = connection.createBehandlerKontor(
                            kontor = BehandlerKontor(
                                partnerId = PARTNERID,
                                herId = OTHER_HERID,
                                navn = KONTOR_NAVN,
                                adresse = null,
                                postnummer = null,
                                poststed = null,
                                orgnummer = null,
                                dialogmeldingEnabled = true,
                                dialogmeldingEnabledLocked = false,
                                system = null,
                                mottatt = OffsetDateTime.now()
                            )
                        )
                        kontorId2 = connection.createBehandlerKontor(
                            kontor = BehandlerKontor(
                                partnerId = OTHER_PARTNERID,
                                herId = OTHER_HERID,
                                navn = KONTOR_NAVN,
                                adresse = null,
                                postnummer = null,
                                poststed = null,
                                orgnummer = null,
                                dialogmeldingEnabled = true,
                                dialogmeldingEnabledLocked = false,
                                system = null,
                                mottatt = OffsetDateTime.now()
                            )
                        )
                        connection.commit()
                    }
                    runBlocking {
                        cronJob.verifyPartnerIdJob()
                    }
                    val kontor1 = database.getBehandlerKontorById(kontorId1)
                    kontor1.dialogmeldingEnabled shouldBeEqualTo null
                    val kontor2 = database.getBehandlerKontorById(kontorId2)
                    kontor2.dialogmeldingEnabled shouldNotBe null
                }
                it("Cronjob gjør ingenting med duplikat kontor med udatert partnerID hvis allerede disabled") {
                    var kontorId1: Int
                    var kontorId2: Int
                    database.connection.use { connection ->
                        kontorId1 = connection.createBehandlerKontor(
                            kontor = BehandlerKontor(
                                partnerId = PARTNERID,
                                herId = OTHER_HERID,
                                navn = KONTOR_NAVN,
                                adresse = null,
                                postnummer = null,
                                poststed = null,
                                orgnummer = null,
                                dialogmeldingEnabled = false,
                                dialogmeldingEnabledLocked = false,
                                system = null,
                                mottatt = OffsetDateTime.now()
                            )
                        )
                        kontorId2 = connection.createBehandlerKontor(
                            kontor = BehandlerKontor(
                                partnerId = OTHER_PARTNERID,
                                herId = OTHER_HERID,
                                navn = KONTOR_NAVN,
                                adresse = null,
                                postnummer = null,
                                poststed = null,
                                orgnummer = null,
                                dialogmeldingEnabled = true,
                                dialogmeldingEnabledLocked = false,
                                system = null,
                                mottatt = OffsetDateTime.now()
                            )
                        )
                        connection.commit()
                    }
                    runBlocking {
                        cronJob.verifyPartnerIdJob()
                    }
                    val kontor1 = database.getBehandlerKontorById(kontorId1)
                    kontor1.dialogmeldingEnabled shouldBeEqualTo null
                    val kontor2 = database.getBehandlerKontorById(kontorId2)
                    kontor2.dialogmeldingEnabled shouldNotBe null
                }
                it("Cronjob gjør ingenting med kontor med udatert partnerID hvis det ikke finnes et annet kontor med samme herId") {
                    val kontorId = database.connection.use { connection ->
                        connection.createBehandlerKontor(
                            kontor = BehandlerKontor(
                                partnerId = PARTNERID,
                                herId = OTHER_HERID,
                                navn = KONTOR_NAVN,
                                adresse = null,
                                postnummer = null,
                                poststed = null,
                                orgnummer = null,
                                dialogmeldingEnabled = true,
                                dialogmeldingEnabledLocked = false,
                                system = null,
                                mottatt = OffsetDateTime.now()
                            )
                        ).also {
                            connection.commit()
                        }
                    }
                    runBlocking {
                        cronJob.verifyPartnerIdJob()
                    }
                    val kontor = database.getBehandlerKontorById(kontorId)
                    kontor.dialogmeldingEnabled shouldNotBe null
                }
            }
        }
    }
})
