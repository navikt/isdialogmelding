package no.nav.syfo.oppfolgingsplan

import no.nav.syfo.application.mq.MQSender
import no.nav.syfo.oppfolgingsplan.converter.FellesformatConverter
import no.nav.syfo.oppfolgingsplan.domain.RSHodemelding
import no.nav.syfo.util.JAXB
import no.nav.xml.eiff._2.XMLEIFellesformat
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("no.nav.syfo.oppfolgingsplan")

class OppfolgingsplanService(
    mqSender: MQSender,
) {
    fun sendMelding(melding: RSHodemelding) {
        log.info("Trying to send oppfolgingsplan to lege")

        val fellesformat: Fellesformat = opprettDialogmelding(melding)

        // TODO Send to emottak with mq!
    }

    private fun opprettDialogmelding(hodemelding: RSHodemelding): Fellesformat {
        val xmleiFellesformat: XMLEIFellesformat = FellesformatConverter(hodemelding).getEiFellesformat()
        return Fellesformat(xmleiFellesformat, JAXB::marshallDialogmelding1_0)
    }
}
