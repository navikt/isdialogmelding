package no.nav.syfo.oppfolgingsplan

import no.nav.syfo.application.mq.MQSender
import no.nav.syfo.behandler.BehandlerService
import no.nav.syfo.behandler.DialogmeldingToBehandlerService
import no.nav.syfo.behandler.api.person.RSOppfolgingsplan
import no.nav.syfo.domain.Personident
import no.nav.syfo.fellesformat.Fellesformat
import no.nav.syfo.metric.COUNT_SEND_OPPFOLGINGSPLAN_FAILED
import no.nav.syfo.metric.COUNT_SEND_OPPFOLGINGSPLAN_SUCCESS
import no.nav.syfo.oppfolgingsplan.converter.createFellesformat
import no.nav.syfo.oppfolgingsplan.domain.RSHodemelding
import no.nav.syfo.oppfolgingsplan.domain.createRSHodemelding
import no.nav.syfo.util.JAXB
import no.nav.xml.eiff._2.XMLEIFellesformat
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("no.nav.syfo.oppfolgingsplan")

class OppfolgingsplanService(
    val mqSender: MQSender,
    val behandlerService: BehandlerService,
    val dialogmeldingToBehandlerService: DialogmeldingToBehandlerService,
) {
    suspend fun sendOppfolgingsplan(
        callId: String,
        token: String,
        oppfolgingsplan: RSOppfolgingsplan,
    ) {
        try {
            val arbeidstakerIdent = Personident(oppfolgingsplan.sykmeldtFnr)
            val fastlegeBehandler = behandlerService.getFastlegeBehandler(
                personident = arbeidstakerIdent,
                token = token,
                callId = callId,
                systemRequest = true,
            ) ?: throw IllegalArgumentException("Feil ved sending av oppfølgingsplan, FastlegeIkkeFunnet")

            val arbeidstaker = dialogmeldingToBehandlerService.getArbeidstakerIfRelasjonToBehandler(
                behandlerRef = fastlegeBehandler.behandlerRef,
                personident = arbeidstakerIdent,
            )

            val melding = createRSHodemelding(
                behandler = fastlegeBehandler,
                arbeidstaker = arbeidstaker,
                oppfolgingsplan = oppfolgingsplan,
            )
            sendMelding(melding)
            COUNT_SEND_OPPFOLGINGSPLAN_SUCCESS.increment()
        } catch (exc: Exception) {
            COUNT_SEND_OPPFOLGINGSPLAN_FAILED.increment()
            throw exc
        }
    }

    fun sendMelding(melding: RSHodemelding) {
        log.info("Sending oppfølgingsplan to lege with partnerId: ${melding.meldingInfo?.mottaker?.partnerId}")

        val fellesformat: Fellesformat = opprettDialogmelding(melding)

        mqSender.sendMessageToEmottak(fellesformat.message!!)
    }

    private fun opprettDialogmelding(hodemelding: RSHodemelding): Fellesformat {
        val xmleiFellesformat: XMLEIFellesformat = createFellesformat(hodemelding)
        return Fellesformat(xmleiFellesformat, JAXB::marshallDialogmelding1_0)
    }
}
