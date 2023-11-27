package no.nav.syfo.behandler

import io.micrometer.core.instrument.Counter
import no.nav.syfo.metric.METRICS_NS
import no.nav.syfo.metric.METRICS_REGISTRY

const val BEHANDLER_BASE = "${METRICS_NS}_behandler"
const val BEHANDLER_UPDATED = "${BEHANDLER_BASE}_updated_count"

val COUNT_BEHANDLER_UPDATED: Counter = Counter
    .builder(BEHANDLER_UPDATED)
    .description("Counts the number of updates to behandler")
    .register(METRICS_REGISTRY)
