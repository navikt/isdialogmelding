package no.nav.syfo.behandler

import kotlinx.coroutines.test.runTest
import no.nav.syfo.behandler.domain.Behandler
import no.nav.syfo.behandler.domain.BehandlerArbeidstakerRelasjonstype
import no.nav.syfo.behandler.fastlege.FastlegeClient
import no.nav.syfo.behandler.partnerinfo.PartnerinfoClient
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.domain.PartnerId
import no.nav.syfo.testhelper.*
import no.nav.syfo.testhelper.generator.generateBehandler
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*

class GetBehandlereTest {

    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val database = externalMockEnvironment.database
    private val mockHttpClient = externalMockEnvironment.mockHttpClient
    private val azureAdClient = AzureAdClient(
        azureAppClientId = externalMockEnvironment.environment.aadAppClient,
        azureAppClientSecret = externalMockEnvironment.environment.azureAppClientSecret,
        azureOpenidConfigTokenEndpoint = externalMockEnvironment.environment.azureOpenidConfigTokenEndpoint,
        httpClient = mockHttpClient,
    )
    private val fastlegeClient = FastlegeClient(
        azureAdClient = azureAdClient,
        fastlegeRestClientId = externalMockEnvironment.environment.fastlegeRestClientId,
        fastlegeRestUrl = externalMockEnvironment.environment.fastlegeRestUrl,
        httpClient = mockHttpClient,
    )
    private val partnerinfoClient = PartnerinfoClient(
        azureAdClient = azureAdClient,
        syfoPartnerinfoClientId = externalMockEnvironment.environment.fastlegeRestClientId,
        syfoPartnerinfoUrl = externalMockEnvironment.environment.syfoPartnerinfoUrl,
        httpClient = mockHttpClient,
    )

    private val behandlerService = BehandlerService(
        fastlegeClient = fastlegeClient,
        partnerinfoClient = partnerinfoClient,
        database = database,
    )
    private val behandlerRef = UUID.randomUUID()

    @AfterEach
    fun afterEach() {
        database.dropData()
    }

    @Test
    fun `find fastlege and sykmeldingbehandler for arbeidstaker`() = runTest {
        database.createBehandlerAndTwoArbeidstakerRelasjoner(
            behandler = generateBehandler(
                behandlerRef = behandlerRef,
                partnerId = UserConstants.OTHER_PARTNERID,
            ),
            arbeidstakerPersonident = UserConstants.ARBEIDSTAKER_FNR,
            otherArbeidstakerPersonident = UserConstants.ANNEN_ARBEIDSTAKER_FNR,
            relasjonstype = BehandlerArbeidstakerRelasjonstype.SYKMELDER,
            otherRelasjonstype = BehandlerArbeidstakerRelasjonstype.SYKMELDER,
        )

        var behandlere: List<Pair<Behandler, BehandlerArbeidstakerRelasjonstype>>
        behandlere = behandlerService.getBehandlere(
            UserConstants.ARBEIDSTAKER_FNR,
            "token",
            "callId"
        )

        assertEquals(2, behandlere.size)
        assertEquals(BehandlerArbeidstakerRelasjonstype.FASTLEGE, behandlere[0].second)
        assertEquals(BehandlerArbeidstakerRelasjonstype.SYKMELDER, behandlere[1].second)
    }

    @Test
    fun `exclude behandlere without electronic communication enabled`() = runTest {
        database.createBehandlerForArbeidstaker(
            behandler = generateBehandler(
                behandlerRef = behandlerRef,
                partnerId = PartnerId(1),
                dialogmeldingEnabled = false,
            ),
            arbeidstakerPersonident = UserConstants.ARBEIDSTAKER_UTEN_FASTLEGE_FNR,
            relasjonstype = BehandlerArbeidstakerRelasjonstype.SYKMELDER,
        )

        var behandlere: List<Pair<Behandler, BehandlerArbeidstakerRelasjonstype>>
        behandlere = behandlerService.getBehandlere(UserConstants.ARBEIDSTAKER_UTEN_FASTLEGE_FNR, "token", "callId")

        assertEquals(0, behandlere.size)
    }

    @Test
    fun `return behandler as fastlege if behandler is also registered as sykmelder for innbygger`() = runTest {
        val behandler = generateBehandler(
            behandlerRef = behandlerRef,
            partnerId = UserConstants.PARTNERID,
            dialogmeldingEnabled = true,
        )
        database.createBehandlerForArbeidstaker(
            behandler = behandler,
            arbeidstakerPersonident = UserConstants.ARBEIDSTAKER_FNR,
            relasjonstype = BehandlerArbeidstakerRelasjonstype.SYKMELDER,
        )

        var behandlere: List<Pair<Behandler, BehandlerArbeidstakerRelasjonstype>>
        behandlere = behandlerService.getBehandlere(UserConstants.ARBEIDSTAKER_FNR, "token", "callId")
        val relasjoner = database.getBehandlerArbeidstakerRelasjoner(UserConstants.ARBEIDSTAKER_FNR)

        assertEquals(1, behandlere.size)
        assertEquals(BehandlerArbeidstakerRelasjonstype.FASTLEGE, behandlere[0].second)
        assertEquals(2, relasjoner.size)
        assertEquals(1, relasjoner.filter { it.type == BehandlerArbeidstakerRelasjonstype.FASTLEGE.name }.size)
        assertEquals(1, relasjoner.filter { it.type == BehandlerArbeidstakerRelasjonstype.SYKMELDER.name }.size)
    }

    @Test
    fun `return newest fastlege if more than one`() = runTest {
        val fastlege = generateBehandler(
            personident = UserConstants.FASTLEGE_ANNEN_FNR,
            behandlerRef = behandlerRef,
            partnerId = UserConstants.PARTNERID,
            dialogmeldingEnabled = true,
        )
        database.createBehandlerForArbeidstaker(
            behandler = fastlege,
            arbeidstakerPersonident = UserConstants.ARBEIDSTAKER_FNR,
            relasjonstype = BehandlerArbeidstakerRelasjonstype.FASTLEGE,
        )

        var behandlere: List<Pair<Behandler, BehandlerArbeidstakerRelasjonstype>>
        behandlere = behandlerService.getBehandlere(UserConstants.ARBEIDSTAKER_FNR, "token", "callId")

        assertEquals(1, behandlere.size)
        assertEquals(BehandlerArbeidstakerRelasjonstype.FASTLEGE, behandlere[0].second)
        assertEquals(UserConstants.FASTLEGE_FNR, behandlere[0].first.personident)
    }

    @Test
    fun `return old fastlege as sykmelder`() = runTest {
        val fastlegeAndSykmelder = generateBehandler(
            personident = UserConstants.FASTLEGE_ANNEN_FNR,
            behandlerRef = behandlerRef,
            partnerId = UserConstants.PARTNERID,
            dialogmeldingEnabled = true,
        )
        database.createBehandlerAndTwoArbeidstakerRelasjoner(
            behandler = fastlegeAndSykmelder,
            arbeidstakerPersonident = UserConstants.ARBEIDSTAKER_FNR,
            otherArbeidstakerPersonident = UserConstants.ARBEIDSTAKER_FNR,
            relasjonstype = BehandlerArbeidstakerRelasjonstype.FASTLEGE,
            otherRelasjonstype = BehandlerArbeidstakerRelasjonstype.SYKMELDER,
        )

        var behandlere: List<Pair<Behandler, BehandlerArbeidstakerRelasjonstype>>
        behandlere = behandlerService.getBehandlere(UserConstants.ARBEIDSTAKER_FNR, "token", "callId")

        assertEquals(2, behandlere.size)
        val fastlege = behandlere.firstOrNull { it.second == BehandlerArbeidstakerRelasjonstype.FASTLEGE }
        assertEquals(UserConstants.FASTLEGE_FNR, fastlege!!.first.personident)
        val sykmelder = behandlere.firstOrNull { it.second == BehandlerArbeidstakerRelasjonstype.SYKMELDER }
        assertEquals(UserConstants.FASTLEGE_ANNEN_FNR, sykmelder!!.first.personident)
    }
}
