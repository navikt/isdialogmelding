package no.nav.syfo.cronjob

import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.behandler.BehandlerService
import no.nav.syfo.behandler.database.domain.PBehandler
import no.nav.syfo.behandler.database.domain.PBehandlerKontor
import no.nav.syfo.behandler.database.domain.toBehandlerKontor
import no.nav.syfo.behandler.domain.Behandler
import no.nav.syfo.behandler.fastlege.BehandlerKontorFraAdresseregisteretDTO
import no.nav.syfo.behandler.fastlege.FastlegeClient
import no.nav.syfo.client.syfohelsenettproxy.SyfohelsenettproxyClient
import no.nav.syfo.domain.Personident
import no.nav.syfo.util.nowUTC
import org.slf4j.LoggerFactory
import java.util.UUID

class VerifyBehandlereForKontorCronjob(
    val behandlerService: BehandlerService,
    val fastlegeClient: FastlegeClient,
    val syfohelsenettproxyClient: SyfohelsenettproxyClient,
) : DialogmeldingCronjob {
    private val runAtHour = 3

    override val initialDelayMinutes: Long = calculateInitialDelay("VerifyBehandlereForKontorCronjob", runAtHour)
    override val intervalDelayMinutes: Long = 24 * 60

    override suspend fun run() {
        verifyBehandlereForKontorJob()
    }

    suspend fun verifyBehandlereForKontorJob() {
        val verifyResult = DialogmeldingCronjobResult()

        val behandlerKontorListe = behandlerService.getKontor().filter {
            it.herId != null && it.dialogmeldingEnabled != null
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
                        val (existingBehandlereForKontor, existingInvalidatedBehandlereForKontor) = behandlerService.getBehandlereForKontor(behandlerKontor).partition { it.invalidated == null }
                        log.info("VerifyBehandlereForKontorCronjob: Fant ${aktiveBehandlereForKontor.size} aktive behandlere for kontor ${behandlerKontor.herId} i Adresseregisteret")
                        log.info("VerifyBehandlereForKontorCronjob: Fant ${inaktiveBehandlereForKontor.size} inaktive behandlere for kontor ${behandlerKontor.herId} i Adresseregisteret")
                        log.info("VerifyBehandlereForKontorCronjob: Fant ${existingBehandlereForKontor.size} behandlere for kontor ${behandlerKontor.herId} i Modia")

                        invalidateInactiveBehandlere(inaktiveBehandlereForKontor, existingBehandlereForKontor)

                        val revalidated = revalidateBehandlere(aktiveBehandlereForKontor, existingInvalidatedBehandlereForKontor)

                        addNewBehandlere(
                            aktiveBehandlereForKontor.toMutableList().also { it.removeAll(revalidated) },
                            existingBehandlereForKontor,
                            behandlerKontor,
                            behandlerKontorFraAdresseregisteret,
                        )

                        // TODO: Hvis duplikater fra før: invalidere behandlerforekomst med D-nr

                        // TODO: Hvis duplikat fra før: invalidere behandlerforekomst som ikke stemmer overens med Adresseregisteret

                        // TODO: Hvis finnes fra før: oppdatere behandlerforekomst
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

    private fun invalidateInactiveBehandlere(
        inaktiveBehandlereForKontor: List<BehandlerKontorFraAdresseregisteretDTO.BehandlerFraAdresseregisteretDTO>,
        existingBehandlereForKontor: List<PBehandler>
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

    private fun revalidateBehandlere(
        aktiveBehandlereForKontor: List<BehandlerKontorFraAdresseregisteretDTO.BehandlerFraAdresseregisteretDTO>,
        existingInvalidatedBehandlereForKontor: List<PBehandler>
    ): List<BehandlerKontorFraAdresseregisteretDTO.BehandlerFraAdresseregisteretDTO> {
        val revalidated = mutableListOf<BehandlerKontorFraAdresseregisteretDTO.BehandlerFraAdresseregisteretDTO>()
        aktiveBehandlereForKontor.filter {
            it.hprId != null
        }.forEach { behandlerFraAdresseregisteret ->
            val behandlerFraAdresseregisteretHprId = behandlerFraAdresseregisteret.hprId!!.toString()
            val existingBehandler = existingInvalidatedBehandlereForKontor.filter {
                it.hprId == behandlerFraAdresseregisteretHprId && it.invalidated != null
            }.firstOrNull()
            existingBehandler?.let {
                behandlerService.revalidateBehandler(it.behandlerRef)
                revalidated.add(behandlerFraAdresseregisteret)
                log.info("VerifyBehandlereForKontorCronjob: behandler ${it.behandlerRef} revalidated since active in Adresseregisteret")
            }
        }
        return revalidated
    }

    private suspend fun addNewBehandlere(
        aktiveBehandlereForKontor: List<BehandlerKontorFraAdresseregisteretDTO.BehandlerFraAdresseregisteretDTO>,
        existingBehandlereForKontor: List<PBehandler>,
        behandlerKontor: PBehandlerKontor,
        behandlerKontorFraAdresseregisteret: BehandlerKontorFraAdresseregisteretDTO,
    ) {
        aktiveBehandlereForKontor.filter {
            it.hprId != null
        }.forEach { behandlerFraAdresseregisteret ->
            val behandlerFraAdresseregisteretHprId = behandlerFraAdresseregisteret.hprId!!.toString()
            val existingBehandler = existingBehandlereForKontor.filter {
                it.hprId == behandlerFraAdresseregisteretHprId
            }.firstOrNull()
            if (existingBehandler == null) {
                val hprBehandler = syfohelsenettproxyClient.finnBehandlerFraHpr(behandlerFraAdresseregisteretHprId)
                if (hprBehandler != null && hprBehandler.fnr != null && hprBehandler.getBehandlerKategori() != null) {
                    val behandlerRef = UUID.randomUUID()
                    behandlerService.createBehandler(
                        behandler = Behandler(
                            behandlerRef = behandlerRef,
                            personident = Personident(hprBehandler.fnr!!),
                            fornavn = hprBehandler.fornavn ?: behandlerFraAdresseregisteret.fornavn,
                            mellomnavn = hprBehandler.mellomnavn ?: behandlerFraAdresseregisteret.mellomnavn,
                            etternavn = hprBehandler.etternavn ?: behandlerFraAdresseregisteret.etternavn,
                            herId = behandlerFraAdresseregisteret.herId,
                            hprId = behandlerFraAdresseregisteretHprId.toInt(),
                            telefon = behandlerKontorFraAdresseregisteret.telefon,
                            kontor = behandlerKontor.toBehandlerKontor(),
                            kategori = hprBehandler.getBehandlerKategori()!!,
                            mottatt = nowUTC(),
                            suspendert = false,
                        ),
                        kontorId = behandlerKontor.id,
                    )
                    log.info("VerifyBehandlereForKontorCronjob: added new behandler from Adresseregisteret: $behandlerRef")
                } else {
                    log.warn("VerifyBehandlereForKontorCronjob: could not add new behandler from Adresseregisteret because hprBehandler incomplete")
                }
            }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(VerifyBehandlereForKontorCronjob::class.java)
    }
}
