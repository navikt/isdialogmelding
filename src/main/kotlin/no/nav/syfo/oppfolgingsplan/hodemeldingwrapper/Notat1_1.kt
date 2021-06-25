package no.nav.syfo.oppfolgingsplan.hodemeldingwrapper

import no.kith.xmlstds.dialog._2013_01_23.XMLNotat

class Notat1_1 : Notat {
    private val xmlNotat: XMLNotat? = null
    override val dokIdNotat: String?
        get() = xmlNotat!!.dokIdNotat
}
