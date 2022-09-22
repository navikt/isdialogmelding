package no.nav.syfo.behandler.api.person

data class RSOppfolgingsplan(
    val sykmeldtFnr: String,
    val oppfolgingsplanPdf: ByteArray,
)
