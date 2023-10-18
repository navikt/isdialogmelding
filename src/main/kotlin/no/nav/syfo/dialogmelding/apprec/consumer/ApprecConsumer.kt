package no.nav.syfo.dialogmelding.apprec.consumer

import kotlinx.coroutines.delay
import no.nav.helse.apprecV1.XMLAppRec
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.dialogmelding.apprec.ApprecService
import no.nav.syfo.dialogmelding.apprec.domain.Apprec
import no.nav.syfo.dialogmelding.apprec.domain.ApprecStatus
import no.nav.syfo.dialogmelding.bestilling.DialogmeldingToBehandlerService
import no.nav.syfo.metric.RECEIVED_APPREC_COUNTER
import no.nav.syfo.metric.RECEIVED_APPREC_MESSAGE_COUNTER
import no.nav.syfo.metric.STORED_APPREC_COUNTER
import no.nav.syfo.util.apprecUnmarshaller
import no.nav.syfo.util.log
import no.nav.xml.eiff._2.XMLEIFellesformat
import org.xml.sax.InputSource
import java.io.StringReader
import java.util.*
import javax.jms.Message
import javax.jms.MessageConsumer
import javax.jms.TextMessage
import javax.xml.bind.JAXBException
import javax.xml.parsers.SAXParserFactory
import javax.xml.transform.Source
import javax.xml.transform.sax.SAXSource

class ApprecConsumer(
    val applicationState: ApplicationState,
    val database: DatabaseInterface,
    val inputconsumer: MessageConsumer,
    private val apprecService: ApprecService,
    private val dialogmeldingToBehandlerService: DialogmeldingToBehandlerService,
) {

    suspend fun run() {
        try {
            while (applicationState.ready) {
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
        inputMessageText: String,
    ) {
        var xmlApprec: XMLAppRec? = null
        try {
            val fellesformat = safeUnmarshal(inputMessageText)
            xmlApprec = fellesformat.get()
        } catch (exc: JAXBException) {
            log.warn("ApprecConsumer received message that could not be parsed", exc)
        }
        if (xmlApprec == null) {
            log.warn("ApprecConsumer received message that was not an apprec")
            return
        }

        val bestillingId = xmlApprec.originalMsgId.id
        val apprecId = xmlApprec.id
        val apprecStatusValue = xmlApprec.status.v
        val apprecExists = apprecService.apprecExists(UUID.fromString(apprecId))

        if (apprecExists) {
            log.warn("Ignoring duplicate apprec with status $apprecStatusValue and id $apprecId for dialogmelding with id $bestillingId\"")
        } else {
            val apprecStatus = ApprecStatus.fromV(apprecStatusValue)
            if (apprecStatus != null) {
                log.info("Received apprec with status $apprecStatus and id $apprecId for dialogmelding with id $bestillingId")
                val dialogmeldingToBehandlerBestillingPair =
                    dialogmeldingToBehandlerService.getBestilling(UUID.fromString(bestillingId))
                if (dialogmeldingToBehandlerBestillingPair != null) {
                    val (dialogmeldingBestillingId, dialogmeldingBestilling) = dialogmeldingToBehandlerBestillingPair
                    val apprec = Apprec(
                        uuid = UUID.fromString(apprecId),
                        bestilling = dialogmeldingBestilling,
                        statusKode = apprecStatus,
                        statusTekst = xmlApprec.status.dn,
                        feilKode = xmlApprec.error.firstOrNull()?.v,
                        feilTekst = xmlApprec.error.firstOrNull()?.dn,
                    )
                    apprecService.createApprec(
                        apprec = apprec,
                        bestillingId = dialogmeldingBestillingId,
                    )
                    STORED_APPREC_COUNTER.increment()
                } else {
                    log.info("Received but skipped apprec with id $apprecId because unknown dialogmelding $bestillingId")
                }
            } else {
                log.info("Received but skipped apprec with id $apprecId because unknown status $apprecStatusValue")
            }
        }
        RECEIVED_APPREC_COUNTER.increment()
    }
}

private fun safeUnmarshal(inputMessageText: String): XMLEIFellesformat {
    // Disable XXE
    val spf: SAXParserFactory = SAXParserFactory.newInstance()
    spf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
    spf.isNamespaceAware = true

    val xmlSource: Source =
        SAXSource(
            spf.newSAXParser().xmlReader,
            InputSource(StringReader(inputMessageText)),
        )
    return apprecUnmarshaller.unmarshal(xmlSource) as XMLEIFellesformat
}

inline fun <reified T> XMLEIFellesformat.get() = this.any.find { it is T } as T
