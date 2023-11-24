package no.nav.syfo.behandler

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.behandler.database.*
import no.nav.syfo.behandler.database.domain.*
import no.nav.syfo.behandler.domain.*
import no.nav.syfo.behandler.fastlege.FastlegeClient
import no.nav.syfo.behandler.fastlege.toBehandler
import no.nav.syfo.behandler.partnerinfo.PartnerinfoClient
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
            createOrGetBehandler(
                behandler = fastlege,
                arbeidstaker = arbeidstaker,
                relasjonstype = BehandlerArbeidstakerRelasjonstype.FASTLEGE,
            )
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
            createOrGetBehandler(
                behandler = vikar,
                arbeidstaker = arbeidstaker,
                relasjonstype = BehandlerArbeidstakerRelasjonstype.FASTLEGEVIKAR,
            )
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
            log.warn("Aktiv fastlege missing foreldreEnhetHerId so cannot request partnerinfo")
            return null
        }

        val partnerinfoResponse = partnerinfoClient.partnerinfo(
            herId = fastlegeResponse.foreldreEnhetHerId.toString(),
            systemRequest = systemRequest,
            token = token,
            callId = callId,
        )

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
        )

        return if (partnerinfoResponse != null) {
            fastlegeResponse.toBehandler(
                partnerId = PartnerId(partnerinfoResponse.partnerId),
            )
        } else null
    }

    fun createOrGetBehandler(
        behandler: Behandler,
        arbeidstaker: Arbeidstaker,
        relasjonstype: BehandlerArbeidstakerRelasjonstype,
    ): Behandler {
        val pBehandler = getBehandler(behandler = behandler)
        if (pBehandler == null) {
            return createBehandlerForArbeidstaker(
                behandler = behandler,
                arbeidstaker = arbeidstaker,
                relasjonstype = relasjonstype,
            )
        } else {
            updateBehandler(
                behandler = behandler,
            )
        }

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

        return pBehandler.toBehandler(
            kontor = database.getBehandlerKontorById(pBehandler.kontorId)
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
                connection.updateBehandlerKontor(
                    behandler = behandler,
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

    private fun updateBehandler(
        behandler: Behandler,
    ) {
        database.connection.use { connection ->
            val existingBehandlerKontor = connection.getBehandlerKontor(behandler.kontor.partnerId)!!
            connection.updateBehandlerKontor(
                behandler = behandler,
                existingBehandlerKontor = existingBehandlerKontor,
            )
            connection.commit()
        }
    }

    private fun Connection.updateBehandlerKontor(
        behandler: Behandler,
        existingBehandlerKontor: PBehandlerKontor,
    ) {
        if (shouldUpdateKontorSystem(behandler.kontor, existingBehandlerKontor)) {
            updateBehandlerKontorSystem(behandler.kontor.partnerId, behandler.kontor)
        }
        if (shouldUpdateKontorAdresse(behandler.kontor, existingBehandlerKontor)) {
            updateBehandlerKontorAddress(behandler.kontor.partnerId, behandler.kontor)
        }
        if (
            behandler.kontor.herId != null &&
            behandler.kontor.herId.toString() != existingBehandlerKontor.herId &&
            existingBehandlerKontor.dialogmeldingEnabled != null
        ) {
            log.error(
                "Persistert behandlerkontor har muligens feil herId ${existingBehandlerKontor.herId}: " +
                    "Sjekk kontor med partnerId ${existingBehandlerKontor.partnerId}." +
                    "Adresseregisteret returnerte overordnet herId ${behandler.kontor.herId} for behandler ${behandler.behandlerRef}"
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
}
