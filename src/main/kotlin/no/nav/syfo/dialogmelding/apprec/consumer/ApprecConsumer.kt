package no.nav.syfo.dialogmelding.apprec.consumer

import kotlinx.coroutines.delay
import no.nav.helse.apprecV1.XMLAppRec
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.behandler.database.*
import no.nav.syfo.behandler.database.domain.toBehandler
import no.nav.syfo.behandler.database.domain.toDialogmeldingToBehandlerBestilling
import no.nav.syfo.dialogmelding.apprec.database.createApprec
import no.nav.syfo.dialogmelding.apprec.database.getApprec
import no.nav.syfo.dialogmelding.apprec.domain.Apprec
import no.nav.syfo.dialogmelding.apprec.domain.ApprecStatus
import no.nav.syfo.metric.RECEIVED_APPREC_COUNTER
import no.nav.syfo.metric.RECEIVED_APPREC_MESSAGE_COUNTER
import no.nav.syfo.util.*
import no.nav.xml.eiff._2.XMLEIFellesformat
import java.io.StringReader
import java.util.UUID
import javax.jms.*
import javax.xml.bind.JAXBException

class ApprecConsumer(
    val applicationState: ApplicationState,
    val database: DatabaseInterface,
    val inputconsumer: MessageConsumer,
) {

    suspend fun run() {
        try {
            while (applicationState.ready && RECEIVED_APPREC_MESSAGE_COUNTER.count() < 1000) {
                val message = inputconsumer.receiveNoWait()
                if (message == null) {
                    delay(100)
                    continue
                }
                processApprecMessage(message)
            }
        } catch (exc: Exception) {
            log.error("ApprecConsumer failed, restarting application", exc)
        } finally {
            applicationState.alive = false
        }
    }

    fun processApprecMessage(message: Message) {
        RECEIVED_APPREC_MESSAGE_COUNTER.increment()
        val inputMessageText = when (message) {
            is TextMessage -> message.text
            else -> {
                log.warn("Apprec message ignored, incoming message needs to be a byte message or text message")
                null
            }
        }

        if (inputMessageText != null) {
            storeApprec(inputMessageText)
        }
        message.acknowledge()
    }

    private fun storeApprec(
        inputMessageText: String
    ): String? {
        var xmlApprec: XMLAppRec? = null
        try {
            val fellesformat = apprecUnmarshaller.unmarshal(StringReader(inputMessageText)) as XMLEIFellesformat
            xmlApprec = fellesformat.get()
        } catch (exc: JAXBException) {
            log.warn("ApprecConsumer received message that could not be parsed", exc)
        }
        if (xmlApprec == null) {
            log.warn("ApprecConsumer received message that was not an apprec")
            return null
        }
        val bestillingId = xmlApprec.originalMsgId.id

        val pBestilling = database.connection.use {
            it.getBestillinger(UUID.fromString(bestillingId))
        }
        val pApprec = database.getApprec(UUID.fromString(xmlApprec.id))
        if (pApprec != null) {
            log.warn("Ignoring duplicate apprec with status ${xmlApprec.status.v} and id ${xmlApprec.id} for dialogmelding with id $bestillingId\"")
        } else {
            val apprecStatus = ApprecStatus.fromV(xmlApprec.status.v)
            if (pBestilling != null && apprecStatus != null) {
                log.info("Received apprec with status ${xmlApprec.status.v} and id ${xmlApprec.id} for dialogmelding with id $bestillingId")
                val pBehandler = database.getBehandlerById(pBestilling.behandlerId)
                val pBehandlerKontor = database.getBehandlerKontorById(pBehandler!!.kontorId)
                val apprec = Apprec(
                    uuid = UUID.fromString(xmlApprec.id),
                    bestilling = pBestilling.toDialogmeldingToBehandlerBestilling(
                        pBehandler.toBehandler(
                            pBehandlerKontor
                        )
                    ),
                    statusKode = apprecStatus,
                    statusTekst = xmlApprec.status.dn,
                    feilKode = xmlApprec.error.firstOrNull()?.v,
                    feilTekst = xmlApprec.error.firstOrNull()?.dn,
                )
                database.connection.use { connection ->
                    connection.createApprec(
                        apprec = apprec,
                        bestillingId = pBestilling.id,
                    )
                    if (apprec.statusKode == ApprecStatus.avvist && apprec.feilKode == "E21") {
                        connection.invalidateBehandler(pBehandler.behandlerRef)
                    }
                    connection.commit()
                }
            } else {
                log.info("Received but skipped apprec with id ${xmlApprec.id} because unknown status ${xmlApprec.status.v} or unknown dialogmelding $bestillingId")
            }
        }
        RECEIVED_APPREC_COUNTER.increment()
        return xmlApprec.id
    }
}

inline fun <reified T> XMLEIFellesformat.get() = this.any.find { it is T } as T
