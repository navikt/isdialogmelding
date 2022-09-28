package no.nav.syfo.metric

import io.micrometer.core.instrument.Counter
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry

const val METRICS_NS = "isdialogmelding"

const val SEND_OPPFOLGINGSPLAN_SUCCESS = "${METRICS_NS}_send_opppfolgingsplan_success_count"
const val SEND_OPPFOLGINGSPLAN_FAILED = "${METRICS_NS}_send_oppfolgingsplan_forbidden_count"
const val SEND_MESSAGE_EMOTTAK_MQ = "${METRICS_NS}_send_message_emottak_mq_count"
const val RECEIVED_APPREC_SUCCESSFUL = "${METRICS_NS}_received_apprec_emottak_mq_count"
const val RECEIVED_APPREC_MESSAGE_QUEUE = "${METRICS_NS}_received_emottak_mq_count"

val METRICS_REGISTRY = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

val COUNT_SEND_OPPFOLGINGSPLAN_SUCCESS: Counter = Counter
    .builder(SEND_OPPFOLGINGSPLAN_SUCCESS)
    .description("Counts the number of successful posts to isdialogmelding oppfolgingsplan")
    .register(METRICS_REGISTRY)

val COUNT_SEND_OPPFOLGINGSPLAN_FAILED: Counter = Counter
    .builder(SEND_OPPFOLGINGSPLAN_FAILED)
    .description("Counts the number of failed posts to isdialogmelding oppfolgingsplan")
    .register(METRICS_REGISTRY)

val COUNT_SEND_MESSAGE_EMOTTAK_MQ: Counter = Counter
    .builder(SEND_MESSAGE_EMOTTAK_MQ)
    .description("Counts the number of messages sent to emottak on mq")
    .register(METRICS_REGISTRY)

val RECEIVED_APPREC_COUNTER: Counter = Counter
    .builder(RECEIVED_APPREC_SUCCESSFUL)
    .description("Counts the number of received apprecs from mq")
    .register(METRICS_REGISTRY)

val RECEIVED_APPREC_MESSAGE_COUNTER: Counter = Counter
    .builder(RECEIVED_APPREC_MESSAGE_QUEUE)
    .description("Counts the number of received apprecs from mq")
    .register(METRICS_REGISTRY)
