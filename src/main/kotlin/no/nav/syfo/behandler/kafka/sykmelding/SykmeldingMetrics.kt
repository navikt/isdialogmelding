package no.nav.syfo.behandler.kafka.sykmelding

import io.micrometer.core.instrument.Counter
import no.nav.syfo.metric.*

const val MOTTATT_SYKMELDING = "${METRICS_NS}_mottatt_sykmelding_count"
const val MOTTATT_SYKMELDING_SUCCESS = "${METRICS_NS}_mottatt_sykmelding_success_count"
const val MOTTATT_SYKMELDING_IGNORED_MISMATCHED = "${METRICS_NS}_mottatt_sykmelding_ignored_mismatched_count"
const val MOTTATT_SYKMELDING_IGNORED_PARTNERID = "${METRICS_NS}_mottatt_sykmelding_ignored_partnerid_count"
const val MOTTATT_SYKMELDING_IGNORED_HERID = "${METRICS_NS}_mottatt_sykmelding_ignored_herid_count"
const val MOTTATT_SYKMELDING_IGNORED_BEHANDLERKATEGORI = "${METRICS_NS}_mottatt_sykmelding_ignored_behandlerkategori_count"

val COUNT_MOTTATT_SYKMELDING: Counter = Counter
    .builder(MOTTATT_SYKMELDING)
    .description("Counts the number of received sykmelding")
    .register(METRICS_REGISTRY)

val COUNT_MOTTATT_SYKMELDING_SUCCESS: Counter = Counter
    .builder(MOTTATT_SYKMELDING_SUCCESS)
    .description("Counts the number of received sykmelding that were successfully processed")
    .register(METRICS_REGISTRY)

val COUNT_MOTTATT_SYKMELDING_IGNORED_MISMATCHED: Counter = Counter
    .builder(MOTTATT_SYKMELDING_IGNORED_MISMATCHED)
    .description("Counts the number of received sykmelding that were ignored due to mismatched fnr for behandler")
    .register(METRICS_REGISTRY)

val COUNT_MOTTATT_SYKMELDING_IGNORED_PARTNERID: Counter = Counter
    .builder(MOTTATT_SYKMELDING_IGNORED_PARTNERID)
    .description("Counts the number of received sykmelding that were ignored due missing partnerid")
    .register(METRICS_REGISTRY)

val COUNT_MOTTATT_SYKMELDING_IGNORED_HERID: Counter = Counter
    .builder(MOTTATT_SYKMELDING_IGNORED_HERID)
    .description("Counts the number of received sykmelding that were ignored due missing kontor-herid")
    .register(METRICS_REGISTRY)

val COUNT_MOTTATT_SYKMELDING_IGNORED_BEHANDLERKATEGORI: Counter = Counter
    .builder(MOTTATT_SYKMELDING_IGNORED_BEHANDLERKATEGORI)
    .description("Counts the number of received sykmelding that were ignored due missing or invalid behandlerkategori")
    .register(METRICS_REGISTRY)
