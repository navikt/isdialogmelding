package no.nav.syfo.dialogmelding

import no.nav.syfo.application.mq.MQSender
import no.nav.syfo.behandler.domain.*
import no.nav.syfo.client.pdl.PdlClient
import no.nav.syfo.dialogmelding.bestilling.domain.DialogmeldingToBehandlerBestilling
import no.nav.syfo.fellesformat.Fellesformat
import no.nav.syfo.dialogmelding.converter.createFellesformat
import no.nav.syfo.dialogmelding.edi2.OutgoingDialogMessageProducer
import no.nav.syfo.domain.Personident
import no.nav.syfo.util.JAXB
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime

private val log = LoggerFactory.getLogger("no.nav.syfo.dialogmelding")

class DialogmeldingService(
    val pdlClient: PdlClient,
    val mqSender: MQSender,
    val outgoingDialogMessageProducer: OutgoingDialogMessageProducer,
    val edi2SendEnabledHerIds: Set<Int> = emptySet(),
) {
    suspend fun sendMelding(melding: DialogmeldingToBehandlerBestilling) {
        if (shouldSendDialogmeldingToEdi2(melding)) {
            log.info(
                "Sending dialogmelding med type ${melding.type} to helsemelding.dialog.out for behandler with partnerId: ${melding.behandler.kontor.partnerId}"
            )
            outgoingDialogMessageProducer.sendDialogmelding(melding)
        } else {
            log.info(
                "Sending dialogmelding med type ${melding.type} to emottak for behandler with partnerId: ${melding.behandler.kontor.partnerId}"
            )
            val arbeidstaker = getArbeidstaker(melding.arbeidstakerPersonident)
            val fellesformat: Fellesformat = opprettDialogmelding(melding, arbeidstaker)
            mqSender.sendMessageToEmottak(fellesformat.message!!)
        }
    }

    private fun shouldSendDialogmeldingToEdi2(melding: DialogmeldingToBehandlerBestilling): Boolean {
        if (edi2SendEnabledHerIds.isEmpty()) {
            return false
        }
        val herIds = listOfNotNull(
            melding.behandler.kontor.herId,
            melding.behandler.herId,
        )
        return herIds.any { edi2SendEnabledHerIds.contains(it) }
    }

    private suspend fun getArbeidstaker(
        personident: Personident,
    ): Arbeidstaker {
        val pdlNavn = pdlClient.person(personident)?.hentPerson?.navn?.first()
            ?: throw RuntimeException("PDL returned empty response")

        return Arbeidstaker(
            arbeidstakerPersonident = personident,
            fornavn = pdlNavn.fornavn,
            mellomnavn = pdlNavn.mellomnavn,
            etternavn = pdlNavn.etternavn,
            mottatt = OffsetDateTime.now(),
        )
    }

    private fun opprettDialogmelding(
        melding: DialogmeldingToBehandlerBestilling,
        arbeidstaker: Arbeidstaker,
    ): Fellesformat {
        val xmleiFellesformat = createFellesformat(
            melding = melding,
            arbeidstaker = arbeidstaker,
        )
        return Fellesformat(xmleiFellesformat, JAXB::marshallDialogmelding1_0)
    }
}
