package no.nav.syfo.cronjob

import io.ktor.server.testing.*
import io.mockk.clearAllMocks
import kotlinx.coroutines.runBlocking
import no.nav.syfo.behandler.BehandlerService
import no.nav.syfo.behandler.database.getBehandlerByBehandlerRef
import no.nav.syfo.behandler.fastlege.FastlegeClient
import no.nav.syfo.behandler.partnerinfo.PartnerinfoClient
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.btsys.LegeSuspensjonClient
import no.nav.syfo.testhelper.*
import no.nav.syfo.testhelper.UserConstants.PARTNERID
import no.nav.syfo.testhelper.generator.generateBehandler
import org.amshove.kluent.*
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.*

class SuspensjonCronjobSpek : Spek({
    describe(SuspensjonCronjobSpek::class.java.simpleName) {
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
            val cronJob = SuspensjonCronjob(
                behandlerService = behandlerService,
                legeSuspensjonClient = LegeSuspensjonClient(
                    azureAdClient = azureAdClient,
                    endpointClientId = environment.btsysClientId,
                    endpointUrl = environment.btsysUrl,
                    httpClient = externalMockEnvironment.mockHttpClient,
                ),
            )
            beforeEachTest {
                database.dropData()
                clearAllMocks()
            }

            describe("Cronjob oppdaterer suspendert for behandlere") {
                it("Cronjob virker selv om ingen behandlere") {
                    runBlocking {
                        cronJob.checkLegeSuspensjonJob()
                    }
                }
                it("Cronjob bevarer suspendert=false for eksisterende behandler") {
                    val behandlerUUID = database.createBehandlerForArbeidstaker(
                        behandler = generateBehandler(
                            behandlerRef = UUID.randomUUID(),
                            partnerId = PARTNERID,
                        ),
                        arbeidstakerPersonident = UserConstants.ARBEIDSTAKER_FNR,
                    )
                    runBlocking {
                        cronJob.checkLegeSuspensjonJob()
                    }
                    database.getBehandlerByBehandlerRef(behandlerUUID)!!.suspendert shouldBeEqualTo false
                }
            }
            it("Cronjob setter suspendert=true for suspendert behandler") {
                val behandlerUUID = database.createBehandlerForArbeidstaker(
                    behandler = generateBehandler(
                        behandlerRef = UUID.randomUUID(),
                        partnerId = PARTNERID,
                        personident = UserConstants.FASTLEGE_FNR_SUSPENDERT,
                    ),
                    arbeidstakerPersonident = UserConstants.ARBEIDSTAKER_FNR,
                )
                database.getBehandlerByBehandlerRef(behandlerUUID)!!.suspendert shouldBeEqualTo false
                runBlocking {
                    cronJob.checkLegeSuspensjonJob()
                }
                database.getBehandlerByBehandlerRef(behandlerUUID)!!.suspendert shouldBeEqualTo true
            }
            it("Cronjob setter suspendert=false for behandler som ikke lengre er suspendert") {
                val behandlerUUID = database.createBehandlerForArbeidstaker(
                    behandler = generateBehandler(
                        behandlerRef = UUID.randomUUID(),
                        partnerId = PARTNERID,
                    ),
                    arbeidstakerPersonident = UserConstants.ARBEIDSTAKER_FNR,
                )
                database.setSuspendert(behandlerUUID.toString())
                database.getBehandlerByBehandlerRef(behandlerUUID)!!.suspendert shouldBeEqualTo true
                runBlocking {
                    cronJob.checkLegeSuspensjonJob()
                }
                database.getBehandlerByBehandlerRef(behandlerUUID)!!.suspendert shouldBeEqualTo false
            }
        }
    }
})
