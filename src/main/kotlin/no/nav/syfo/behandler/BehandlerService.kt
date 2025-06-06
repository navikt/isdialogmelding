package no.nav.syfo.behandler

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.behandler.api.behandlerPersonident
import no.nav.syfo.behandler.database.*
import no.nav.syfo.behandler.database.domain.*
import no.nav.syfo.behandler.domain.*
import no.nav.syfo.behandler.fastlege.FastlegeClient
import no.nav.syfo.behandler.fastlege.toBehandler
import no.nav.syfo.behandler.partnerinfo.PartnerinfoClient
import no.nav.syfo.behandler.partnerinfo.PartnerinfoResponse
import no.nav.syfo.domain.*
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.time.OffsetDateTime
import java.util.*

private val log = LoggerFactory.getLogger("no.nav.syfo.behandler")

class BehandlerService(
    private val fastlegeClient: FastlegeClient,
    private val partnerinfoClient: PartnerinfoClient,
    private val database: DatabaseInterface,
) {
    suspend fun getBehandlere(
        personident: Personident,
        token: String,
        callId: String,
        systemRequest: Boolean = false,
    ): List<Pair<Behandler, BehandlerArbeidstakerRelasjonstype>> {
        val behandlere = mutableListOf<Pair<Behandler, BehandlerArbeidstakerRelasjonstype>>()

        val fastlegeBehandler = getFastlegeBehandler(
            personident = personident,
            systemRequest = systemRequest,
            token = token,
            callId = callId,
        )
        fastlegeBehandler?.let { behandlere.add(Pair(it, BehandlerArbeidstakerRelasjonstype.FASTLEGE)) }

        database.getSykmeldereExtended(personident)
            .forEach { (pBehandler, pBehandlerKontor) ->
                behandlere.add(
                    Pair(
                        pBehandler.toBehandler(pBehandlerKontor),
                        BehandlerArbeidstakerRelasjonstype.SYKMELDER,
                    )
                )
            }
        return behandlere.removeDuplicates()
    }

    fun searchBehandlere(
        searchStrings: String,
    ): List<Behandler> {
        val tokens = searchStrings.split(" ")
            .filter { s -> s.isNotBlank() }
            .map { s ->
                s
                    .replace(",", "")
                    .replace(".", "")
                    .replace(":", "")
                    .trim()
            }
            .filter { s -> s.length > 2 }
        return database.searchBehandler(
            searchStrings = tokens,
        ).map { pair ->
            pair.first.toBehandler(pair.second)
        }
    }

    suspend fun getFastlegeBehandler(
        personident: Personident,
        token: String,
        callId: String,
        systemRequest: Boolean = false,
    ): Behandler? {
        val fastlege = getAktivFastlegeBehandlerMedKontor(
            personident = personident,
            token = token,
            callId = callId,
            systemRequest = systemRequest,
        )
        return if (fastlege != null && fastlege.hasAnId()) {
            val arbeidstaker = Arbeidstaker(
                arbeidstakerPersonident = personident,
                mottatt = OffsetDateTime.now(),
            )
            val behandler = createOrGetBehandler(
                behandler = fastlege,
                arbeidstaker = arbeidstaker,
                relasjonstype = BehandlerArbeidstakerRelasjonstype.FASTLEGE,
            )
            if (behandler.suspendert) null else behandler
        } else null
    }

    suspend fun getFastlegevikarBehandler(
        personident: Personident,
        token: String,
        callId: String,
    ): Behandler? {
        val vikar = getFastlegevikarBehandlerMedKontor(
            personident = personident,
            token = token,
            callId = callId,
        )
        return if (vikar != null && vikar.hasAnId()) {
            val arbeidstaker = Arbeidstaker(
                arbeidstakerPersonident = personident,
                mottatt = OffsetDateTime.now(),
            )
            val behandler = createOrGetBehandler(
                behandler = vikar,
                arbeidstaker = arbeidstaker,
                relasjonstype = BehandlerArbeidstakerRelasjonstype.FASTLEGEVIKAR,
            )
            if (behandler.suspendert) null else behandler
        } else null
    }

    private suspend fun getAktivFastlegeBehandlerMedKontor(
        personident: Personident,
        token: String,
        callId: String,
        systemRequest: Boolean = false,
    ): Behandler? {
        val fastlegeResponse = fastlegeClient.fastlege(
            personident = personident,
            systemRequest = systemRequest,
            token = token,
            callId = callId,
        ) ?: return null

        if (fastlegeResponse.foreldreEnhetHerId == null) {
            log.warn("Aktiv fastlege missing foreldreEnhetHerId so cannot request partnerinfo: ${fastlegeResponse.herId}")
            return null
        }

        val partnerinfoResponse = partnerinfoClient.partnerinfo(
            herId = fastlegeResponse.foreldreEnhetHerId.toString(),
            systemRequest = systemRequest,
            token = token,
            callId = callId,
        ).selectActiveBehandlerKontor()

        return if (partnerinfoResponse != null) {
            fastlegeResponse.toBehandler(
                partnerId = PartnerId(partnerinfoResponse.partnerId),
            )
        } else null
    }

    private suspend fun getFastlegevikarBehandlerMedKontor(
        personident: Personident,
        token: String,
        callId: String,
    ): Behandler? {
        val fastlegeResponse = fastlegeClient.fastlegevikar(
            personident = personident,
            callId = callId,
        ) ?: return null

        if (fastlegeResponse.foreldreEnhetHerId == null) {
            log.warn("Vikar missing foreldreEnhetHerId so cannot request partnerinfo")
            return null
        }

        val partnerinfoResponse = partnerinfoClient.partnerinfo(
            herId = fastlegeResponse.foreldreEnhetHerId.toString(),
            systemRequest = true,
            token = token,
            callId = callId,
        ).selectActiveBehandlerKontor()

        return if (partnerinfoResponse != null) {
            fastlegeResponse.toBehandler(
                partnerId = PartnerId(partnerinfoResponse.partnerId),
            )
        } else null
    }

    fun createBehandlerKontorIfMissing(
        behandlerKontor: BehandlerKontor,
    ) {
        database.connection.use { connection ->
            val pBehandlerKontor = connection.getBehandlerKontor(behandlerKontor.partnerId)
            if (pBehandlerKontor == null) {
                connection.createBehandlerKontor(behandlerKontor)
            } else {
                connection.updateBehandlerKontorSystemAndAdresse(behandlerKontor, pBehandlerKontor)
            }
            connection.commit()
        }
    }

    fun createOrGetBehandler(
        behandler: Behandler,
        arbeidstaker: Arbeidstaker,
        relasjonstype: BehandlerArbeidstakerRelasjonstype,
        disableCreate: Boolean = false,
    ): Behandler {
        val pBehandler = getBehandler(behandler = behandler)
        if (pBehandler == null) {
            return if (!disableCreate) {
                createBehandlerForArbeidstaker(
                    behandler = behandler,
                    arbeidstaker = arbeidstaker,
                    relasjonstype = relasjonstype,
                )
            } else {
                behandler
            }
        } else {
            updateBehandlerTelefon(
                behandler = behandler,
                existingBehandler = pBehandler,
            )
        }
        if (!pBehandler.suspendert) {
            val pBehandlereForArbeidstakerList =
                database.getBehandlerAndRelasjonstypeList(
                    arbeidstakerIdent = arbeidstaker.arbeidstakerPersonident,
                )

            val isBytteAvFastlege = relasjonstype == BehandlerArbeidstakerRelasjonstype.FASTLEGE && pBehandlereForArbeidstakerList
                .filter { (_, behandlerType) -> behandlerType == BehandlerArbeidstakerRelasjonstype.FASTLEGE }
                .map { (pBehandler, _) -> pBehandler.id }.firstOrNull() != pBehandler.id

            val behandlerIkkeKnyttetTilArbeidstaker = !pBehandlereForArbeidstakerList
                .filter { (_, behandlerType) -> behandlerType == relasjonstype }
                .map { (pBehandler, _) -> pBehandler.id }.contains(pBehandler.id)

            if (isBytteAvFastlege || behandlerIkkeKnyttetTilArbeidstaker) {
                addBehandlerToArbeidstaker(
                    arbeidstaker = arbeidstaker,
                    relasjonstype = relasjonstype,
                    behandlerId = pBehandler.id,
                )
            } else if (relasjonstype == BehandlerArbeidstakerRelasjonstype.SYKMELDER) {
                database.updateBehandlerArbeidstakerRelasjon(
                    arbeidstaker = arbeidstaker,
                    relasjonstype = relasjonstype,
                    behandlerId = pBehandler.id,
                )
            }
        }

        return pBehandler.toBehandler(
            kontor = database.getBehandlerKontorById(pBehandler.kontorId)
        )
    }

    fun createBehandler(
        behandler: Behandler,
        kontorId: Int,
    ) {
        database.createBehandler(
            behandler = behandler,
            kontorId = kontorId,
        )
    }

    fun getBehandler(behandlerRef: UUID): Behandler? {
        val pBehandler = database.getBehandlerByBehandlerRef(
            behandlerRef = behandlerRef,
        )
        return pBehandler?.toBehandler(
            kontor = database.getBehandlerKontorById(pBehandler.kontorId)
        )
    }

    fun getKontor(): List<PBehandlerKontor> =
        database.getAllBehandlerKontor()

    fun getBehandlereForKontor(kontor: PBehandlerKontor): List<PBehandler> =
        database.getBehandlereForKontor(kontor.id)

    fun getBehandlerPersonidenterForAktiveKontor(): List<Personident> =
        database.getBehandlerPersonidenterForAktiveKontor().filterNot { it.isNullOrBlank() }.map { Personident(it) }

    fun updateBehandlerSuspensjon(behandlerPersonident: Personident, suspendert: Boolean) {
        database.updateSuspensjon(behandlerPersonident, suspendert)
    }

    fun updateBehandlerIdenter(behandlerRef: UUID, identer: Map<BehandleridentType, String>) {
        database.updateBehandlerIdenter(behandlerRef, identer)
    }

    fun updateBehandlerPersonident(behandlerRef: UUID, personident: String) {
        database.updateBehandlerPersonident(behandlerRef, personident)
    }

    fun updateBehandlerNavnAndKategoriAndHerId(
        behandlerRef: UUID,
        fornavn: String,
        mellomnavn: String?,
        etternavn: String,
        kategori: BehandlerKategori?,
        herId: String,
    ) {
        database.updateBehandlerNavnAndKategoriAndHerId(
            behandlerRef = behandlerRef,
            fornavn = fornavn,
            mellomnavn = mellomnavn,
            etternavn = etternavn,
            kategori = kategori,
            herId = herId,
        )
    }

    fun invalidateBehandler(behandlerRef: UUID) {
        database.invalidateBehandler(behandlerRef)
    }

    fun revalidateBehandler(behandlerRef: UUID) {
        database.revalidateBehandler(behandlerRef)
    }

    fun existsOtherValidKontorWithSameHerId(
        behandlerKontor: PBehandlerKontor,
        partnerIds: List<Int>,
    ): Boolean {
        return behandlerKontor.herId?.let {
            database.getBehandlerKontorByHerId(it).any { other ->
                other.partnerId != behandlerKontor.partnerId && partnerIds.contains(other.partnerId.toInt())
            }
        } ?: false
    }

    fun disableDialogmeldingerForKontor(behandlerKontor: PBehandlerKontor) {
        database.updateBehandlerKontorDialogmeldingDisabled(PartnerId(behandlerKontor.partnerId.toInt()))
    }

    private fun getBehandler(behandler: Behandler): PBehandler? {
        return when {
            behandler.personident != null -> database.getBehandlerByBehandlerPersonidentAndPartnerId(
                behandlerPersonident = behandler.personident,
                partnerId = behandler.kontor.partnerId,
            )
            behandler.hprId != null -> database.getBehandlerByHprIdAndPartnerId(
                hprId = behandler.hprId,
                partnerId = behandler.kontor.partnerId,
            )
            behandler.herId != null -> database.getBehandlerByHerIdAndPartnerId(
                herId = behandler.herId,
                partnerId = behandler.kontor.partnerId,
            )
            else -> throw IllegalArgumentException("Behandler missing personident, hprId and herId")
        }
    }

    private fun createBehandlerForArbeidstaker(
        arbeidstaker: Arbeidstaker,
        relasjonstype: BehandlerArbeidstakerRelasjonstype,
        behandler: Behandler,
    ): Behandler {
        database.connection.use { connection ->
            val pBehandlerKontor = connection.getBehandlerKontor(behandler.kontor.partnerId)
            val kontorId = if (pBehandlerKontor != null) {
                connection.updateBehandlerKontorSystemAndAdresse(
                    behandlerKontor = behandler.kontor,
                    existingBehandlerKontor = pBehandlerKontor,
                )
                pBehandlerKontor.id
            } else {
                connection.createBehandlerKontor(behandler.kontor)
            }
            val pBehandler = connection.createBehandler(
                behandler = behandler,
                kontorId = kontorId,
            )
            connection.createBehandlerArbeidstakerRelasjon(
                arbeidstaker = arbeidstaker,
                relasjonstype = relasjonstype,
                behandlerId = pBehandler.id,
            )
            connection.commit()

            return pBehandler.toBehandler(
                database.getBehandlerKontorById(pBehandler.kontorId)
            )
        }
    }

    private fun updateBehandlerTelefon(
        behandler: Behandler,
        existingBehandler: PBehandler,
    ) {
        database.connection.use { connection ->
            if (behandler.telefon?.isNotBlank() == true && behandler.telefon != existingBehandler.telefon) {
                connection.updateBehandlerTelefon(
                    id = existingBehandler.id,
                    telefon = behandler.telefon,
                )
                COUNT_BEHANDLER_UPDATED.increment()
            }
            val existingBehandlerKontor = connection.getBehandlerKontor(behandler.kontor.partnerId)!!
            connection.updateBehandlerKontorSystemAndAdresse(
                behandlerKontor = behandler.kontor,
                existingBehandlerKontor = existingBehandlerKontor,
                existingBehandler = existingBehandler,
            )
            connection.commit()
        }
    }

    fun updateBehandlerKontorSystemAndAdresse(
        behandlerKontor: BehandlerKontor,
    ) {
        database.connection.use { connection ->
            connection.getBehandlerKontor(behandlerKontor.partnerId)?.let { existingBehandlerKontor ->
                connection.updateBehandlerKontorSystemAndAdresse(behandlerKontor, existingBehandlerKontor)
            }
            connection.commit()
        }
    }

    private fun Connection.updateBehandlerKontorSystemAndAdresse(
        behandlerKontor: BehandlerKontor,
        existingBehandlerKontor: PBehandlerKontor,
        existingBehandler: PBehandler? = null,

    ) {
        if (shouldUpdateKontorSystem(behandlerKontor, existingBehandlerKontor)) {
            updateBehandlerKontorSystem(behandlerKontor.partnerId, behandlerKontor)
        }
        if (shouldUpdateKontorAdresse(behandlerKontor, existingBehandlerKontor)) {
            updateBehandlerKontorAddress(behandlerKontor.partnerId, behandlerKontor)
        }
        if (
            behandlerKontor.herId != null &&
            behandlerKontor.herId.toString() != existingBehandlerKontor.herId &&
            existingBehandlerKontor.dialogmeldingEnabled != null
        ) {
            log.warn(
                "Persistert behandlerkontor har muligens feil herId ${existingBehandlerKontor.herId}: " +
                    "Sjekk kontor med partnerId ${existingBehandlerKontor.partnerId}." +
                    "Adresseregisteret returnerte overordnet herId ${behandlerKontor.herId} for behandler id ${existingBehandler?.id}"
            )
        }
    }

    private fun shouldUpdateKontorSystem(
        behandlerKontor: BehandlerKontor,
        existingBehandlerKontor: PBehandlerKontor,
    ): Boolean =
        !behandlerKontor.system.isNullOrBlank() && (
            existingBehandlerKontor.system.isNullOrBlank() ||
                behandlerKontor.mottatt.isAfter(existingBehandlerKontor.mottatt)
            )

    private fun shouldUpdateKontorAdresse(
        behandlerKontor: BehandlerKontor,
        existingBehandlerKontor: PBehandlerKontor,
    ): Boolean =
        behandlerKontor.harKomplettAdresse() && behandlerKontor.mottatt.isAfter(existingBehandlerKontor.mottatt)

    private fun addBehandlerToArbeidstaker(
        arbeidstaker: Arbeidstaker,
        relasjonstype: BehandlerArbeidstakerRelasjonstype,
        behandlerId: Int,
    ) {
        database.connection.use { connection ->
            connection.createBehandlerArbeidstakerRelasjon(
                arbeidstaker = arbeidstaker,
                relasjonstype = relasjonstype,
                behandlerId = behandlerId,
            )
            connection.commit()
        }
    }

    private fun List<PartnerinfoResponse>.selectActiveBehandlerKontor(): PartnerinfoResponse? =
        if (this.size < 2)
            this.firstOrNull()
        else {
            this.selectBehandlerKontorWithLatestDialogmeldingEnabled()
        } ?: this.maxByOrNull { it.partnerId }

    private fun List<PartnerinfoResponse>.selectBehandlerKontorWithLatestDialogmeldingEnabled(): PartnerinfoResponse? {
        val pBehandlerKontor = this.mapNotNull { partnerInfoResponse ->
            database.connection.use { it.getBehandlerKontor(PartnerId(partnerInfoResponse.partnerId)) }
        }.filter {
            it.dialogmeldingEnabled != null
        }.maxByOrNull {
            it.dialogmeldingEnabled!!
        }
        return if (pBehandlerKontor != null) {
            this.first { it.partnerId == pBehandlerKontor.partnerId.toInt() }
        } else null
    }
}
