package no.nav.syfo.dialogmelding.converter

import no.kith.xmlstds.msghead._2006_05_24.ObjectFactory
import no.kith.xmlstds.msghead._2006_05_24.XMLReceiver
import no.nav.syfo.behandler.domain.BehandlerDialogmeldingBestilling

fun createReceiver(
    melding: BehandlerDialogmeldingBestilling,
): XMLReceiver {
    val factory = ObjectFactory()
    return factory.createXMLReceiver()
        .withOrganisation(
            factory.createXMLOrganisation()
                .withOrganisationName(melding.behandler.kontor.navn)
                .withIdent(
                    factory.createXMLIdent()
                        .withId(melding.behandler.kontor.herId.toString())
                        .withTypeId(
                            factory.createXMLCV()
                                .withDN("Identifikator fra Helsetjenesteenhetsregisteret (HER-id)")
                                .withS("2.16.578.1.12.4.1.1.9051")
                                .withV("HER")
                        )
                )
                .withIdent(
                    factory.createXMLIdent()
                        .withId(melding.behandler.kontor.orgnummer!!.value)
                        .withTypeId(
                            factory.createXMLCV()
                                .withDN("Organisasjonsnummeret i Enhetsregisteret")
                                .withS("2.16.578.1.12.4.1.1.9051")
                                .withV("ENH")
                        )
                )
                .withAddress(
                    factory.createXMLAddress()
                        .withType(
                            factory.createXMLCS()
                                .withDN("Besøksadresse")
                                .withV("RES")
                        )
                        .withStreetAdr(melding.behandler.kontor.adresse)
                        .withPostalCode(melding.behandler.kontor.postnummer)
                        .withCity(melding.behandler.kontor.poststed)
                )
                .withHealthcareProfessional(
                    factory.createXMLHealthcareProfessional()
                        .withRoleToPatient(
                            factory.createXMLCV()
                                .withV("6")
                                .withS("2.16.578.1.12.4.1.1.9034")
                                .withDN("Fastlege")
                        )
                        .withFamilyName(melding.behandler.etternavn)
                        .withMiddleName(melding.behandler.mellomnavn)
                        .withGivenName(melding.behandler.fornavn)
                        .withIdent(
                            factory.createXMLIdent()
                                .withId(melding.behandler.personident!!.value)
                                .withTypeId(
                                    factory.createXMLCV()
                                        .withDN("Fødselsnummer Norsk fødselsnummer")
                                        .withS("2.16.578.1.12.4.1.1.8116")
                                        .withV("FNR")
                                )
                        )
                        .withIdent(
                            factory.createXMLIdent()
                                .withId(melding.behandler.hprId.toString())
                                .withTypeId(
                                    factory.createXMLCV()
                                        .withDN("HPR-nummer")
                                        .withS("2.16.578.1.12.4.1.1.8116")
                                        .withV("HPR")
                                )
                        )
                )
        )
}
