package no.nav.syfo.behandler

import io.ktor.server.testing.*
import io.mockk.mockk
import no.nav.syfo.behandler.database.getBehandlerDialogmeldingForArbeidstaker
import no.nav.syfo.behandler.fastlege.toBehandler
import no.nav.syfo.testhelper.ExternalMockEnvironment
import no.nav.syfo.testhelper.UserConstants
import no.nav.syfo.testhelper.createBehandlerDialogmeldingForArbeidstaker
import no.nav.syfo.testhelper.dropData
import no.nav.syfo.testhelper.generator.generateFastlegeResponse
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEqualTo
import org.junit.Assert.assertThrows
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

class BehandlerServiceSpek : Spek({
    describe("BehandlerService") {
        with(TestApplicationEngine()) {
            start()

            val externalMockEnvironment = ExternalMockEnvironment.instance
            val database = externalMockEnvironment.database
            val behandlerService = BehandlerService(
                fastlegeClient = mockk(),
                partnerinfoClient = mockk(),
                database = database
            )

            afterEachTest {
                database.dropData()
            }

            describe("createOrGetBehandler") {
                it("lagrer behandler for arbeidstaker") {
                    val behandler =
                        behandlerService.createOrGetBehandler(
                            generateFastlegeResponse().toBehandler(UserConstants.PARTNERID),
                            UserConstants.ARBEIDSTAKER_FNR
                        )

                    val pBehandlerList = database.getBehandlerDialogmeldingForArbeidstaker(
                        UserConstants.ARBEIDSTAKER_FNR,
                    )
                    pBehandlerList.size shouldBeEqualTo 1
                    pBehandlerList[0].behandlerRef shouldBeEqualTo behandler.behandlerRef
                }
                it("lagrer behandler for arbeidstaker én gang når kallt flere ganger for samme behandler og arbeidstaker") {
                    val behandler = generateFastlegeResponse().toBehandler(UserConstants.PARTNERID)
                    behandlerService.createOrGetBehandler(behandler, UserConstants.ARBEIDSTAKER_FNR)
                    behandlerService.createOrGetBehandler(behandler, UserConstants.ARBEIDSTAKER_FNR)

                    val pBehandlerList = database.getBehandlerDialogmeldingForArbeidstaker(
                        UserConstants.ARBEIDSTAKER_FNR,
                    )
                    pBehandlerList.size shouldBeEqualTo 1
                }
                it("lagrer én behandler koblet til begge arbeidstakere når kallt for to ulike arbeidstakere med samme behandler") {
                    val behandler = generateFastlegeResponse().toBehandler(UserConstants.PARTNERID)
                    behandlerService.createOrGetBehandler(behandler, UserConstants.ANNEN_ARBEIDSTAKER_FNR)
                    behandlerService.createOrGetBehandler(behandler, UserConstants.ARBEIDSTAKER_FNR)

                    val pBehandlerForArbeidstakerList = database.getBehandlerDialogmeldingForArbeidstaker(
                        UserConstants.ARBEIDSTAKER_FNR,
                    )
                    val pBehandlerForAnnenArbeidstakerList = database.getBehandlerDialogmeldingForArbeidstaker(
                        UserConstants.ANNEN_ARBEIDSTAKER_FNR,
                    )
                    pBehandlerForArbeidstakerList.size shouldBeEqualTo 1
                    pBehandlerForAnnenArbeidstakerList.size shouldBeEqualTo 1
                    pBehandlerForArbeidstakerList[0].behandlerRef shouldBeEqualTo pBehandlerForAnnenArbeidstakerList[0].behandlerRef
                }
                it("lagrer behandler uten fnr") {
                    val behandler =
                        generateFastlegeResponse(null, UserConstants.HERID, UserConstants.HPRID).toBehandler(UserConstants.PARTNERID)
                    behandlerService.createOrGetBehandler(
                        behandler,
                        UserConstants.ARBEIDSTAKER_FNR
                    )

                    database.getBehandlerDialogmeldingForArbeidstaker(
                        UserConstants.ARBEIDSTAKER_FNR,
                    ).size shouldBeEqualTo 1
                }
                it("lagrer behandler uten herId") {
                    val behandler =
                        generateFastlegeResponse(
                            UserConstants.FASTLEGE_FNR,
                            null,
                            UserConstants.HPRID
                        ).toBehandler(UserConstants.PARTNERID)
                    behandlerService.createOrGetBehandler(
                        behandler,
                        UserConstants.ARBEIDSTAKER_FNR
                    )

                    database.getBehandlerDialogmeldingForArbeidstaker(
                        UserConstants.ARBEIDSTAKER_FNR,
                    ).size shouldBeEqualTo 1
                }
                it("lagrer behandler uten hprId") {
                    val behandler =
                        generateFastlegeResponse(
                            UserConstants.FASTLEGE_FNR,
                            UserConstants.HERID,
                            null
                        ).toBehandler(UserConstants.PARTNERID)
                    behandlerService.createOrGetBehandler(
                        behandler,
                        UserConstants.ARBEIDSTAKER_FNR
                    )

                    database.getBehandlerDialogmeldingForArbeidstaker(
                        UserConstants.ARBEIDSTAKER_FNR,
                    ).size shouldBeEqualTo 1
                }
                it("lagrer ikke behandler uten fnr, herId og hprId") {
                    val behandler =
                        generateFastlegeResponse(null, null, null).toBehandler(UserConstants.PARTNERID)

                    assertThrows(IllegalArgumentException::class.java) {
                        behandlerService.createOrGetBehandler(
                            behandler,
                            UserConstants.ARBEIDSTAKER_FNR
                        )
                    }
                    database.getBehandlerDialogmeldingForArbeidstaker(
                        UserConstants.ARBEIDSTAKER_FNR,
                    ).size shouldBeEqualTo 0
                }
                it("lagrer ikke behandler for arbeidstaker når samme behandler er siste lagrede behandler for arbeidstaker") {
                    val behandler = generateFastlegeResponse().toBehandler(UserConstants.PARTNERID)
                    val existingBehandlerRef =
                        database.createBehandlerDialogmeldingForArbeidstaker(
                            behandler = behandler,
                            arbeidstakerPersonIdent = UserConstants.ARBEIDSTAKER_FNR
                        )

                    behandlerService.createOrGetBehandler(
                        behandler = behandler,
                        arbeidstakerPersonIdent = UserConstants.ARBEIDSTAKER_FNR
                    )

                    val pBehandlerForArbeidstakerList = database.getBehandlerDialogmeldingForArbeidstaker(
                        UserConstants.ARBEIDSTAKER_FNR,
                    )
                    pBehandlerForArbeidstakerList.size shouldBeEqualTo 1
                    pBehandlerForArbeidstakerList[0].behandlerRef shouldBeEqualTo existingBehandlerRef
                }
                it("lagrer behandler for arbeidstaker når samme behandler er lagret for annen arbeidstaker") {
                    val behandler = generateFastlegeResponse().toBehandler(UserConstants.PARTNERID)
                    val existingBehandlerRef =
                        database.createBehandlerDialogmeldingForArbeidstaker(
                            behandler = behandler,
                            arbeidstakerPersonIdent = UserConstants.ANNEN_ARBEIDSTAKER_FNR
                        )
                    behandlerService.createOrGetBehandler(
                        behandler = behandler,
                        arbeidstakerPersonIdent = UserConstants.ARBEIDSTAKER_FNR
                    )

                    val pBehandlerForArbeidstakerList = database.getBehandlerDialogmeldingForArbeidstaker(
                        UserConstants.ARBEIDSTAKER_FNR,
                    )
                    pBehandlerForArbeidstakerList.size shouldBeEqualTo 1
                    pBehandlerForArbeidstakerList[0].behandlerRef shouldBeEqualTo existingBehandlerRef
                }
                it("lagrer behandler for arbeidstaker når behandler er annen enn siste lagrede behandler for arbeidstaker") {
                    val behandler = generateFastlegeResponse(UserConstants.FASTLEGE_FNR).toBehandler(UserConstants.PARTNERID)
                    val annenBehandler =
                        generateFastlegeResponse(UserConstants.FASTLEGE_ANNEN_FNR).toBehandler(UserConstants.PARTNERID)
                    val existingBehandlerRef =
                        database.createBehandlerDialogmeldingForArbeidstaker(
                            behandler = behandler,
                            arbeidstakerPersonIdent = UserConstants.ARBEIDSTAKER_FNR
                        )
                    behandlerService.createOrGetBehandler(
                        behandler = annenBehandler,
                        arbeidstakerPersonIdent = UserConstants.ARBEIDSTAKER_FNR
                    )

                    val pBehandlerForArbeidstakerList = database.getBehandlerDialogmeldingForArbeidstaker(
                        UserConstants.ARBEIDSTAKER_FNR,
                    )
                    pBehandlerForArbeidstakerList.size shouldBeEqualTo 2
                    pBehandlerForArbeidstakerList[0].behandlerRef shouldNotBeEqualTo existingBehandlerRef
                    pBehandlerForArbeidstakerList[1].behandlerRef shouldBeEqualTo existingBehandlerRef
                }
                it("lagrer behandler for arbeidstaker når behandler er lagret for arbeidstaker men annen behandler er siste lagrede behandler for arbeidstaker") {
                    val behandler = generateFastlegeResponse(UserConstants.FASTLEGE_FNR).toBehandler(UserConstants.PARTNERID)
                    val annenBehandler =
                        generateFastlegeResponse(UserConstants.FASTLEGE_ANNEN_FNR).toBehandler(UserConstants.PARTNERID)
                    database.createBehandlerDialogmeldingForArbeidstaker(
                        behandler = behandler,
                        arbeidstakerPersonIdent = UserConstants.ARBEIDSTAKER_FNR
                    )
                    database.createBehandlerDialogmeldingForArbeidstaker(
                        behandler = annenBehandler,
                        arbeidstakerPersonIdent = UserConstants.ARBEIDSTAKER_FNR
                    )
                    behandlerService.createOrGetBehandler(
                        behandler = behandler,
                        arbeidstakerPersonIdent = UserConstants.ARBEIDSTAKER_FNR
                    )

                    val pBehandlerForArbeidstakerList = database.getBehandlerDialogmeldingForArbeidstaker(
                        UserConstants.ARBEIDSTAKER_FNR,
                    )
                    pBehandlerForArbeidstakerList.size shouldBeEqualTo 3
                    pBehandlerForArbeidstakerList[0].behandlerRef shouldBeEqualTo behandler.behandlerRef
                    pBehandlerForArbeidstakerList[1].behandlerRef shouldBeEqualTo annenBehandler.behandlerRef
                    pBehandlerForArbeidstakerList[2].behandlerRef shouldBeEqualTo behandler.behandlerRef
                }
                it("lagrer behandler for arbeidstaker når behandler har samme partnerId som annen behandler for annen arbeidstaker") {
                    val behandler = generateFastlegeResponse(UserConstants.FASTLEGE_FNR).toBehandler(UserConstants.PARTNERID)
                    val annenBehandler =
                        generateFastlegeResponse(UserConstants.FASTLEGE_ANNEN_FNR).toBehandler(UserConstants.PARTNERID)
                    database.createBehandlerDialogmeldingForArbeidstaker(
                        behandler = annenBehandler,
                        arbeidstakerPersonIdent = UserConstants.ANNEN_ARBEIDSTAKER_FNR
                    )
                    behandlerService.createOrGetBehandler(
                        behandler = behandler,
                        arbeidstakerPersonIdent = UserConstants.ARBEIDSTAKER_FNR
                    )

                    val pBehandlerForArbeidstakerList = database.getBehandlerDialogmeldingForArbeidstaker(
                        UserConstants.ARBEIDSTAKER_FNR,
                    )
                    pBehandlerForArbeidstakerList.size shouldBeEqualTo 1
                    pBehandlerForArbeidstakerList[0].behandlerRef shouldNotBeEqualTo annenBehandler.behandlerRef
                }
            }
        }
    }
})