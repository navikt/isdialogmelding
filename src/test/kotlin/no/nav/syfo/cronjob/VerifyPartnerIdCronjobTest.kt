package no.nav.syfo.cronjob

import io.mockk.clearAllMocks
import kotlinx.coroutines.test.runTest
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
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class VerifyPartnerIdCronjobTest {
    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val database = externalMockEnvironment.database
    private val environment = externalMockEnvironment.environment
    private val azureAdClient = AzureAdClient(
        azureAppClientId = environment.aadAppClient,
        azureAppClientSecret = environment.azureAppClientSecret,
        azureOpenidConfigTokenEndpoint = environment.azureOpenidConfigTokenEndpoint,
        httpClient = externalMockEnvironment.mockHttpClient,
    )
    private val partnerinfoClient = PartnerinfoClient(
        azureAdClient = azureAdClient,
        syfoPartnerinfoClientId = environment.syfoPartnerinfoClientId,
        syfoPartnerinfoUrl = environment.syfoPartnerinfoUrl,
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
    private val cronJob = VerifyPartnerIdCronjob(
        behandlerService = behandlerService,
        partnerinfoClient = partnerinfoClient,
    )

    @BeforeEach
    fun beforeEach() {
        database.dropData()
        clearAllMocks()
    }

    @Test
    fun `Cronjob virker selv om ingen kontor`() = runTest {
        cronJob.verifyPartnerIdJob()
    }

    @Test
    fun `Cronjob disabler duplikat kontor med udatert partnerID`() = runTest {
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
        cronJob.verifyPartnerIdJob()
        val kontor1 = database.getBehandlerKontorById(kontorId1)
        assertNull(kontor1.dialogmeldingEnabled)
        val kontor2 = database.getBehandlerKontorById(kontorId2)
        assertNotNull(kontor2.dialogmeldingEnabled)
    }

    @Test
    fun `Cronjob gjør ingenting med duplikat kontor med udatert partnerID hvis allerede disabled`() = runTest {
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
        cronJob.verifyPartnerIdJob()
        val kontor1 = database.getBehandlerKontorById(kontorId1)
        assertNull(kontor1.dialogmeldingEnabled)
        val kontor2 = database.getBehandlerKontorById(kontorId2)
        assertNotNull(kontor2.dialogmeldingEnabled)
    }

    @Test
    fun `Cronjob gjør ingenting med kontor med udatert partnerID hvis det ikke finnes et annet kontor med samme herId`() =
        runTest {
            val kontorId = database.createKontor(
                partnerId = PARTNERID,
                herId = OTHER_HERID,
                navn = KONTOR_NAVN,
            )
            cronJob.verifyPartnerIdJob()
            val kontor = database.getBehandlerKontorById(kontorId)
            assertNotNull(kontor.dialogmeldingEnabled)
        }
}
