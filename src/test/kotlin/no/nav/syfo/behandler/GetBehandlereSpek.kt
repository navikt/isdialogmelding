package no.nav.syfo.behandler

import io.mockk.*
import kotlinx.coroutines.runBlocking
import no.nav.syfo.behandler.domain.Behandler
import no.nav.syfo.behandler.domain.BehandlerArbeidstakerRelasjonstype
import no.nav.syfo.behandler.fastlege.FastlegeClient
import no.nav.syfo.behandler.partnerinfo.PartnerinfoClient
import no.nav.syfo.domain.PartnerId
import no.nav.syfo.domain.PersonIdentNumber
import no.nav.syfo.testhelper.*
import no.nav.syfo.testhelper.generator.*
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.*

class GetBehandlereSpek : Spek({

    val externalMockEnvironment = ExternalMockEnvironment.instance
    val database = externalMockEnvironment.database
    val fastlegeClientMock = mockk<FastlegeClient>()
    val partnerinfoClientMock = mockk<PartnerinfoClient>()
    val behandlerService = BehandlerService(
        fastlegeClient = fastlegeClientMock,
        partnerinfoClient = partnerinfoClientMock,
        database = database,
        externalMockEnvironment.environment.toggleSykmeldingbehandlere,
    )
    val callId = "callId"
    val token = "token"
    val behandlerRef = UUID.randomUUID()

    afterEachTest {
        database.dropData()
        clearMocks(
            fastlegeClientMock,
            partnerinfoClientMock,
        )
    }

    describe("BehandlerService") {
        describe("get fastlege and behandlere who have sykmeldt arbeidstaker") {
            it("find fastlege and sykmeldingbehandler for arbeidstaker") {
                mockFastlege(
                    personident = UserConstants.FASTLEGE_FNR,
                    partnerId = UserConstants.PARTNERID,
                    fastlegeClientMock = fastlegeClientMock,
                    partnerinfoClientMock = partnerinfoClientMock,
                )
                database.createBehandlerAndTwoArbeidstakerRelasjoner(
                    behandler = generateBehandler(
                        behandlerRef = behandlerRef,
                        partnerId = UserConstants.OTHER_PARTNERID,
                    ),
                    arbeidstakerPersonIdent = UserConstants.ARBEIDSTAKER_FNR,
                    otherArbeidstakerPersonIdent = UserConstants.ANNEN_ARBEIDSTAKER_FNR,
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
                coEvery {
                    fastlegeClientMock.fastlege(
                        callId = callId,
                        personIdentNumber = UserConstants.ARBEIDSTAKER_FNR,
                        token = token,
                    )
                } returns null
                database.createBehandlerForArbeidstaker(
                    behandler = generateBehandler(
                        behandlerRef = behandlerRef,
                        partnerId = PartnerId(1),
                        dialogmeldingEnabled = false,
                    ),
                    arbeidstakerPersonIdent = UserConstants.ARBEIDSTAKER_FNR,
                    relasjonstype = BehandlerArbeidstakerRelasjonstype.SYKMELDER,
                )

                var behandlere: List<Pair<Behandler, BehandlerArbeidstakerRelasjonstype>>
                runBlocking {
                    behandlere = behandlerService.getBehandlere(UserConstants.ARBEIDSTAKER_FNR, "token", "callId")
                }

                behandlere.size shouldBeEqualTo 0
            }

            it("return behandler as fastlege if behandler is also registered as sykmelder for innbygger") {
                val behandler = generateBehandler(
                    behandlerRef = behandlerRef,
                    partnerId = PartnerId(1),
                    dialogmeldingEnabled = true,
                )
                database.createBehandlerForArbeidstaker(
                    behandler = behandler,
                    arbeidstakerPersonIdent = UserConstants.ARBEIDSTAKER_FNR,
                    relasjonstype = BehandlerArbeidstakerRelasjonstype.SYKMELDER,
                )

                mockFastlege(
                    personident = behandler.personident!!,
                    partnerId = behandler.kontor.partnerId,
                    fastlegeClientMock = fastlegeClientMock,
                    partnerinfoClientMock = partnerinfoClientMock,
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
                    behandlerRef = behandlerRef,
                    partnerId = PartnerId(1),
                    dialogmeldingEnabled = true,
                )
                database.createBehandlerForArbeidstaker(
                    behandler = fastlege,
                    arbeidstakerPersonIdent = UserConstants.ARBEIDSTAKER_FNR,
                    relasjonstype = BehandlerArbeidstakerRelasjonstype.FASTLEGE,
                )

                mockFastlege(
                    personident = UserConstants.FASTLEGE_ANNEN_FNR,
                    partnerId = PartnerId(1),
                    fastlegeClientMock = fastlegeClientMock,
                    partnerinfoClientMock = partnerinfoClientMock,
                )

                var behandlere: List<Pair<Behandler, BehandlerArbeidstakerRelasjonstype>>
                runBlocking {
                    behandlere = behandlerService.getBehandlere(UserConstants.ARBEIDSTAKER_FNR, "token", "callId")
                }

                behandlere.size shouldBeEqualTo 1
                behandlere[0].second shouldBeEqualTo BehandlerArbeidstakerRelasjonstype.FASTLEGE
                behandlere[0].first.personident shouldBeEqualTo UserConstants.FASTLEGE_ANNEN_FNR
            }

            it("return old fastlege as sykmelder") {
                val fastlegeAndSykmelder = generateBehandler(
                    behandlerRef = behandlerRef,
                    partnerId = PartnerId(1),
                    dialogmeldingEnabled = true,
                )
                database.createBehandlerAndTwoArbeidstakerRelasjoner(
                    behandler = fastlegeAndSykmelder,
                    arbeidstakerPersonIdent = UserConstants.ARBEIDSTAKER_FNR,
                    otherArbeidstakerPersonIdent = UserConstants.ARBEIDSTAKER_FNR,
                    relasjonstype = BehandlerArbeidstakerRelasjonstype.FASTLEGE,
                    otherRelasjonstype = BehandlerArbeidstakerRelasjonstype.SYKMELDER,
                )
                mockFastlege(
                    personident = UserConstants.FASTLEGE_ANNEN_FNR,
                    partnerId = PartnerId(1),
                    fastlegeClientMock = fastlegeClientMock,
                    partnerinfoClientMock = partnerinfoClientMock,
                )

                var behandlere: List<Pair<Behandler, BehandlerArbeidstakerRelasjonstype>>
                runBlocking {
                    behandlere = behandlerService.getBehandlere(UserConstants.ARBEIDSTAKER_FNR, "token", "callId")
                }

                behandlere.size shouldBeEqualTo 2
                val fastlege = behandlere.firstOrNull { it.second == BehandlerArbeidstakerRelasjonstype.FASTLEGE }
                fastlege!!.first.personident shouldBeEqualTo UserConstants.FASTLEGE_ANNEN_FNR
                val sykmelder = behandlere.firstOrNull { it.second == BehandlerArbeidstakerRelasjonstype.SYKMELDER }
                sykmelder!!.first.personident shouldBeEqualTo UserConstants.FASTLEGE_FNR
            }
        }
    }
})

fun mockFastlege(
    personident: PersonIdentNumber,
    partnerId: PartnerId,
    fastlegeClientMock: FastlegeClient,
    partnerinfoClientMock: PartnerinfoClient,
) {
    val callId = "callId"
    val token = "token"

    coEvery {
        fastlegeClientMock.fastlege(
            callId = callId,
            personIdentNumber = UserConstants.ARBEIDSTAKER_FNR,
            token = token,
        )
    } returns generateFastlegeResponse(
        personident = personident,
    )
    coEvery { partnerinfoClientMock.partnerinfo(any(), token, callId) } returns generatePartnerinfoResponse(partnerId.value)
}
