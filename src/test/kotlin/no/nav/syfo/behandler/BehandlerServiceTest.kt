package no.nav.syfo.behandler

import io.mockk.clearMocks
import io.mockk.mockk
import no.nav.syfo.behandler.database.domain.toBehandler
import no.nav.syfo.behandler.database.getBehandlerByArbeidstaker
import no.nav.syfo.behandler.database.getBehandlerKontorById
import no.nav.syfo.behandler.database.updateBehandlerKontorDialogmeldingEnabled
import no.nav.syfo.behandler.database.updateBehandlerKontorSystem
import no.nav.syfo.behandler.domain.Arbeidstaker
import no.nav.syfo.behandler.domain.BehandlerArbeidstakerRelasjonstype
import no.nav.syfo.behandler.domain.BehandlerKontor
import no.nav.syfo.behandler.fastlege.FastlegeClient
import no.nav.syfo.behandler.fastlege.toBehandler
import no.nav.syfo.behandler.partnerinfo.PartnerinfoClient
import no.nav.syfo.testhelper.*
import no.nav.syfo.testhelper.generator.generateFastlegeResponse
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.OffsetDateTime

class BehandlerServiceTest {
    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val database = externalMockEnvironment.database
    private val fastlegeClientMock = mockk<FastlegeClient>()
    private val partnerinfoClientMock = mockk<PartnerinfoClient>()
    private val behandlerService = BehandlerService(
        fastlegeClient = fastlegeClientMock,
        partnerinfoClient = partnerinfoClientMock,
        database = database,
    )

    @AfterEach
    fun afterEach() {
        database.dropData()
        clearMocks(
            fastlegeClientMock,
            partnerinfoClientMock,
        )
    }

    @Test
    fun `lagrer behandler for arbeidstaker`() {
        val behandler =
            behandlerService.createOrGetBehandler(
                generateFastlegeResponse().toBehandler(UserConstants.PARTNERID),
                Arbeidstaker(
                    arbeidstakerPersonident = UserConstants.ARBEIDSTAKER_FNR,
                    mottatt = OffsetDateTime.now(),
                ),
                relasjonstype = BehandlerArbeidstakerRelasjonstype.FASTLEGE,
            )

        val pBehandlerList = database.getBehandlerByArbeidstaker(
            UserConstants.ARBEIDSTAKER_FNR,
        )
        assertEquals(1, pBehandlerList.size)
        assertEquals(behandler.behandlerRef, pBehandlerList[0].behandlerRef)
        val pBehandlerKontor = database.getBehandlerKontorById(pBehandlerList[0].kontorId)
        assertNotNull(pBehandlerKontor.dialogmeldingEnabled)
    }

    @Test
    fun `lagrer behandler for arbeidstaker og setter dialogmeldingEnabled senere`() {
        val behandler =
            behandlerService.createOrGetBehandler(
                generateFastlegeResponse().toBehandler(
                    partnerId = UserConstants.PARTNERID,
                    dialogmeldingEnabled = false,
                ),
                Arbeidstaker(
                    arbeidstakerPersonident = UserConstants.ARBEIDSTAKER_FNR,
                    mottatt = OffsetDateTime.now(),
                ),
                relasjonstype = BehandlerArbeidstakerRelasjonstype.FASTLEGE,
            )

        val pBehandlerList = database.getBehandlerByArbeidstaker(
            UserConstants.ARBEIDSTAKER_FNR,
        )
        assertEquals(1, pBehandlerList.size)
        val pBehandler = pBehandlerList[0]
        val behandlerFromDB = pBehandler.toBehandler(database.getBehandlerKontorById(pBehandler.kontorId))
        assertEquals(behandler.behandlerRef, behandlerFromDB.behandlerRef)
        assertFalse(behandlerFromDB.kontor.dialogmeldingEnabled)

        database.updateBehandlerKontorDialogmeldingEnabled(behandlerFromDB.kontor.partnerId)

        val behandlerFromDBUpdated = pBehandler.toBehandler(database.getBehandlerKontorById(pBehandler.kontorId))
        assertEquals(behandler.behandlerRef, behandlerFromDBUpdated.behandlerRef)
        assertTrue(behandlerFromDBUpdated.kontor.dialogmeldingEnabled)
    }

    @Test
    fun `Oppdaterer telefonnr på eksisterende behandler når telefonnr endret`() {
        val behandler =
            behandlerService.createOrGetBehandler(
                generateFastlegeResponse().toBehandler(
                    partnerId = UserConstants.PARTNERID,
                    dialogmeldingEnabled = false,
                ),
                Arbeidstaker(
                    arbeidstakerPersonident = UserConstants.ARBEIDSTAKER_FNR,
                    mottatt = OffsetDateTime.now(),
                ),
                relasjonstype = BehandlerArbeidstakerRelasjonstype.FASTLEGE,
            )

        val pBehandlerList = database.getBehandlerByArbeidstaker(
            UserConstants.ARBEIDSTAKER_FNR,
        )
        assertEquals(1, pBehandlerList.size)
        assertEquals("", pBehandlerList[0].telefon)
        behandlerService.createOrGetBehandler(
            behandler.copy(telefon = "987654321"),
            Arbeidstaker(
                arbeidstakerPersonident = UserConstants.ARBEIDSTAKER_FNR,
                mottatt = OffsetDateTime.now(),
            ),
            relasjonstype = BehandlerArbeidstakerRelasjonstype.FASTLEGE,
        )
        val pBehandlerListUpdated = database.getBehandlerByArbeidstaker(
            UserConstants.ARBEIDSTAKER_FNR,
        )
        assertEquals(1, pBehandlerListUpdated.size)
        assertEquals("987654321", pBehandlerListUpdated[0].telefon)
    }

    @Test
    fun `lagrer behandler for arbeidstaker og setter system senere`() {
        val behandler =
            behandlerService.createOrGetBehandler(
                generateFastlegeResponse().toBehandler(
                    partnerId = UserConstants.PARTNERID,
                    dialogmeldingEnabled = false,
                ),
                Arbeidstaker(
                    arbeidstakerPersonident = UserConstants.ARBEIDSTAKER_FNR,
                    mottatt = OffsetDateTime.now(),
                ),
                relasjonstype = BehandlerArbeidstakerRelasjonstype.FASTLEGE,
            )

        val pBehandlerList = database.getBehandlerByArbeidstaker(
            UserConstants.ARBEIDSTAKER_FNR,
        )
        assertEquals(1, pBehandlerList.size)
        val pBehandler = pBehandlerList[0]
        val behandlerFromDB = pBehandler.toBehandler(database.getBehandlerKontorById(pBehandler.kontorId))
        assertEquals(behandler.behandlerRef, behandlerFromDB.behandlerRef)
        assertNull(behandlerFromDB.kontor.system)

        database.connection.use {
            it.updateBehandlerKontorSystem(
                partnerId = behandlerFromDB.kontor.partnerId,
                kontor = BehandlerKontor(
                    partnerId = behandlerFromDB.kontor.partnerId,
                    herId = behandlerFromDB.kontor.herId,
                    navn = behandlerFromDB.kontor.navn,
                    adresse = behandlerFromDB.kontor.adresse,
                    postnummer = behandlerFromDB.kontor.postnummer,
                    poststed = behandlerFromDB.kontor.poststed,
                    orgnummer = behandlerFromDB.kontor.orgnummer,
                    dialogmeldingEnabled = behandlerFromDB.kontor.dialogmeldingEnabled,
                    dialogmeldingEnabledLocked = behandlerFromDB.kontor.dialogmeldingEnabledLocked,
                    system = "EPJ-systemet",
                    mottatt = OffsetDateTime.now(),
                ),
            )
            it.commit()
        }

        val behandlerFromDBUpdated = pBehandler.toBehandler(database.getBehandlerKontorById(pBehandler.kontorId))
        assertEquals(behandler.behandlerRef, behandlerFromDBUpdated.behandlerRef)
        assertEquals("EPJ-systemet", behandlerFromDBUpdated.kontor.system)
    }

    @Test
    fun `lagrer behandler for arbeidstaker én gang når kalt flere ganger for samme behandler og arbeidstaker`() {
        val behandler = generateFastlegeResponse().toBehandler(UserConstants.PARTNERID)
        behandlerService.createOrGetBehandler(
            behandler,
            Arbeidstaker(
                arbeidstakerPersonident = UserConstants.ARBEIDSTAKER_FNR,
                mottatt = OffsetDateTime.now(),
            ),
            relasjonstype = BehandlerArbeidstakerRelasjonstype.FASTLEGE,
        )
        behandlerService.createOrGetBehandler(
            behandler,
            Arbeidstaker(
                arbeidstakerPersonident = UserConstants.ARBEIDSTAKER_FNR,
                mottatt = OffsetDateTime.now(),
            ),
            relasjonstype = BehandlerArbeidstakerRelasjonstype.FASTLEGE,
        )
        val pBehandlerList = database.getBehandlerByArbeidstaker(
            UserConstants.ARBEIDSTAKER_FNR,
        )
        assertEquals(1, pBehandlerList.size)
    }

    @Test
    fun `oppretter ikke duplikate koblinger til arbeidstaker for suspendert behandler`() {
        val behandler = generateFastlegeResponse().toBehandler(UserConstants.PARTNERID)
        behandlerService.createOrGetBehandler(
            behandler,
            Arbeidstaker(
                arbeidstakerPersonident = UserConstants.ARBEIDSTAKER_FNR,
                mottatt = OffsetDateTime.now(),
            ),
            relasjonstype = BehandlerArbeidstakerRelasjonstype.FASTLEGE,
        )
        val pBehandlerListBefore = database.getBehandlerArbeidstakerRelasjoner(
            UserConstants.ARBEIDSTAKER_FNR,
        )
        assertEquals(1, pBehandlerListBefore.size)
        behandlerService.updateBehandlerSuspensjon(behandler.personident!!, true)
        behandlerService.createOrGetBehandler(
            behandler,
            Arbeidstaker(
                arbeidstakerPersonident = UserConstants.ARBEIDSTAKER_FNR,
                mottatt = OffsetDateTime.now(),
            ),
            relasjonstype = BehandlerArbeidstakerRelasjonstype.FASTLEGE,
        )
        val pBehandlerListAfter = database.getBehandlerArbeidstakerRelasjoner(
            UserConstants.ARBEIDSTAKER_FNR,
        )
        assertEquals(1, pBehandlerListAfter.size)
        val pBehandlerList = database.getBehandlerByArbeidstaker(
            UserConstants.ARBEIDSTAKER_FNR,
        )
        assertEquals(0, pBehandlerList.size)
    }

    @Test
    fun `lagrer én behandler koblet til begge arbeidstakere når kalt for to ulike arbeidstakere med samme behandler`() {
        val behandler = generateFastlegeResponse().toBehandler(UserConstants.PARTNERID)
        behandlerService.createOrGetBehandler(
            behandler,
            Arbeidstaker(
                arbeidstakerPersonident = UserConstants.ANNEN_ARBEIDSTAKER_FNR,
                mottatt = OffsetDateTime.now(),
            ),
            relasjonstype = BehandlerArbeidstakerRelasjonstype.FASTLEGE,
        )
        behandlerService.createOrGetBehandler(
            behandler,
            Arbeidstaker(
                arbeidstakerPersonident = UserConstants.ARBEIDSTAKER_FNR,
                mottatt = OffsetDateTime.now(),
            ),
            relasjonstype = BehandlerArbeidstakerRelasjonstype.FASTLEGE,
        )

        val pBehandlerForArbeidstakerList = database.getBehandlerByArbeidstaker(
            UserConstants.ARBEIDSTAKER_FNR,
        )
        val pBehandlerForAnnenArbeidstakerList = database.getBehandlerByArbeidstaker(
            UserConstants.ANNEN_ARBEIDSTAKER_FNR,
        )
        assertEquals(1, pBehandlerForArbeidstakerList.size)
        assertEquals(1, pBehandlerForAnnenArbeidstakerList.size)
        assertEquals(pBehandlerForAnnenArbeidstakerList[0].behandlerRef, pBehandlerForArbeidstakerList[0].behandlerRef)
    }

    @Test
    fun `lagrer behandler uten fnr`() {
        val behandler =
            generateFastlegeResponse(
                null,
                UserConstants.HERID,
                UserConstants.HPRID
            ).toBehandler(UserConstants.PARTNERID)
        behandlerService.createOrGetBehandler(
            behandler,
            Arbeidstaker(
                arbeidstakerPersonident = UserConstants.ARBEIDSTAKER_FNR,
                mottatt = OffsetDateTime.now(),
            ),
            relasjonstype = BehandlerArbeidstakerRelasjonstype.FASTLEGE,
        )

        assertEquals(
            1,
            database.getBehandlerByArbeidstaker(
                UserConstants.ARBEIDSTAKER_FNR,
            ).size
        )
    }

    @Test
    fun `lagrer behandler uten herId`() {
        val behandler =
            generateFastlegeResponse(
                UserConstants.FASTLEGE_FNR,
                null,
                UserConstants.HPRID
            ).toBehandler(UserConstants.PARTNERID)
        behandlerService.createOrGetBehandler(
            behandler,
            Arbeidstaker(
                arbeidstakerPersonident = UserConstants.ARBEIDSTAKER_FNR,
                mottatt = OffsetDateTime.now(),
            ),
            relasjonstype = BehandlerArbeidstakerRelasjonstype.FASTLEGE,
        )

        assertEquals(
            1,
            database.getBehandlerByArbeidstaker(
                UserConstants.ARBEIDSTAKER_FNR,
            ).size
        )
    }

    @Test
    fun `lagrer behandler uten hprId`() {
        val behandler =
            generateFastlegeResponse(
                UserConstants.FASTLEGE_FNR,
                UserConstants.HERID,
                null
            ).toBehandler(UserConstants.PARTNERID)
        behandlerService.createOrGetBehandler(
            behandler,
            Arbeidstaker(
                arbeidstakerPersonident = UserConstants.ARBEIDSTAKER_FNR,
                mottatt = OffsetDateTime.now(),
            ),
            relasjonstype = BehandlerArbeidstakerRelasjonstype.FASTLEGE,
        )

        assertEquals(
            1,
            database.getBehandlerByArbeidstaker(
                UserConstants.ARBEIDSTAKER_FNR,
            ).size
        )
    }

    @Test
    fun `lagrer ikke behandler uten fnr, herId og hprId`() {
        val behandler =
            generateFastlegeResponse(null, null, null).toBehandler(UserConstants.PARTNERID)

        assertThrows<IllegalArgumentException> {
            behandlerService.createOrGetBehandler(
                behandler,
                Arbeidstaker(
                    arbeidstakerPersonident = UserConstants.ARBEIDSTAKER_FNR,
                    mottatt = OffsetDateTime.now(),
                ),
                relasjonstype = BehandlerArbeidstakerRelasjonstype.FASTLEGE,
            )
        }
        assertEquals(
            0,
            database.getBehandlerByArbeidstaker(
                UserConstants.ARBEIDSTAKER_FNR,
            ).size
        )
    }

    @Test
    fun `lagrer ikke behandler for arbeidstaker når samme behandler er siste lagrede behandler for arbeidstaker`() {
        val behandler = generateFastlegeResponse().toBehandler(UserConstants.PARTNERID)
        val existingBehandlerRef =
            database.createBehandlerForArbeidstaker(
                behandler = behandler,
                arbeidstakerPersonident = UserConstants.ARBEIDSTAKER_FNR
            )

        behandlerService.createOrGetBehandler(
            behandler,
            Arbeidstaker(
                arbeidstakerPersonident = UserConstants.ARBEIDSTAKER_FNR,
                mottatt = OffsetDateTime.now(),
            ),
            relasjonstype = BehandlerArbeidstakerRelasjonstype.FASTLEGE,
        )

        val pBehandlerForArbeidstakerList = database.getBehandlerByArbeidstaker(
            UserConstants.ARBEIDSTAKER_FNR,
        )
        assertEquals(1, pBehandlerForArbeidstakerList.size)
        assertEquals(existingBehandlerRef, pBehandlerForArbeidstakerList[0].behandlerRef)
    }

    @Test
    fun `lagrer behandler for arbeidstaker når samme behandler er lagret for annen arbeidstaker`() {
        val behandler = generateFastlegeResponse().toBehandler(UserConstants.PARTNERID)
        val existingBehandlerRef =
            database.createBehandlerForArbeidstaker(
                behandler = behandler,
                arbeidstakerPersonident = UserConstants.ANNEN_ARBEIDSTAKER_FNR
            )
        behandlerService.createOrGetBehandler(
            behandler,
            Arbeidstaker(
                arbeidstakerPersonident = UserConstants.ARBEIDSTAKER_FNR,
                mottatt = OffsetDateTime.now(),
            ),
            relasjonstype = BehandlerArbeidstakerRelasjonstype.FASTLEGE,
        )

        val pBehandlerForArbeidstakerList = database.getBehandlerByArbeidstaker(
            UserConstants.ARBEIDSTAKER_FNR,
        )
        assertEquals(1, pBehandlerForArbeidstakerList.size)
        assertEquals(existingBehandlerRef, pBehandlerForArbeidstakerList[0].behandlerRef)
    }

    @Test
    fun `lagrer behandler for arbeidstaker når fastlege er annen enn siste lagrede fastlege for arbeidstaker`() {
        val behandler = generateFastlegeResponse(UserConstants.FASTLEGE_FNR).toBehandler(UserConstants.PARTNERID)
        val annenBehandler =
            generateFastlegeResponse(UserConstants.FASTLEGE_ANNEN_FNR).toBehandler(UserConstants.PARTNERID)
        val existingBehandlerRef =
            database.createBehandlerForArbeidstaker(
                behandler = behandler,
                arbeidstakerPersonident = UserConstants.ARBEIDSTAKER_FNR
            )
        behandlerService.createOrGetBehandler(
            behandler = annenBehandler,
            Arbeidstaker(
                arbeidstakerPersonident = UserConstants.ARBEIDSTAKER_FNR,
                mottatt = OffsetDateTime.now(),
            ),
            relasjonstype = BehandlerArbeidstakerRelasjonstype.FASTLEGE,
        )

        val pBehandlerForArbeidstakerList = database.getBehandlerByArbeidstaker(
            UserConstants.ARBEIDSTAKER_FNR,
        )
        assertEquals(2, pBehandlerForArbeidstakerList.size)
        assertNotEquals(existingBehandlerRef, pBehandlerForArbeidstakerList[0].behandlerRef)
        assertEquals(existingBehandlerRef, pBehandlerForArbeidstakerList[1].behandlerRef)
    }

    @Test
    fun `lagrer behandler for arbeidstaker når fastlege er lagret for arbeidstaker men annen behandler er siste lagrede fastlege for arbeidstaker`() {
        val behandler = generateFastlegeResponse(UserConstants.FASTLEGE_FNR).toBehandler(UserConstants.PARTNERID)
        val annenBehandler =
            generateFastlegeResponse(UserConstants.FASTLEGE_ANNEN_FNR).toBehandler(UserConstants.PARTNERID)
        database.createBehandlerForArbeidstaker(
            behandler = behandler,
            arbeidstakerPersonident = UserConstants.ARBEIDSTAKER_FNR
        )
        database.createBehandlerForArbeidstaker(
            behandler = annenBehandler,
            arbeidstakerPersonident = UserConstants.ARBEIDSTAKER_FNR
        )
        behandlerService.createOrGetBehandler(
            behandler,
            Arbeidstaker(
                arbeidstakerPersonident = UserConstants.ARBEIDSTAKER_FNR,
                mottatt = OffsetDateTime.now(),
            ),
            relasjonstype = BehandlerArbeidstakerRelasjonstype.FASTLEGE,
        )

        val pBehandlerForArbeidstakerList = database.getBehandlerByArbeidstaker(
            UserConstants.ARBEIDSTAKER_FNR,
        )
        assertEquals(3, pBehandlerForArbeidstakerList.size)
        assertEquals(behandler.behandlerRef, pBehandlerForArbeidstakerList[0].behandlerRef)
        assertEquals(annenBehandler.behandlerRef, pBehandlerForArbeidstakerList[1].behandlerRef)
        assertEquals(behandler.behandlerRef, pBehandlerForArbeidstakerList[2].behandlerRef)
    }

    @Test
    fun `lagrer behandler for arbeidstaker når behandler har samme partnerId som annen behandler for annen arbeidstaker`() {
        val behandler = generateFastlegeResponse(UserConstants.FASTLEGE_FNR).toBehandler(UserConstants.PARTNERID)
        val annenBehandler =
            generateFastlegeResponse(UserConstants.FASTLEGE_ANNEN_FNR).toBehandler(UserConstants.PARTNERID)
        database.createBehandlerForArbeidstaker(
            behandler = annenBehandler,
            arbeidstakerPersonident = UserConstants.ANNEN_ARBEIDSTAKER_FNR
        )
        behandlerService.createOrGetBehandler(
            behandler,
            Arbeidstaker(
                arbeidstakerPersonident = UserConstants.ARBEIDSTAKER_FNR,
                mottatt = OffsetDateTime.now(),
            ),
            relasjonstype = BehandlerArbeidstakerRelasjonstype.FASTLEGE,
        )

        val pBehandlerForArbeidstakerList = database.getBehandlerByArbeidstaker(
            UserConstants.ARBEIDSTAKER_FNR,
        )
        assertEquals(1, pBehandlerForArbeidstakerList.size)
        assertNotEquals(annenBehandler.behandlerRef, pBehandlerForArbeidstakerList[0].behandlerRef)
    }

    @Test
    fun `lagrer behandler for arbeidstaker når samme behandler er lagret for arbeidstaker, men med annen partnerId`() {
        val behandler = generateFastlegeResponse(UserConstants.FASTLEGE_FNR).toBehandler(UserConstants.PARTNERID)
        val sammeBehandlerAnnenPartnerId =
            generateFastlegeResponse(UserConstants.FASTLEGE_FNR).toBehandler(UserConstants.OTHER_PARTNERID)
        val existingBehandlerRef =
            database.createBehandlerForArbeidstaker(
                behandler = behandler,
                arbeidstakerPersonident = UserConstants.ARBEIDSTAKER_FNR
            )

        behandlerService.createOrGetBehandler(
            behandler = sammeBehandlerAnnenPartnerId,
            Arbeidstaker(
                arbeidstakerPersonident = UserConstants.ARBEIDSTAKER_FNR,
                mottatt = OffsetDateTime.now(),
            ),
            relasjonstype = BehandlerArbeidstakerRelasjonstype.FASTLEGE,
        )

        val pBehandlerForArbeidstakerList = database.getBehandlerByArbeidstaker(
            UserConstants.ARBEIDSTAKER_FNR,
        )
        assertEquals(2, pBehandlerForArbeidstakerList.size)
        assertNotEquals(existingBehandlerRef, pBehandlerForArbeidstakerList[0].behandlerRef)
        assertEquals(existingBehandlerRef, pBehandlerForArbeidstakerList[1].behandlerRef)
    }
}
