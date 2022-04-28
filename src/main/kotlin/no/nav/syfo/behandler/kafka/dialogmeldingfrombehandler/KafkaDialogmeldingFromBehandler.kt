package no.nav.syfo.behandler.kafka.dialogmeldingfrombehandler

import no.nav.syfo.application.ApplicationEnvironmentKafka
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.behandler.database.updateBehandlerKontorDialogmeldingEnabled
import no.nav.syfo.behandler.kafka.kafkaDialogmeldingFromBehandlerConsumerConfig
import no.nav.syfo.domain.PartnerId
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
    database: DatabaseInterface,
) {
    val consumerProperties = kafkaDialogmeldingFromBehandlerConsumerConfig(applicationEnvironmentKafka)
    val kafkaConsumerDialogmelding = KafkaConsumer<String, KafkaDialogmeldingFromBehandlerDTO>(consumerProperties)
    kafkaConsumerDialogmelding.subscribe(
        listOf(DIALOGMELDING_FROM_BEHANDLER_TOPIC)
    )
    while (applicationState.ready) {
        pollAndProcessDialogmeldingFromBehandler(
            database = database,
            kafkaConsumerDialogmeldingFromBehandler = kafkaConsumerDialogmelding,
        )
    }
}

fun pollAndProcessDialogmeldingFromBehandler(
    database: DatabaseInterface,
    kafkaConsumerDialogmeldingFromBehandler: KafkaConsumer<String, KafkaDialogmeldingFromBehandlerDTO>,
) {
    val records = kafkaConsumerDialogmeldingFromBehandler.poll(Duration.ofMillis(1000))
    if (records.count() > 0) {
        updateBehandlerOffice(
            consumerRecords = records,
            database = database,
        )
        kafkaConsumerDialogmeldingFromBehandler.commitSync()
    }
}

// TODO: Legg til id-typer som legen identifiserer seg med: se https://jira.adeo.no/browse/FAGSYSTEM-214710
fun updateBehandlerOffice(
    consumerRecords: ConsumerRecords<String, KafkaDialogmeldingFromBehandlerDTO>,
    database: DatabaseInterface,
) {
    consumerRecords.forEach {
        val dialogmeldingFromBehandler = it.value()
        log.info("Received a dialogmelding from behandler: navLogId: ${dialogmeldingFromBehandler.navLogId}, kontorOrgnr: ${dialogmeldingFromBehandler.legekontorOrgNr}, msgId: ${dialogmeldingFromBehandler.msgId}")

        val partnerId = getPartnerId(
            dialogmeldingFromBehandler.fellesformatXML,
            dialogmeldingFromBehandler.navLogId,
            dialogmeldingFromBehandler.msgId,
        )

        partnerId?.let { id ->
            val dialogmeldingEnabled = database.updateBehandlerKontorDialogmeldingEnabled(id)
            if (dialogmeldingEnabled)
                log.info("Behandlerkontor with partnerId: $id is ready for dialogmelding")
            else
                log.info("No behandlerkontor to update for $id")
        }
    }
}

private fun getPartnerId(fellesformatXML: String, navLogid: String, msgId: String): PartnerId? {
    return try {
        val mottakenhetBlokk = getObjectFromXmlString<XMLMottakenhetBlokk>(fellesformatXML, "MottakenhetBlokk")
        PartnerId(mottakenhetBlokk.partnerReferanse.toInt())
    } catch (e: Exception) {
        log.warn("Can't find partnerId in dialogmelding from behandler: navLogId: $navLogid, msgId: $msgId", e)
        null
    }
}
