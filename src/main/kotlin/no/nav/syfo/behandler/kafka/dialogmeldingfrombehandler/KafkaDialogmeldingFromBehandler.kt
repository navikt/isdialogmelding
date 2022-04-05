package no.nav.syfo.behandler.kafka.dialogmeldingfrombehandler

import no.nav.syfo.application.ApplicationEnvironmentKafka
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.behandler.kafka.kafkaDialogmeldingFromBehandlerConsumerConfig
import no.nav.syfo.util.getObjectFromXmlString
import no.nav.xml.eiff._2.XMLMottakenhetBlokk
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration

const val DIALOGMELDING_FROM_BEHANDLER_TOPIC = "teamsykefravr.dialogmelding"

private val log: Logger = LoggerFactory.getLogger("no.nav.syfo.behandler.kafka.dialogmeldingFromBehandler")

fun blockingApplicationLogicDialogmeldingFromBehandler(
    applicationState: ApplicationState,
    applicationEnvironmentKafka: ApplicationEnvironmentKafka,
) {
    val consumerProperties = kafkaDialogmeldingFromBehandlerConsumerConfig(applicationEnvironmentKafka)
    val kafkaConsumerDialogmelding = KafkaConsumer<String, KafkaDialogmeldingFromBehandlerDTO>(consumerProperties)
    kafkaConsumerDialogmelding.subscribe(
        listOf(DIALOGMELDING_FROM_BEHANDLER_TOPIC)
    )
    while (applicationState.ready) {
        pollAndProcessDialogmeldingFromBehandler(
            kafkaConsumerDialogmeldingFromBehandler = kafkaConsumerDialogmelding,
        )
    }
}

fun pollAndProcessDialogmeldingFromBehandler(
    kafkaConsumerDialogmeldingFromBehandler: KafkaConsumer<String, KafkaDialogmeldingFromBehandlerDTO>,
) {
    val records = kafkaConsumerDialogmeldingFromBehandler.poll(Duration.ofMillis(1000))
    if (records.count() > 0) {
        updateBehandlerOffice(
            consumerRecords = records,
        )
        kafkaConsumerDialogmeldingFromBehandler.commitSync()
    }
}

// TODO: Faktisk lagre, håndtere tomme partnerreferanser
// TODO: Legg til id-typer som legen identifiserer seg med: se https://jira.adeo.no/browse/FAGSYSTEM-214710
fun updateBehandlerOffice(
    consumerRecords: ConsumerRecords<String, KafkaDialogmeldingFromBehandlerDTO>,
) {
    consumerRecords.forEach {
        val dialogmeldingFromBehandler = it.value()

        log.info("Received a dialogmelding from behandler: navLogId: ${dialogmeldingFromBehandler.navLogId}, kontorOrgnr: ${dialogmeldingFromBehandler.legekontorOrgNr}, msgId: ${dialogmeldingFromBehandler.msgId}")

        val partnerReferanse = getPartnerReferanse(
            dialogmeldingFromBehandler.fellesformatXML,
            dialogmeldingFromBehandler.navLogId,
            dialogmeldingFromBehandler.msgId,
        )
        log.info("Received dialogmelding with partnerReferanse: $partnerReferanse")
    }
}

private fun getPartnerReferanse(fellesformatXML: String, navLogid: String, msgId: String): String {
    return try {
        val mottakenhetBlokk = getObjectFromXmlString(fellesformatXML, "MottakenhetBlokk", XMLMottakenhetBlokk::class.java)
        mottakenhetBlokk.partnerReferanse
    } catch (e: Exception) {
        log.warn("Noe gikk galt ved henting av partnerReferanse fra dialogmelding fra behandler: navLogId: $navLogid, msgId: $msgId", e)
        ""
    }
}
