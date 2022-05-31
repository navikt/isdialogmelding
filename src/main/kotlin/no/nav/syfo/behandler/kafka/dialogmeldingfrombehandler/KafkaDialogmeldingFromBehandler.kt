package no.nav.syfo.behandler.kafka.dialogmeldingfrombehandler

import no.kith.xmlstds.msghead._2006_05_24.XMLHealthcareProfessional
import no.nav.syfo.application.ApplicationEnvironmentKafka
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.behandler.database.*
import no.nav.syfo.behandler.domain.BehandleridentType
import no.nav.syfo.behandler.kafka.kafkaDialogmeldingFromBehandlerConsumerConfig
import no.nav.syfo.domain.PartnerId
import no.nav.syfo.domain.Personident
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
        processConsumerRecords(
            consumerRecords = records,
            database = database,
        )
        kafkaConsumerDialogmeldingFromBehandler.commitSync()
    }
}

fun processConsumerRecords(
    consumerRecords: ConsumerRecords<String, KafkaDialogmeldingFromBehandlerDTO>,
    database: DatabaseInterface,
) {
    consumerRecords.forEach {
        val dialogmeldingFromBehandler = it.value()
        log.info("Received a dialogmelding from behandler: navLogId: ${dialogmeldingFromBehandler.navLogId}, kontorOrgnr: ${dialogmeldingFromBehandler.legekontorOrgNr}, msgId: ${dialogmeldingFromBehandler.msgId}")

        updateKontorAndBehandler(dialogmeldingFromBehandler, database)
    }
}

private fun updateKontorAndBehandler(
    dialogmeldingFromBehandler: KafkaDialogmeldingFromBehandlerDTO,
    database: DatabaseInterface,
) {
    val partnerId = getPartnerId(
        dialogmeldingFromBehandler.fellesformatXML,
        dialogmeldingFromBehandler.navLogId,
        dialogmeldingFromBehandler.msgId,
    )
    val behandleridenter = getIdenterForBehandler(
        dialogmeldingFromBehandler.fellesformatXML,
        dialogmeldingFromBehandler.navLogId,
        dialogmeldingFromBehandler.msgId,
    )

    partnerId?.let { id ->
        updateDialogmeldingEnabledForKontor(
            partnerId = id,
            database = database,
        )

        updateIdenterForBehandler(
            identer = behandleridenter,
            partnerId = id,
            database = database,
        )
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

private fun getIdenterForBehandler(
    fellesformatXML: String,
    navLogid: String,
    msgId: String,
): Map<BehandleridentType, String> {
    return try {
        val xmlBehandler = getObjectFromXmlString<XMLHealthcareProfessional>(
            fellesformatXML,
            "HealthcareProfessional"
        )
        val behandlerIdenter = xmlBehandler.ident

        behandlerIdenter.associate {
            BehandleridentType.valueOf(it.typeId.v) to it.id
        }
    } catch (e: Exception) {
        log.warn("Can't find behandleridenter in dialogmelding from behandler: navLogId: $navLogid, msgId: $msgId", e)
        emptyMap()
    }
}

private fun updateDialogmeldingEnabledForKontor(
    partnerId: PartnerId,
    database: DatabaseInterface,
) {
    val dialogmeldingEnabled = database.updateBehandlerKontorDialogmeldingEnabled(partnerId)

    if (dialogmeldingEnabled)
        log.info("Behandlerkontor with partnerId: $partnerId is ready for dialogmelding")
    else
        log.info("No behandlerkontor to update for $partnerId")
}

private fun updateIdenterForBehandler(
    identer: Map<BehandleridentType, String>,
    partnerId: PartnerId,
    database: DatabaseInterface,
) {
    val behandlerFnr = identer[BehandleridentType.FNR]
    log.info("Update behandler idents for behandler connected to partnerId: $partnerId")

    behandlerFnr?.let {
        val behandlerToUpdate = database.getBehandlerByBehandlerPersonidentAndPartnerId(Personident(behandlerFnr), partnerId)

        behandlerToUpdate?.let {
            log.info("Behandler found with behandlerRef ${behandlerToUpdate.behandlerRef}")
            database.updateBehandlerIdenter(behandlerToUpdate.behandlerRef, identer)
        }
    }
}
