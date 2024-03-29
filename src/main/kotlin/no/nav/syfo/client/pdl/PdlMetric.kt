package no.nav.syfo.client.pdl

import io.micrometer.core.instrument.Counter
import no.nav.syfo.metric.METRICS_NS
import no.nav.syfo.metric.METRICS_REGISTRY

const val CALL_PDL_BASE = "${METRICS_NS}_call_pdl"
const val CALL_PDL_SUCCESS = "${CALL_PDL_BASE}_success_count"
const val CALL_PDL_FAIL = "${METRICS_NS}_call_pdl_fail_count"

val COUNT_CALL_PDL_SUCCESS: Counter = Counter
    .builder(CALL_PDL_SUCCESS)
    .description("Counts the number of successful calls to pdl - hentPerson")
    .register(METRICS_REGISTRY)
val COUNT_CALL_PDL_FAIL: Counter = Counter
    .builder(CALL_PDL_FAIL)
    .description("Counts the number of failed calls to pdl - hentPerson")
    .register(METRICS_REGISTRY)

const val CALL_PDL_IDENTER_BASE = "${METRICS_NS}_call_pdl_identer"
const val CALL_PDL_IDENTER_SUCCESS = "${CALL_PDL_IDENTER_BASE}_success_count"
const val CALL_PDL_IDENTER_FAIL = "${CALL_PDL_IDENTER_BASE}_fail_count"

val COUNT_CALL_PDL_IDENTER_SUCCESS: Counter = Counter.builder(CALL_PDL_IDENTER_SUCCESS)
    .description("Counts the number of successful calls to persondatalosning - hentIdenter")
    .register(METRICS_REGISTRY)
val COUNT_CALL_PDL_IDENTER_FAIL: Counter = Counter.builder(CALL_PDL_IDENTER_FAIL)
    .description("Counts the number of failed calls to persondatalosning - hentIdenter")
    .register(METRICS_REGISTRY)
