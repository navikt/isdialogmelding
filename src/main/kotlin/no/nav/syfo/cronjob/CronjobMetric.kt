package no.nav.syfo.cronjob

import io.micrometer.core.instrument.Counter
import no.nav.syfo.metric.METRICS_NS
import no.nav.syfo.metric.METRICS_REGISTRY

const val CRONJOB_METRICS_BASE = "${METRICS_NS}_cronjob"
const val CRONJOB_DIALOGMELDING_SEND = "${CRONJOB_METRICS_BASE}_dialogmelding_send_count"
const val CRONJOB_DIALOGMELDING_SEND_FAIL = "${CRONJOB_METRICS_BASE}_dialogmelding_fail_count"

val COUNT_CRONJOB_DIALOGMELDING_SEND_COUNT: Counter = Counter
    .builder(CRONJOB_DIALOGMELDING_SEND)
    .description("Counts the number of updates in Cronjob - DialogmeldingSend")
    .register(METRICS_REGISTRY)
val COUNT_CRONJOB_DIALOGMELDING_FAIL_COUNT: Counter = Counter
    .builder(CRONJOB_DIALOGMELDING_SEND_FAIL)
    .description("Counts the number of failures in Cronjob - DialogmeldingSend")
    .register(METRICS_REGISTRY)
