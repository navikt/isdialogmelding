package no.nav.syfo.behandler.kafka.dialogmeldingtobehandlerbestilling

import io.micrometer.core.instrument.Counter
import no.nav.syfo.metric.METRICS_NS
import no.nav.syfo.metric.METRICS_REGISTRY

const val KAFKA_CONSUMER_DIALOGMELDING_BESTILLING_BASE = "${METRICS_NS}_kafka_consumer_dialogmelding_bestilling"
const val KAFKA_CONSUMER_DIALOGMELDING_BESTILLING_READ = "${KAFKA_CONSUMER_DIALOGMELDING_BESTILLING_BASE}_read"
const val KAFKA_CONSUMER_DIALOGMELDING_BESTILLING_DUPLICATE = "${KAFKA_CONSUMER_DIALOGMELDING_BESTILLING_BASE}_duplicate"
const val KAFKA_CONSUMER_DIALOGMELDING_BESTILLING_TOMBSTONE = "${KAFKA_CONSUMER_DIALOGMELDING_BESTILLING_BASE}_tombstone"

val COUNT_KAFKA_CONSUMER_DIALOGMELDING_BESTILLING_READ: Counter = Counter.builder(KAFKA_CONSUMER_DIALOGMELDING_BESTILLING_READ)
    .description("Counts the number of reads from topic - DialogmeldingBestilling")
    .register(METRICS_REGISTRY)
val COUNT_KAFKA_CONSUMER_DIALOGMELDING_BESTILLING_DUPLICATE: Counter = Counter.builder(KAFKA_CONSUMER_DIALOGMELDING_BESTILLING_DUPLICATE)
    .description("Counts the number of duplicates received from topic - DialogmeldingBestilling")
    .register(METRICS_REGISTRY)
val COUNT_KAFKA_CONSUMER_DIALOGMELDING_BESTILLING_TOMBSTONE: Counter = Counter.builder(KAFKA_CONSUMER_DIALOGMELDING_BESTILLING_TOMBSTONE)
    .description("Counts the number of tombstones received from topic - DialogmeldingBestilling")
    .register(METRICS_REGISTRY)
