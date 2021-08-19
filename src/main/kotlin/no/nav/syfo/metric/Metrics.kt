package no.nav.syfo.metric

import io.prometheus.client.Counter

const val METRICS_NS = "isdialogmelding"

const val SEND_OPPFOLGINGSPLAN_SUCCESS = "send_opppfolgingsplan_success_count"
const val SEND_OPPFOLGINGSPLAN_FAILED = "send_oppfolgingsplan_forbidden_count"

const val SEND_MESSAGE_EMOTTAK_MQ = "send_message_emottak_mq_count"

val COUNT_SEND_OPPFOLGINGSPLAN_SUCCESS: Counter = Counter.build()
    .namespace(METRICS_NS)
    .name(SEND_OPPFOLGINGSPLAN_SUCCESS)
    .help("Counts the number of successful posts to isdialogmelding oppfolgingsplan")
    .register()

val COUNT_SEND_OPPFOLGINGSPLAN_FAILED: Counter = Counter.build()
    .namespace(METRICS_NS)
    .name(SEND_OPPFOLGINGSPLAN_FAILED)
    .help("Counts the number of failed posts to isdialogmelding oppfolgingsplan")
    .register()

val COUNT_SEND_MESSAGE_EMOTTAK_MQ: Counter = Counter.build()
    .namespace(METRICS_NS)
    .name(SEND_MESSAGE_EMOTTAK_MQ)
    .help("Counts the number of messages sent to emottak on mq")
    .register()
