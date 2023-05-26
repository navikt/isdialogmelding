package no.nav.syfo.behandler.fastlege

import io.micrometer.core.instrument.Counter
import no.nav.syfo.metric.METRICS_NS
import no.nav.syfo.metric.METRICS_REGISTRY

const val CALL_FASTLEGEREST_FASTLEGE_BASE = "${METRICS_NS}_call_fastlegerest_fastlege"
const val CALL_FASTLEGEREST_FASTLEGE_SUCCESS = "${CALL_FASTLEGEREST_FASTLEGE_BASE}_success_count"
const val CALL_FASTLEGEREST_VIKAR_BASE = "${METRICS_NS}_call_fastlegerest_vikar"
const val CALL_FASTLEGEREST_VIKAR_SUCCESS = "${CALL_FASTLEGEREST_VIKAR_BASE}_success_count"
const val CALL_FASTLEGEREST_FASTLEGE_FAIL = "${CALL_FASTLEGEREST_FASTLEGE_BASE}_fail_count"

val COUNT_CALL_FASTLEGEREST_FASTLEGE_SUCCESS: Counter = Counter
    .builder(CALL_FASTLEGEREST_FASTLEGE_SUCCESS)
    .description("Counts the number of successful calls to Fastlegerest - fastlege")
    .register(METRICS_REGISTRY)
val COUNT_CALL_FASTLEGEREST_FASTLEGE_FAIL: Counter = Counter
    .builder(CALL_FASTLEGEREST_FASTLEGE_FAIL)
    .description("Counts the number of failed calls to Fastlegerest - fastlege")
    .register(METRICS_REGISTRY)
val COUNT_CALL_FASTLEGEREST_VIKAR_SUCCESS: Counter = Counter
    .builder(CALL_FASTLEGEREST_VIKAR_SUCCESS)
    .description("Counts the number of successful calls to Fastlegerest - vikar")
    .register(METRICS_REGISTRY)
