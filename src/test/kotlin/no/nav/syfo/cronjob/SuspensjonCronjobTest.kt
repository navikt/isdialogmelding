package no.nav.syfo.cronjob

import kotlinx.coroutines.test.runTest
import no.nav.syfo.behandler.BehandlerService
import no.nav.syfo.behandler.database.getBehandlerByBehandlerRef
import no.nav.syfo.behandler.fastlege.FastlegeClient
import no.nav.syfo.behandler.partnerinfo.PartnerinfoClient
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.btsys.LegeSuspensjonClient
import no.nav.syfo.testhelper.*
import no.nav.syfo.testhelper.UserConstants.PARTNERID
import no.nav.syfo.testhelper.generator.generateBehandler
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.*

class SuspensjonCronjobTest {
    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val database = externalMockEnvironment.database
    private val environment = externalMockEnvironment.environment
    private val azureAdClient = AzureAdClient(
        azureAppClientId = environment.aadAppClient,
        azureAppClientSecret = environment.azureAppClientSecret,
        azureOpenidConfigTokenEndpoint = environment.azureOpenidConfigTokenEndpoint,
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
    private val cronJob = SuspensjonCronjob(
        behandlerService = behandlerService,
        legeSuspensjonClient = LegeSuspensjonClient(
            azureAdClient = azureAdClient,
            endpointClientId = environment.btsysClientId,
            endpointUrl = environment.btsysUrl,
            httpClient = externalMockEnvironment.mockHttpClient,
        ),
    )

    @AfterEach
    fun afterEach() {
        database.dropData()
    }

    @Test
    fun `Cronjob virker selv om ingen behandlere`() = runTest {
        cronJob.checkLegeSuspensjonJob()
    }

    @Test
    fun `Cronjob bevarer suspendert=false for eksisterende behandler`() = runTest {
        val behandlerUUID = database.createBehandlerForArbeidstaker(
            behandler = generateBehandler(
                behandlerRef = UUID.randomUUID(),
                partnerId = PARTNERID,
            ),
            arbeidstakerPersonident = UserConstants.ARBEIDSTAKER_FNR,
        )
        cronJob.checkLegeSuspensjonJob()
        assertFalse(database.getBehandlerByBehandlerRef(behandlerUUID)!!.suspendert)
    }

    @Test
    fun `Cronjob setter suspendert=true for suspendert behandler`() = runTest {
        val behandlerUUID = database.createBehandlerForArbeidstaker(
            behandler = generateBehandler(
                behandlerRef = UUID.randomUUID(),
                partnerId = PARTNERID,
                personident = UserConstants.FASTLEGE_FNR_SUSPENDERT,
            ),
            arbeidstakerPersonident = UserConstants.ARBEIDSTAKER_FNR,
        )
        assertFalse(database.getBehandlerByBehandlerRef(behandlerUUID)!!.suspendert)
        cronJob.checkLegeSuspensjonJob()
        assertTrue(database.getBehandlerByBehandlerRef(behandlerUUID)!!.suspendert)
    }

    @Test
    fun `Cronjob setter suspendert=false for behandler som ikke lengre er suspendert`() = runTest {
        val behandlerUUID = database.createBehandlerForArbeidstaker(
            behandler = generateBehandler(
                behandlerRef = UUID.randomUUID(),
                partnerId = PARTNERID,
            ),
            arbeidstakerPersonident = UserConstants.ARBEIDSTAKER_FNR,
        )
        database.setSuspendert(behandlerUUID.toString())
        assertTrue(database.getBehandlerByBehandlerRef(behandlerUUID)!!.suspendert)
        cronJob.checkLegeSuspensjonJob()
        assertFalse(database.getBehandlerByBehandlerRef(behandlerUUID)!!.suspendert)
    }
}
