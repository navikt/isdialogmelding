package no.nav.syfo.oppfolgingsplan.hodemeldingwrapper

import no.kith.xmlstds.dialog._2006_10_11.XMLNotat

class Notat1_0 : Notat {
    private val xmlNotat: XMLNotat? = null
    override val dokIdNotat: String?
        get() = xmlNotat!!.dokIdNotat
}
