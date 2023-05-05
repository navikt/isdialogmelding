package no.nav.syfo.dialogmelding.apprec.domain

enum class ApprecStatus(val v: String, val dn: String) {
    AVVIST("2", "Avvist"),
    OK("1", "OK");

    companion object {
        fun fromV(v: String?): ApprecStatus? =
            ApprecStatus.values().firstOrNull { it.v == v }
    }
}
