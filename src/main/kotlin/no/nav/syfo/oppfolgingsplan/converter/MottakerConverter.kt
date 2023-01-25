package no.nav.syfo.oppfolgingsplan.converter

import no.kith.xmlstds.msghead._2006_05_24.ObjectFactory
import no.kith.xmlstds.msghead._2006_05_24.XMLReceiver
import no.nav.syfo.domain.Personident
import no.nav.syfo.util.createXMLIdentForPersonident
import no.nav.syfo.oppfolgingsplan.domain.RSMottaker

fun createReceiver(
    rsMottaker: RSMottaker?,
): XMLReceiver {
    val factory = ObjectFactory()
    return factory.createXMLReceiver()
        .withOrganisation(
            factory.createXMLOrganisation()
                .withOrganisationName(rsMottaker!!.navn)
                .withIdent(
                    factory.createXMLIdent()
                        .withId(rsMottaker.herId)
                        .withTypeId(
                            factory.createXMLCV()
                                .withDN("Identifikator fra Helsetjenesteenhetsregisteret (HER-id)")
                                .withS("2.16.578.1.12.4.1.1.9051")
                                .withV("HER")
                        )
                )
                .withIdent(
                    factory.createXMLIdent()
                        .withId(rsMottaker.orgnummer)
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
                        .withStreetAdr(rsMottaker.adresse)
                        .withPostalCode(rsMottaker.postnummer)
                        .withCity(rsMottaker.poststed)
                )
                .withHealthcareProfessional(
                    factory.createXMLHealthcareProfessional()
                        .withRoleToPatient(
                            factory.createXMLCV()
                                .withV("6")
                                .withS("2.16.578.1.12.4.1.1.9034")
                                .withDN("Fastlege")
                        )
                        .withFamilyName(rsMottaker.behandler!!.etternavn)
                        .withMiddleName(rsMottaker.behandler.mellomnavn)
                        .withGivenName(rsMottaker.behandler.fornavn)
                        .withIdent(
                            rsMottaker.behandler.fnr?.let {
                                factory.createXMLIdentForPersonident(Personident(it))
                            }
                        )
                        .withIdent(
                            factory.createXMLIdent()
                                .withId(rsMottaker.behandler.hprId.toString())
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
