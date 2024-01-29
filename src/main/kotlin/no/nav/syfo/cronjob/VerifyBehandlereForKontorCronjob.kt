package no.nav.syfo.cronjob

import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.behandler.BehandlerService
import no.nav.syfo.behandler.database.domain.toBehandlerKontor
import no.nav.syfo.behandler.domain.Behandler
import no.nav.syfo.behandler.domain.BehandlerKategori
import no.nav.syfo.behandler.fastlege.FastlegeClient
import no.nav.syfo.domain.Personident
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime
import java.util.UUID

class VerifyBehandlereForKontorCronjob(
    val behandlerService: BehandlerService,
    val fastlegeClient: FastlegeClient,
) : DialogmeldingCronjob {

    override val initialDelayMinutes: Long = 2
    override val intervalDelayMinutes: Long = 24 * 60

    override suspend fun run() {
        verifyBehandlereForKontorJob()
    }

    suspend fun verifyBehandlereForKontorJob() {
        val verifyResult = DialogmeldingCronjobResult()

        val behandlerKontorListe = behandlerService.getKontor().filter {
            it.herId != null && it.dialogmeldingEnabled != null
        }
        // .filter {
        // TODO: Dette er bare for den første utprøvingen
        // it.herId in herIdsToUpdate
        // }
        behandlerKontorListe.forEach { behandlerKontor ->
            try {
                val behandlerKontorFraAdresseregisteret = fastlegeClient.behandlereForKontor(
                    callId = UUID.randomUUID().toString(),
                    kontorHerId = behandlerKontor.herId!!.toInt()
                )
                log.info("Behandlerkontor mer herId ${behandlerKontor.herId} ble funnet: ${behandlerKontorFraAdresseregisteret != null}")
                if (behandlerKontorFraAdresseregisteret != null) {
                    log.info("Behandlerkontor med herId ${behandlerKontorFraAdresseregisteret.herId} er aktivt: ${behandlerKontorFraAdresseregisteret.aktiv}")
                    if (!behandlerKontorFraAdresseregisteret.aktiv) {
                        // Deaktiver kontor
                        behandlerService.disableDialogmeldingerForKontor(behandlerKontor)
                    } else {
                        log.info("Fant ${behandlerKontorFraAdresseregisteret.behandlere.size} behandlere for kontor ${behandlerKontor.herId}")
                        val existingBehandlereForKontor = behandlerService.getBehandlereForKontor(behandlerKontor)
                        behandlerKontorFraAdresseregisteret.behandlere.forEach { behandlerDTO ->
                            log.info("Checking behandler with hprId ${behandlerDTO.hprId}")
                            // Sjekk om behandler finnes fra før og er unik med hensyn til hprNr
                            val existingBehandlere = existingBehandlereForKontor.filter { it.hprId == behandlerDTO.hprId.toString() }
                            if (existingBehandlere.size > 1) {
                                // handle duplicates
                            } else if (existingBehandlere.size == 1) {
                                // update existing instance
                            } else {
                                // add new behandler valid kategori
                                val kategori = BehandlerKategori.fromKategoriKode(behandlerDTO.kategori)
                                if (kategori != null) {
                                    behandlerService.createBehandler(
                                        Behandler(
                                            behandlerRef = UUID.randomUUID(),
                                            personident = behandlerDTO.personIdent?.let { Personident(it) },
                                            fornavn = behandlerDTO.fornavn,
                                            mellomnavn = behandlerDTO.mellomnavn,
                                            etternavn = behandlerDTO.etternavn,
                                            herId = behandlerDTO.herId,
                                            hprId = behandlerDTO.hprId,
                                            telefon = behandlerKontorFraAdresseregisteret.telefon,
                                            kontor = behandlerKontor.toBehandlerKontor(),
                                            kategori = kategori,
                                            mottatt = OffsetDateTime.now(),
                                            suspendert = false,
                                        ),
                                        kontorId = behandlerKontor.id,
                                    )
                                }
                            }
                        }

                        // Hvis duplikater fra før: invalidere forekomst med D-nr

                        // Hvis duplikat fra før: invalidere forekomst som ikke stemmer overens med Adresseregisteret

                        // Hvis finnes fra før: oppdatere

                        // Hvis finnes fra før, men deaktivert i adresseregisteret: sette invalidated-timestamp

                        // Hvis deaktivert fra før, men aktiv i adresseregisteret: sette invalidated=null
                    }
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

    companion object {
        private val herIdsToUpdate = listOf("2175", "80481")
        private val log = LoggerFactory.getLogger(VerifyBehandlereForKontorCronjob::class.java)
    }
}
