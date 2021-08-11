package no.nav.syfo.oppfolgingsplan.hodemeldingwrapper

import no.kith.xmlstds.dialog._2006_10_11.XMLForesporsel

class Foresporsel1_0 : Foresporsel {
    private val xmlForesporsel: XMLForesporsel? = null
    override val dokIdForesporsel: String?
        get() = xmlForesporsel!!.dokIdForesp
}
