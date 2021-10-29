package no.nav.syfo.oppfolgingsplan.converter

import no.kith.xmlstds.msghead._2006_05_24.ObjectFactory
import no.kith.xmlstds.msghead._2006_05_24.XMLReceiver
import no.nav.syfo.oppfolgingsplan.domain.RSMottaker

class MottakerConverter(private val rsMottaker: RSMottaker?) {
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
                        .withOrganisationName(rsMottaker!!.navn)
                        .withIdent(
                            FACTORY.createXMLIdent()
                                .withId(rsMottaker.herId)
                                .withTypeId(
                                    FACTORY.createXMLCV()
                                        .withDN("Identifikator fra Helsetjenesteenhetsregisteret (HER-id)")
                                        .withS("2.16.578.1.12.4.1.1.9051")
                                        .withV("HER")
                                )
                        )
                        .withIdent(
                            FACTORY.createXMLIdent()
                                .withId(rsMottaker.orgnummer)
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
                                .withStreetAdr(rsMottaker.adresse)
                                .withPostalCode(rsMottaker.postnummer)
                                .withCity(rsMottaker.poststed)
                        )
                        .withHealthcareProfessional(
                            FACTORY.createXMLHealthcareProfessional()
                                .withRoleToPatient(
                                    FACTORY.createXMLCV()
                                        .withV("6")
                                        .withS("2.16.578.1.12.4.1.1.9034")
                                        .withDN("Fastlege")
                                )
                                .withFamilyName(rsMottaker.behandler!!.etternavn)
                                .withMiddleName(rsMottaker.behandler.mellomnavn)
                                .withGivenName(rsMottaker.behandler.fornavn)
                                .withIdent(
                                    FACTORY.createXMLIdent()
                                        .withId(rsMottaker.behandler.fnr)
                                        .withTypeId(
                                            FACTORY.createXMLCV()
                                                .withDN("Fødselsnummer Norsk fødselsnummer")
                                                .withS("2.16.578.1.12.4.1.1.8116")
                                                .withV("FNR")
                                        )
                                )
                                .withIdent(
                                    FACTORY.createXMLIdent()
                                        .withId(rsMottaker.behandler.hprId)
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
