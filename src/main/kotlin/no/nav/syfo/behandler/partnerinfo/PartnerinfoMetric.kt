package no.nav.syfo.behandler.partnerinfo

import io.micrometer.core.instrument.Counter
import no.nav.syfo.metric.METRICS_NS
import no.nav.syfo.metric.METRICS_REGISTRY

const val CAL_PARTNERINFO_BASE = "${METRICS_NS}_call_syfopartnerinfo"
const val CALL_PARTNERINFO_SUCCESS = "${CAL_PARTNERINFO_BASE}_success_count"
const val CALL_PARTNERINFO_EMPTY_RESPONSE = "${CAL_PARTNERINFO_BASE}_empty_response_count"
const val CALL_PARTNERINFO_MULTIPLE_RESPONSE = "${CAL_PARTNERINFO_BASE}_multiple_response_count"
const val CALL_PARTNERINFO_FAIL = "${CAL_PARTNERINFO_BASE}_fail_count"

val COUNT_CALL_PARTNERINFO_SUCCESS: Counter = Counter
    .builder(CALL_PARTNERINFO_SUCCESS)
    .description("Counts the number of successful calls to Syfopartnerinfo - partnerinfo")
    .register(METRICS_REGISTRY)
val COUNT_CALL_PARTNERINFO_EMPTY_RESPONSE: Counter = Counter
    .builder(CALL_PARTNERINFO_EMPTY_RESPONSE)
    .description("Counts the number of calls to Syfopartnerinfo - partnerinfo with empty response")
    .register(METRICS_REGISTRY)
val COUNT_CALL_PARTNERINFO_MULTIPLE_RESPONSE: Counter = Counter
    .builder(CALL_PARTNERINFO_MULTIPLE_RESPONSE)
    .description("Counts the number of calls to Syfopartnerinfo - partnerinfo with multiple partnerIds in response")
    .register(METRICS_REGISTRY)
val COUNT_CALL_PARTNERINFO_FAIL: Counter = Counter
    .builder(CALL_PARTNERINFO_FAIL)
    .description("Counts the number of failed calls to Syfopartnerinfo - partnerinfo")
    .register(METRICS_REGISTRY)
