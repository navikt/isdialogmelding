package no.nav.syfo.cronjob

import io.micrometer.core.instrument.Counter
import no.nav.syfo.metric.METRICS_NS
import no.nav.syfo.metric.METRICS_REGISTRY

const val CRONJOB_METRICS_BASE = "${METRICS_NS}_cronjob"
const val CRONJOB_DIALOGMELDING_SEND = "${CRONJOB_METRICS_BASE}_dialogmelding_send_count"
const val CRONJOB_DIALOGMELDING_OPPFOLGINGSPLAN_SEND = "${CRONJOB_METRICS_BASE}_dialogmelding_oppfolgingsplan_count"
const val CRONJOB_DIALOGMELDING_DIALOGMOTE_SEND = "${CRONJOB_METRICS_BASE}_dialogmelding_dialogmote_count"
const val CRONJOB_DIALOGMELDING_FORESPORSEL_SEND = "${CRONJOB_METRICS_BASE}_dialogmelding_foresporsel_count"
const val CRONJOB_DIALOGMELDING_NOTAT_SEND = "${CRONJOB_METRICS_BASE}_dialogmelding_notat_count"
const val CRONJOB_DIALOGMELDING_SEND_FAIL = "${CRONJOB_METRICS_BASE}_dialogmelding_fail_count"
const val CRONJOB_SUSPENSJON_FOUND = "${CRONJOB_METRICS_BASE}_suspensjon_found_count"

val COUNT_CRONJOB_DIALOGMELDING_SEND_COUNT: Counter = Counter
    .builder(CRONJOB_DIALOGMELDING_SEND)
    .description("Counts the number of updates in Cronjob - DialogmeldingSend")
    .register(METRICS_REGISTRY)
val COUNT_CRONJOB_DIALOGMELDING_OPPFOLGINGSPLAN_SEND_COUNT: Counter = Counter
    .builder(CRONJOB_DIALOGMELDING_OPPFOLGINGSPLAN_SEND)
    .description("Counts the number of updates in Cronjob - DialogmeldingSend - oppfølgingsplan")
    .register(METRICS_REGISTRY)
val COUNT_CRONJOB_DIALOGMELDING_DIALOGMOTE_SEND_COUNT: Counter = Counter
    .builder(CRONJOB_DIALOGMELDING_DIALOGMOTE_SEND)
    .description("Counts the number of updates in Cronjob - DialogmeldingSend - dialogmote")
    .register(METRICS_REGISTRY)
val COUNT_CRONJOB_DIALOGMELDING_FORESPORSEL_SEND_COUNT: Counter = Counter
    .builder(CRONJOB_DIALOGMELDING_FORESPORSEL_SEND)
    .description("Counts the number of updates in Cronjob - DialogmeldingSend - forespørsel")
    .register(METRICS_REGISTRY)
val COUNT_CRONJOB_DIALOGMELDING_NOTAT_SEND_COUNT: Counter = Counter
    .builder(CRONJOB_DIALOGMELDING_NOTAT_SEND)
    .description("Counts the number of updates in Cronjob - DialogmeldingSend - notat")
    .register(METRICS_REGISTRY)
val COUNT_CRONJOB_DIALOGMELDING_FAIL_COUNT: Counter = Counter
    .builder(CRONJOB_DIALOGMELDING_SEND_FAIL)
    .description("Counts the number of failures in Cronjob - DialogmeldingSend")
    .register(METRICS_REGISTRY)
val COUNT_CRONJOB_SUSPENSJON_FOUND_COUNT: Counter = Counter
    .builder(CRONJOB_SUSPENSJON_FOUND)
    .description("Counts the number of suspensjon found in Cronjob - SuspensjonCronjob")
    .register(METRICS_REGISTRY)
