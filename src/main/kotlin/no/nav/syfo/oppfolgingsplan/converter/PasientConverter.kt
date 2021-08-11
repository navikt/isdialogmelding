package no.nav.syfo.oppfolgingsplan.converter

import no.kith.xmlstds.msghead._2006_05_24.ObjectFactory
import no.kith.xmlstds.msghead._2006_05_24.XMLPatient
import no.nav.syfo.oppfolgingsplan.domain.RSPasient

class PasientConverter(private val rsPasient: RSPasient?) {
    private var patient: XMLPatient? = null
    fun getPatient(): XMLPatient {
        ensurePasient()
        return patient!!
    }

    private fun ensurePasient() {
        if (patient == null) {
            patient = FACTORY.createXMLPatient()
                .withFamilyName(rsPasient!!.etternavn)
                .withMiddleName(rsPasient.mellomnavn)
                .withGivenName(rsPasient.fornavn)
                .withIdent(
                    FACTORY.createXMLIdent()
                        .withId(rsPasient.fnr)
                        .withTypeId(
                            FACTORY.createXMLCV()
                                .withDN("Fødselsnummer")
                                .withS("2.16.578.1.12.4.1.1.8116")
                                .withV("FNR")
                        )
                )
        }
    }

    companion object {
        private val FACTORY = ObjectFactory()
    }
}
