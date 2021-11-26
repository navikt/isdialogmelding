package no.nav.syfo.dialogmelding.converter

import no.kith.xmlstds.msghead._2006_05_24.ObjectFactory
import no.kith.xmlstds.msghead._2006_05_24.XMLReceiver
import no.nav.syfo.behandler.domain.BehandlerDialogmeldingBestilling

class MottakerConverter(private val melding: BehandlerDialogmeldingBestilling) {
    private var receiver: XMLReceiver? = null
    fun getReceiver(): XMLReceiver {
        ensureOrganisation()
        return receiver!!
    }

    private fun ensureOrganisation() {
        if (receiver == null) {
            receiver = FACTORY.createXMLReceiver()
                .withOrganisation(
                    FACTORY.createXMLOrganisation()
                        .withOrganisationName(melding.behandler.kontor)
                        .withIdent(
                            FACTORY.createXMLIdent()
                                .withId(melding.behandler.parentHerId.toString())
                                .withTypeId(
                                    FACTORY.createXMLCV()
                                        .withDN("Identifikator fra Helsetjenesteenhetsregisteret (HER-id)")
                                        .withS("2.16.578.1.12.4.1.1.9051")
                                        .withV("HER")
                                )
                        )
                        .withIdent(
                            FACTORY.createXMLIdent()
                                .withId(melding.behandler.orgnummer!!.value)
                                .withTypeId(
                                    FACTORY.createXMLCV()
                                        .withDN("Organisasjonsnummeret i Enhetsregisteret")
                                        .withS("2.16.578.1.12.4.1.1.9051")
                                        .withV("ENH")
                                )
                        )
                        .withAddress(
                            FACTORY.createXMLAddress()
                                .withType(
                                    FACTORY.createXMLCS()
                                        .withDN("Besøksadresse")
                                        .withV("RES")
                                )
                                .withStreetAdr(melding.behandler.adresse)
                                .withPostalCode(melding.behandler.postnummer)
                                .withCity(melding.behandler.poststed)
                        )
                        .withHealthcareProfessional(
                            FACTORY.createXMLHealthcareProfessional()
                                .withRoleToPatient(
                                    FACTORY.createXMLCV()
                                        .withV("6")
                                        .withS("2.16.578.1.12.4.1.1.9034")
                                        .withDN("Fastlege")
                                )
                                .withFamilyName(melding.behandler.etternavn)
                                .withMiddleName(melding.behandler.mellomnavn)
                                .withGivenName(melding.behandler.fornavn)
                                .withIdent(
                                    FACTORY.createXMLIdent()
                                        .withId(melding.behandler.personident!!.value)
                                        .withTypeId(
                                            FACTORY.createXMLCV()
                                                .withDN("Fødselsnummer Norsk fødselsnummer")
                                                .withS("2.16.578.1.12.4.1.1.8116")
                                                .withV("FNR")
                                        )
                                )
                                .withIdent(
                                    FACTORY.createXMLIdent()
                                        .withId(melding.behandler.hprId.toString())
                                        .withTypeId(
                                            FACTORY.createXMLCV()
                                                .withDN("HPR-nummer")
                                                .withS("2.16.578.1.12.4.1.1.8116")
                                                .withV("HPR")
                                        )
                                )
                        )
                )
        }
    }

    companion object {
        private val FACTORY = ObjectFactory()
    }
}
