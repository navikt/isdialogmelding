package no.nav.syfo.cronjob

import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.behandler.BehandlerService
import no.nav.syfo.behandler.database.domain.PBehandler
import no.nav.syfo.behandler.database.domain.PBehandlerKontor
import no.nav.syfo.behandler.database.domain.toBehandlerKontor
import no.nav.syfo.behandler.domain.Behandler
import no.nav.syfo.behandler.domain.BehandlerKategori
import no.nav.syfo.behandler.domain.BehandleridentType
import no.nav.syfo.behandler.fastlege.BehandlerKontorFraAdresseregisteretDTO
import no.nav.syfo.behandler.fastlege.FastlegeClient
import no.nav.syfo.client.syfohelsenettproxy.SyfohelsenettproxyClient
import no.nav.syfo.domain.Personident
import no.nav.syfo.domain.isDNR
import no.nav.syfo.util.nowUTC
import org.slf4j.LoggerFactory
import java.time.DayOfWeek
import java.util.UUID

const val ALERIS_HER_ID = "2033"

class VerifyBehandlereForKontorCronjob(
    val behandlerService: BehandlerService,
    val fastlegeClient: FastlegeClient,
    val syfohelsenettproxyClient: SyfohelsenettproxyClient,
) : DialogmeldingCronjob {
    private val runAtHour = 6
    private val runDay = DayOfWeek.SUNDAY

    override val initialDelayMinutes: Long = calculateWeeklyInitialDelay("VerifyBehandlereForKontorCronjob", runDay, runAtHour)
    override val intervalDelayMinutes: Long = 24 * 60 * 7

    override suspend fun run() {
        verifyBehandlereForKontorJob()
    }

    suspend fun verifyBehandlereForKontorJob() {
        val verifyResult = DialogmeldingCronjobResult()

        val behandlerKontorListe = behandlerService.getKontor().filter {
            it.herId != null && it.dialogmeldingEnabled != null && it.herId != ALERIS_HER_ID
        }
        behandlerKontorListe.forEach { behandlerKontor ->
            try {
                val start = System.currentTimeMillis()
                log.info("VerifyBehandlereForKontorCronjob: Starter henting av behandlere for ${behandlerKontor.herId} fra Adresseregisteret")
                val behandlerKontorFraAdresseregisteret = fastlegeClient.behandlereForKontor(
                    callId = UUID.randomUUID().toString(),
                    kontorHerId = behandlerKontor.herId!!.toInt()
                )
                log.info("VerifyBehandlereForKontorCronjob: Behandlere for ${behandlerKontor.herId} hentet fra Adresseregisteret, brukte ${System.currentTimeMillis() - start} ms")
                if (behandlerKontorFraAdresseregisteret != null) {
                    if (!behandlerKontorFraAdresseregisteret.aktiv) {
                        log.info("VerifyBehandlereForKontorCronjob: Disable dialogmelding for kontor siden inaktiv i Adresseregisteret: herId ${behandlerKontor.herId} partnerId ${behandlerKontor.partnerId}")
                        behandlerService.disableDialogmeldingerForKontor(behandlerKontor)
                    } else {
                        val (aktiveBehandlereForKontor, inaktiveBehandlereForKontor) = behandlerKontorFraAdresseregisteret.behandlere.partition { it.aktiv }
                        val existingBehandlereForKontorWithoutHPR = behandlerService.getBehandlereForKontor(behandlerKontor).filter { it.hprId.isNullOrEmpty() }
                        if (existingBehandlereForKontorWithoutHPR.isNotEmpty()) {
                            repairMissingHPR(existingBehandlereForKontorWithoutHPR, behandlerKontorFraAdresseregisteret.behandlere)
                        }

                        val (existingBehandlereForKontor, existingInvalidatedBehandlereForKontor) = behandlerService.getBehandlereForKontor(behandlerKontor).partition { it.invalidated == null }
                        log.info("VerifyBehandlereForKontorCronjob: Fant ${aktiveBehandlereForKontor.size} aktive behandlere for kontor ${behandlerKontor.herId} i Adresseregisteret")
                        log.info("VerifyBehandlereForKontorCronjob: Fant ${inaktiveBehandlereForKontor.size} inaktive behandlere for kontor ${behandlerKontor.herId} i Adresseregisteret")
                        log.info("VerifyBehandlereForKontorCronjob: Fant ${existingBehandlereForKontor.size} behandlere for kontor ${behandlerKontor.herId} i Modia")

                        invalidateUnknownBehandlere(behandlerKontorFraAdresseregisteret.behandlere, existingBehandlereForKontor)
                        invalidateInactiveBehandlere(inaktiveBehandlereForKontor, existingBehandlereForKontor)

                        val revalidated = revalidateBehandlere(
                            aktiveBehandlereForKontor,
                            existingBehandlereForKontor,
                            existingInvalidatedBehandlereForKontor,
                        )

                        val added = addNewBehandlere(
                            aktiveBehandlereForKontor.toMutableList().also { it.removeAll(revalidated) },
                            existingBehandlereForKontor,
                            behandlerKontor,
                            behandlerKontorFraAdresseregisteret,
                        )

                        invalidateDuplicates(
                            aktiveBehandlereForKontor.toMutableList().also { it.removeAll(revalidated) }.also { it.removeAll(added) },
                            existingBehandlereForKontor,
                        )

                        updateExistingBehandlere(
                            aktiveBehandlereForKontor,
                            behandlerService.getBehandlereForKontor(behandlerKontor).filter { it.invalidated == null },
                        )
                    }
                } else {
                    log.warn("VerifyBehandlereForKontorCronjob: Behandlerkontor mer herId ${behandlerKontor.herId} ble ikke funnet i Adresseregisteret")
                }
                verifyResult.updated++
            } catch (e: Exception) {
                log.error("Exception caught while checking behandlerkontor with herId ${behandlerKontor.herId}", e)
                verifyResult.failed++
            }
        }
        log.info(
            "Completed checking behandlerkontor result: {}, {}",
            StructuredArguments.keyValue("failed", verifyResult.failed),
            StructuredArguments.keyValue("updated", verifyResult.updated),
        )
    }

    private suspend fun repairMissingHPR(
        existingBehandlereForKontorWithoutHPR: List<PBehandler>,
        behandlereForKontor: List<BehandlerKontorFraAdresseregisteretDTO.BehandlerFraAdresseregisteretDTO>,
    ) {
        existingBehandlereForKontorWithoutHPR.forEach { behandler ->
            if (behandler.personident.isNullOrEmpty()) {
                log.warn("VerifyBehandlereForKontorCronjob: Behandler missing both hpr and fnr: ${behandler.behandlerRef}")
            } else {
                val match = behandlereForKontor.firstOrNull {
                    val hprBehandler = syfohelsenettproxyClient.finnBehandlerFraHpr(it.hprId!!.toString())
                    hprBehandler != null && behandler.personident!! == hprBehandler.fnr
                }
                if (match != null) {
                    behandlerService.updateBehandlerIdenter(
                        behandlerRef = behandler.behandlerRef,
                        identer = mapOf(
                            Pair(BehandleridentType.HER, match.herId.toString()),
                            Pair(BehandleridentType.HPR, match.hprId.toString())
                        ),
                    )
                    log.info("VerifyBehandlereForKontorCronjob: Fixed missing hprId for behandler: ${behandler.behandlerRef}")
                } else {
                    log.warn("VerifyBehandlereForKontorCronjob: Behandler missing hpr but found no match: ${behandler.behandlerRef}")
                }
            }
        }
    }

    private fun invalidateInactiveBehandlere(
        inaktiveBehandlereForKontor: List<BehandlerKontorFraAdresseregisteretDTO.BehandlerFraAdresseregisteretDTO>,
        existingBehandlereForKontor: List<PBehandler>,
    ) {
        inaktiveBehandlereForKontor.filter {
            it.hprId != null
        }.forEach { behandlerFraAdresseregisteret ->
            val behandlerFraAdresseregisteretHprId = behandlerFraAdresseregisteret.hprId!!.toString()
            val existingBehandlere = existingBehandlereForKontor.filter {
                it.hprId == behandlerFraAdresseregisteretHprId && it.invalidated == null
            }
            existingBehandlere.forEach { existingBehandler ->
                behandlerService.invalidateBehandler(existingBehandler.behandlerRef)
                log.info("VerifyBehandlereForKontorCronjob: behandler ${existingBehandler.behandlerRef} invalidated since inactive in Adresseregisteret")
            }
        }
    }

    private fun invalidateUnknownBehandlere(
        allBehandlereForKontor: List<BehandlerKontorFraAdresseregisteretDTO.BehandlerFraAdresseregisteretDTO>,
        existingBehandlereForKontor: List<PBehandler>,
    ) {
        existingBehandlereForKontor.filter {
            it.hprId != null
        }.forEach { existingBehandler ->
            val existingBehandlereFraAdresseregisteret = allBehandlereForKontor.filter {
                it.hprId.toString() == existingBehandler.hprId
            }
            if (existingBehandlereFraAdresseregisteret.isEmpty()) {
                behandlerService.invalidateBehandler(existingBehandler.behandlerRef)
                log.info("VerifyBehandlereForKontorCronjob: behandler ${existingBehandler.behandlerRef} invalidated since not found in Adresseregisteret")
            }
        }
    }

    private fun revalidateBehandlere(
        aktiveBehandlereForKontor: List<BehandlerKontorFraAdresseregisteretDTO.BehandlerFraAdresseregisteretDTO>,
        existingBehandlereForKontor: List<PBehandler>,
        existingInvalidatedBehandlereForKontor: List<PBehandler>,
    ): List<BehandlerKontorFraAdresseregisteretDTO.BehandlerFraAdresseregisteretDTO> {
        val revalidated = mutableListOf<BehandlerKontorFraAdresseregisteretDTO.BehandlerFraAdresseregisteretDTO>()
        aktiveBehandlereForKontor.filter {
            it.hprId != null
        }.forEach { behandlerFraAdresseregisteret ->
            val behandlerFraAdresseregisteretHprId = behandlerFraAdresseregisteret.hprId!!.toString()
            val existingInvalidatedBehandler = existingInvalidatedBehandlereForKontor.firstOrNull {
                it.hprId == behandlerFraAdresseregisteretHprId && it.invalidated != null
            }
            existingInvalidatedBehandler?.let { invalidated ->
                val existingBehandler = existingBehandlereForKontor.firstOrNull {
                    it.hprId == existingInvalidatedBehandler.hprId ||
                        (it.personident != null && it.personident == existingInvalidatedBehandler.personident)
                }
                if (existingBehandler == null) {
                    // Only revalidate if there is no active duplicate
                    behandlerService.revalidateBehandler(invalidated.behandlerRef)
                    revalidated.add(behandlerFraAdresseregisteret)
                    log.info("VerifyBehandlereForKontorCronjob: behandler ${invalidated.behandlerRef} revalidated since active in Adresseregisteret")
                }
            }
        }
        return revalidated
    }

    private suspend fun addNewBehandlere(
        aktiveBehandlereForKontor: List<BehandlerKontorFraAdresseregisteretDTO.BehandlerFraAdresseregisteretDTO>,
        existingBehandlereForKontor: List<PBehandler>,
        behandlerKontor: PBehandlerKontor,
        behandlerKontorFraAdresseregisteret: BehandlerKontorFraAdresseregisteretDTO,
    ): List<BehandlerKontorFraAdresseregisteretDTO.BehandlerFraAdresseregisteretDTO> {
        val added = mutableListOf<BehandlerKontorFraAdresseregisteretDTO.BehandlerFraAdresseregisteretDTO>()
        aktiveBehandlereForKontor.filter {
            it.hprId != null
        }.forEach { behandlerFraAdresseregisteret ->
            val behandlerFraAdresseregisteretHprId = behandlerFraAdresseregisteret.hprId!!.toString()
            val existingBehandler = existingBehandlereForKontor.firstOrNull {
                it.hprId == behandlerFraAdresseregisteretHprId
            }
            if (existingBehandler == null) {
                val hprBehandler = syfohelsenettproxyClient.finnBehandlerFraHpr(behandlerFraAdresseregisteretHprId)
                val hprBehandlerKategori = hprBehandler?.getBehandlerKategori()
                if (hprBehandler != null && hprBehandler.fnr != null && hprBehandlerKategori != null) {
                    val behandlerRef = UUID.randomUUID()
                    behandlerService.createBehandler(
                        behandler = Behandler(
                            behandlerRef = behandlerRef,
                            personident = Personident(hprBehandler.fnr),
                            fornavn = hprBehandler.fornavn ?: behandlerFraAdresseregisteret.fornavn,
                            mellomnavn = hprBehandler.mellomnavn ?: behandlerFraAdresseregisteret.mellomnavn,
                            etternavn = hprBehandler.etternavn ?: behandlerFraAdresseregisteret.etternavn,
                            herId = behandlerFraAdresseregisteret.herId,
                            hprId = behandlerFraAdresseregisteretHprId.toInt(),
                            telefon = behandlerKontorFraAdresseregisteret.telefon,
                            kontor = behandlerKontor.toBehandlerKontor(),
                            kategori = hprBehandlerKategori,
                            mottatt = nowUTC(),
                            suspendert = false,
                        ),
                        kontorId = behandlerKontor.id,
                    )
                    added.add(behandlerFraAdresseregisteret)
                    log.info("VerifyBehandlereForKontorCronjob: added new behandler from Adresseregisteret: $behandlerRef")
                }
            }
        }
        return added
    }

    private suspend fun invalidateDuplicates(
        aktiveBehandlereForKontor: List<BehandlerKontorFraAdresseregisteretDTO.BehandlerFraAdresseregisteretDTO>,
        existingBehandlereForKontor: List<PBehandler>,
    ) {
        aktiveBehandlereForKontor.filter {
            it.hprId != null
        }.forEach { behandlerFraAdresseregisteret ->
            val behandlerFraAdresseregisteretHprId = behandlerFraAdresseregisteret.hprId!!.toString()
            val hprBehandlerFnr = syfohelsenettproxyClient.finnBehandlerFraHpr(behandlerFraAdresseregisteretHprId)?.fnr
            val existingBehandlereWithSameId = existingBehandlereForKontor.filter {
                it.hprId == behandlerFraAdresseregisteretHprId ||
                    (hprBehandlerFnr != null && hprBehandlerFnr == it.personident)
            }
            if (existingBehandlereWithSameId.size > 1) {
                val existingBehandlerWithoutDNummer = existingBehandlereWithSameId.firstOrNull {
                    it.personident != null && !Personident(it.personident).isDNR()
                }
                val existingBehandlerToKeep = if (existingBehandlerWithoutDNummer != null) {
                    existingBehandlerWithoutDNummer
                } else {
                    existingBehandlereWithSameId.firstOrNull {
                        it.hprId != null && it.personident != null
                    }
                }
                if (existingBehandlerToKeep != null) {
                    // invalidate the others
                    existingBehandlereWithSameId.filter { it != existingBehandlerToKeep }.forEach {
                        behandlerService.invalidateBehandler(it.behandlerRef)
                        log.info("VerifyBehandlereForKontorCronjob: invalidated duplicate: ${it.behandlerRef}")
                    }
                } else {
                    log.warn("VerifyBehandlereForKontorCronjob: Found duplicates, but could not decide which instance to keep: ${existingBehandlereWithSameId.first().behandlerRef}")
                }
            }
        }
    }

    private suspend fun updateExistingBehandlere(
        aktiveBehandlereForKontor: List<BehandlerKontorFraAdresseregisteretDTO.BehandlerFraAdresseregisteretDTO>,
        existingBehandlereForKontor: List<PBehandler>,
    ) {
        aktiveBehandlereForKontor.filter {
            it.hprId != null
        }.forEach { behandlerFraAdresseregisteret ->
            val behandlerFraAdresseregisteretHprId = behandlerFraAdresseregisteret.hprId!!.toString()
            val existingBehandlereWithSameHprId = existingBehandlereForKontor.filter {
                it.hprId == behandlerFraAdresseregisteretHprId
            }
            if (existingBehandlereWithSameHprId.size > 1) {
                log.warn("VerifyBehandlereForKontorCronjob: Expected to find exactly one behandler: ${existingBehandlereWithSameHprId.firstOrNull()?.behandlerRef}")
            } else if (existingBehandlereWithSameHprId.size == 1) {
                val existingBehandler = existingBehandlereWithSameHprId[0]
                val hprBehandlerFnr = syfohelsenettproxyClient.finnBehandlerFraHpr(behandlerFraAdresseregisteretHprId)?.fnr
                if (hprBehandlerFnr == null || hprBehandlerFnr != existingBehandler.personident) {
                    log.warn("VerifyBehandlereForKontorCronjob: Mismatched personident: ${existingBehandler.behandlerRef}")
                } else {
                    // both hpr and personident match: update name, herid and kategori
                    behandlerService.updateBehandlerNavnAndKategoriAndHerId(
                        behandlerRef = existingBehandler.behandlerRef,
                        fornavn = behandlerFraAdresseregisteret.fornavn,
                        mellomnavn = behandlerFraAdresseregisteret.mellomnavn,
                        etternavn = behandlerFraAdresseregisteret.etternavn,
                        kategori = BehandlerKategori.fromKategoriKode(behandlerFraAdresseregisteret.kategori),
                        herId = behandlerFraAdresseregisteret.herId.toString(),
                    )
                }
            }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(VerifyBehandlereForKontorCronjob::class.java)
    }
}
