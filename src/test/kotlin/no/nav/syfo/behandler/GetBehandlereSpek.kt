package no.nav.syfo.behandler

import kotlinx.coroutines.runBlocking
import no.nav.syfo.behandler.domain.Behandler
import no.nav.syfo.behandler.domain.BehandlerArbeidstakerRelasjonstype
import no.nav.syfo.behandler.fastlege.FastlegeClient
import no.nav.syfo.behandler.partnerinfo.PartnerinfoClient
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.domain.PartnerId
import no.nav.syfo.testhelper.*
import no.nav.syfo.testhelper.generator.generateBehandler
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.*

class GetBehandlereSpek : Spek({

    val externalMockEnvironment = ExternalMockEnvironment.instance
    val database = externalMockEnvironment.database
    val mockHttpClient = externalMockEnvironment.mockHttpClient
    val azureAdClient = AzureAdClient(
        azureAppClientId = externalMockEnvironment.environment.aadAppClient,
        azureAppClientSecret = externalMockEnvironment.environment.azureAppClientSecret,
        azureOpenidConfigTokenEndpoint = externalMockEnvironment.environment.azureOpenidConfigTokenEndpoint,
        httpClient = mockHttpClient,
    )
    val fastlegeClient = FastlegeClient(
        azureAdClient = azureAdClient,
        fastlegeRestClientId = externalMockEnvironment.environment.fastlegeRestClientId,
        fastlegeRestUrl = externalMockEnvironment.environment.fastlegeRestUrl,
        httpClient = mockHttpClient,
    )
    val partnerinfoClient = PartnerinfoClient(
        azureAdClient = azureAdClient,
        syfoPartnerinfoClientId = externalMockEnvironment.environment.fastlegeRestClientId,
        syfoPartnerinfoUrl = externalMockEnvironment.environment.syfoPartnerinfoUrl,
        httpClient = mockHttpClient,
    )

    val behandlerService = BehandlerService(
        fastlegeClient = fastlegeClient,
        partnerinfoClient = partnerinfoClient,
        database = database,
    )
    val behandlerRef = UUID.randomUUID()

    afterEachTest {
        database.dropData()
    }

    describe("BehandlerService") {
        describe("get fastlege and behandlere who have sykmeldt arbeidstaker") {
            it("find fastlege and sykmeldingbehandler for arbeidstaker") {
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
                runBlocking {
                    behandlere = behandlerService.getBehandlere(
                        UserConstants.ARBEIDSTAKER_FNR,
                        "token",
                        "callId"
                    )
                }

                behandlere.size shouldBeEqualTo 2
                behandlere[0].second shouldBeEqualTo BehandlerArbeidstakerRelasjonstype.FASTLEGE
                behandlere[1].second shouldBeEqualTo BehandlerArbeidstakerRelasjonstype.SYKMELDER
            }

            it("exclude behandlere without electronic communication enabled") {
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
                runBlocking {
                    behandlere = behandlerService.getBehandlere(UserConstants.ARBEIDSTAKER_UTEN_FASTLEGE_FNR, "token", "callId")
                }

                behandlere.size shouldBeEqualTo 0
            }

            it("return behandler as fastlege if behandler is also registered as sykmelder for innbygger") {
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
                runBlocking {
                    behandlere = behandlerService.getBehandlere(UserConstants.ARBEIDSTAKER_FNR, "token", "callId")
                }
                val relasjoner = database.getBehandlerArbeidstakerRelasjoner(UserConstants.ARBEIDSTAKER_FNR)

                behandlere.size shouldBeEqualTo 1
                behandlere[0].second shouldBeEqualTo BehandlerArbeidstakerRelasjonstype.FASTLEGE
                relasjoner.size shouldBeEqualTo 2
                relasjoner.filter { it.type == BehandlerArbeidstakerRelasjonstype.FASTLEGE.name }.size shouldBeEqualTo 1
                relasjoner.filter { it.type == BehandlerArbeidstakerRelasjonstype.SYKMELDER.name }.size shouldBeEqualTo 1
            }

            it("return newest fastlege if more than one") {
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
                runBlocking {
                    behandlere = behandlerService.getBehandlere(UserConstants.ARBEIDSTAKER_FNR, "token", "callId")
                }

                behandlere.size shouldBeEqualTo 1
                behandlere[0].second shouldBeEqualTo BehandlerArbeidstakerRelasjonstype.FASTLEGE
                behandlere[0].first.personident shouldBeEqualTo UserConstants.FASTLEGE_FNR
            }

            it("return old fastlege as sykmelder") {
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
                runBlocking {
                    behandlere = behandlerService.getBehandlere(UserConstants.ARBEIDSTAKER_FNR, "token", "callId")
                }

                behandlere.size shouldBeEqualTo 2
                val fastlege = behandlere.firstOrNull { it.second == BehandlerArbeidstakerRelasjonstype.FASTLEGE }
                fastlege!!.first.personident shouldBeEqualTo UserConstants.FASTLEGE_FNR
                val sykmelder = behandlere.firstOrNull { it.second == BehandlerArbeidstakerRelasjonstype.SYKMELDER }
                sykmelder!!.first.personident shouldBeEqualTo UserConstants.FASTLEGE_ANNEN_FNR
            }
        }
    }
})
