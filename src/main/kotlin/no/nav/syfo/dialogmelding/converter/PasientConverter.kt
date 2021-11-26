package no.nav.syfo.dialogmelding.converter

import no.kith.xmlstds.msghead._2006_05_24.ObjectFactory
import no.kith.xmlstds.msghead._2006_05_24.XMLPatient
import no.nav.syfo.behandler.domain.BehandlerDialogmeldingArbeidstaker
import no.nav.syfo.behandler.domain.BehandlerDialogmeldingBestilling

class PasientConverter(
    val melding: BehandlerDialogmeldingBestilling,
    val arbeidstakerNavn: BehandlerDialogmeldingArbeidstaker,
) {
    private var patient: XMLPatient? = null
    fun getPatient(): XMLPatient {
        ensurePasient()
        return patient!!
    }

    private fun ensurePasient() {
        if (patient == null) {
            patient = FACTORY.createXMLPatient()
                .withFamilyName(arbeidstakerNavn.etternavn)
                .withMiddleName(arbeidstakerNavn.mellomnavn)
                .withGivenName(arbeidstakerNavn.fornavn)
                .withIdent(
                    FACTORY.createXMLIdent()
                        .withId(arbeidstakerNavn.arbeidstakerPersonident.value)
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
