package no.nav.syfo.dialogmelding.apprec.domain

enum class ApprecStatus(val v: String, val dn: String) {
    avvist("2", "Avvist"),
    ok("1", "OK");

    companion object {
        fun fromV(v: String?): ApprecStatus? =
            ApprecStatus.values().firstOrNull { it.v == v }
    }
}
