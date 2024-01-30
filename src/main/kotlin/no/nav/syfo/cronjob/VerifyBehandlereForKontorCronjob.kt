package no.nav.syfo.cronjob

import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.behandler.BehandlerService
import no.nav.syfo.behandler.fastlege.FastlegeClient
import org.slf4j.LoggerFactory
import java.util.UUID

class VerifyBehandlereForKontorCronjob(
    val behandlerService: BehandlerService,
    val fastlegeClient: FastlegeClient,
) : DialogmeldingCronjob {

    override val initialDelayMinutes: Long = 15
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
                val behandlerKontorFraAdresseregisteret = fastlegeClient.behandlereForKontor(
                    callId = UUID.randomUUID().toString(),
                    kontorHerId = behandlerKontor.herId!!.toInt()
                )
                if (behandlerKontorFraAdresseregisteret != null) {
                    if (!behandlerKontorFraAdresseregisteret.aktiv) {
                        log.info("VerifyBehandlereForKontorCronjob: Disable dialogmelding for kontor siden inaktiv i Adresseregisteret: herId ${behandlerKontor.herId} partnerId ${behandlerKontor.partnerId}")
                        behandlerService.disableDialogmeldingerForKontor(behandlerKontor)
                    } else {
                        val (aktiveBehandlereForKontor, inaktiveBehandlereForKontor) = behandlerKontorFraAdresseregisteret.behandlere.partition { it.aktiv }
                        val existingBehandlereForKontor = behandlerService.getBehandlereForKontor(behandlerKontor).filter { it.invalidated == null }
                        log.info("VerifyBehandlereForKontorCronjob: Fant ${aktiveBehandlereForKontor.size} aktive behandlere for kontor ${behandlerKontor.herId} i Adresseregisteret")
                        log.info("VerifyBehandlereForKontorCronjob: Fant ${inaktiveBehandlereForKontor.size} inaktive behandlere for kontor ${behandlerKontor.herId} i Adresseregisteret")
                        log.info("VerifyBehandlereForKontorCronjob: Fant ${existingBehandlereForKontor.size} behandlere for kontor ${behandlerKontor.herId} i Modia")

                        // TODO: Hvis duplikater fra før: invalidere behandlerforekomst med D-nr

                        // TODO: Hvis duplikat fra før: invalidere behandlerforekomst som ikke stemmer overens med Adresseregisteret

                        // TODO: Hvis finnes fra før: oppdatere behandlerforekomst

                        // TODO: Hvis finnes fra før, men deaktivert i adresseregisteret: sette invalidated-timestamp

                        // TODO: Hvis deaktivert fra før, men aktiv i adresseregisteret: sette invalidated=null
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

    companion object {
        private val log = LoggerFactory.getLogger(VerifyBehandlereForKontorCronjob::class.java)
    }
}
