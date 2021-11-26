package no.nav.syfo.dialogmelding

import no.nav.syfo.application.mq.MQSender
import no.nav.syfo.behandler.BehandlerDialogmeldingService
import no.nav.syfo.behandler.domain.BehandlerDialogmeldingArbeidstaker
import no.nav.syfo.behandler.domain.BehandlerDialogmeldingBestilling
import no.nav.syfo.fellesformat.Fellesformat
import no.nav.syfo.dialogmelding.converter.FellesformatConverter
import no.nav.syfo.util.JAXB
import no.nav.xml.eiff._2.XMLEIFellesformat
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("no.nav.syfo.dialogmelding")

class DialogmeldingService(
    val behandlerDialogmeldingService: BehandlerDialogmeldingService,
    val mqSender: MQSender,
) {
    fun sendMelding(melding: BehandlerDialogmeldingBestilling) {
        log.info("Sending dialogmelding to lege with partnerId: ${melding.behandler.partnerId}")
        val arbeidstakerNavn = behandlerDialogmeldingService.getArbeidstakerNavn(melding.arbeidstakerPersonIdent)

        val fellesformat: Fellesformat = opprettDialogmelding(melding, arbeidstakerNavn)

        mqSender.sendMessageToEmottak(fellesformat.message!!)
    }

    private fun opprettDialogmelding(
        melding: BehandlerDialogmeldingBestilling,
        arbeidstakerNavn: BehandlerDialogmeldingArbeidstaker,
    ): Fellesformat {
        val xmleiFellesformat: XMLEIFellesformat = FellesformatConverter(
            melding = melding,
            arbeidstakerNavn = arbeidstakerNavn,
        ).getEiFellesformat()
        return Fellesformat(xmleiFellesformat, JAXB::marshallDialogmelding1_0)
    }
}
