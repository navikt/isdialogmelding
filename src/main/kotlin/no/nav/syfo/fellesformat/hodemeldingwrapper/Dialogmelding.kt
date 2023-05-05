package no.nav.syfo.fellesformat.hodemeldingwrapper

import java.util.stream.Stream

interface Dialogmelding {
    @Suppress("ktlint")
    enum class Versjon {
        _1_0, _1_1
    }

    fun versjon(): Versjon?
    fun erForesporsel(): Boolean
    fun erNotat(): Boolean
    val dokIdForesporselStream: Stream<String?>?
    val dokIdNotatStream: Stream<String?>?
}
