package no.nav.syfo.oppfolgingsplan

import no.nav.syfo.application.mq.MQSender
import no.nav.syfo.domain.RSHodemelding
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("no.nav.syfo.oppfolgingsplan")

class OppfolgingsplanService(
    mqSender: MQSender,
) {
    fun sendMelding(melding: RSHodemelding) {
        log.info("Trying to send oppfolgingsplan to lege")
        // TODO Send to emottak with mq!
    }
}
