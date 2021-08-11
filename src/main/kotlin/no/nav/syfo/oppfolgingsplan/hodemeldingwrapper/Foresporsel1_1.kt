package no.nav.syfo.oppfolgingsplan.hodemeldingwrapper

import no.kith.xmlstds.dialog._2013_01_23.XMLForesporsel

class Foresporsel1_1 : Foresporsel {
    private val xmlForesporsel: XMLForesporsel? = null
    override val dokIdForesporsel: String?
        get() = xmlForesporsel!!.dokIdForesp
}
