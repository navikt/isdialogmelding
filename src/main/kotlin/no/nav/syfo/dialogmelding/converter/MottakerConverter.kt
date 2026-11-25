package no.nav.syfo.dialogmelding.converter

import no.kith.xmlstds.msghead._2006_05_24.ObjectFactory
import no.kith.xmlstds.msghead._2006_05_24.XMLHealthcareProfessional
import no.kith.xmlstds.msghead._2006_05_24.XMLOrganisation
import no.kith.xmlstds.msghead._2006_05_24.XMLReceiver
import no.nav.syfo.behandler.domain.isTjeneste
import no.nav.syfo.dialogmelding.bestilling.domain.DialogmeldingToBehandlerBestilling
import no.nav.syfo.dialogmelding.bestilling.domain.DialogmeldingType
import no.nav.syfo.util.KODEVERK_FOR_ENHET_ID
import no.nav.syfo.util.KODEVERK_FOR_HELSEPERSONELL_TYPE
import no.nav.syfo.util.KODEVERK_FOR_PERSON_ID
import no.nav.syfo.util.createXMLIdentForPersonident

fun createReceiver(
    melding: DialogmeldingToBehandlerBestilling,
): XMLReceiver {
    val factory = ObjectFactory()
    val kontorOrganisation = createKontorOrganisation(factory, melding)
    if (melding.behandler.isTjeneste()) {
        kontorOrganisation.withOrganisation(createTjenesteOrganisation(factory, melding))
    } else {
        kontorOrganisation.withHealthcareProfessional(createHealthcareProfessional(factory, melding))
    }
    return factory.createXMLReceiver().withOrganisation(kontorOrganisation)
}

private fun createKontorOrganisation(
    factory: ObjectFactory,
    melding: DialogmeldingToBehandlerBestilling,
): XMLOrganisation =
    factory.createXMLOrganisation().apply {
        withOrganisationName(melding.behandler.kontor.navn)
        withIdent(
            factory.createXMLIdent()
                .withId(melding.behandler.kontor.herId.toString())
                .withTypeId(
                    factory.createXMLCV()
                        .withDN("Identifikator fra Helsetjenesteenhetsregisteret (HER-id)")
                        .withS(KODEVERK_FOR_ENHET_ID)
                        .withV("HER")
                )
        )
        if (melding.behandler.kontor.orgnummer != null) {
            withIdent(
                factory.createXMLIdent()
                    .withId(melding.behandler.kontor.orgnummer.value)
                    .withTypeId(
                        factory.createXMLCV()
                            .withDN("Organisasjonsnummeret i Enhetsregisteret")
                            .withS(KODEVERK_FOR_ENHET_ID)
                            .withV("ENH")
                    )
            )
        }
        withAddress(
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
    }

// Tjenester har bare HER-id (ingen HPR/personident) og adresseres som underorganisasjon av kontoret
private fun createTjenesteOrganisation(
    factory: ObjectFactory,
    melding: DialogmeldingToBehandlerBestilling,
): XMLOrganisation =
    factory.createXMLOrganisation().apply {
        withOrganisationName(melding.behandler.etternavn)
        if (melding.behandler.herId != null) {
            withIdent(
                factory.createXMLIdent()
                    .withId(melding.behandler.herId.toString())
                    .withTypeId(
                        factory.createXMLCV()
                            .withDN("Identifikator fra Helsetjenesteenhetsregisteret (HER-id)")
                            .withS(KODEVERK_FOR_ENHET_ID)
                            .withV("HER")
                    )
            )
        }
    }

private fun createHealthcareProfessional(
    factory: ObjectFactory,
    melding: DialogmeldingToBehandlerBestilling,
): XMLHealthcareProfessional =
    factory.createXMLHealthcareProfessional().apply {
        if (melding.type == DialogmeldingType.OPPFOLGINGSPLAN) {
            withRoleToPatient(
                factory.createXMLCV()
                    .withV("6")
                    .withS(KODEVERK_FOR_HELSEPERSONELL_TYPE)
                    .withDN("Fastlege")
            )
        }
        withFamilyName(melding.behandler.etternavn)
        withMiddleName(melding.behandler.mellomnavn)
        withGivenName(melding.behandler.fornavn)
        withIdent(
            melding.behandler.personident?.let {
                factory.createXMLIdentForPersonident(it)
            }
        )
        if (melding.behandler.hprId != null) {
            withIdent(
                factory.createXMLIdent()
                    .withId(melding.behandler.hprId.toString())
                    .withTypeId(
                        factory.createXMLCV()
                            .withDN("HPR-nummer")
                            .withS(KODEVERK_FOR_PERSON_ID)
                            .withV("HPR")
                    )
            )
        }
        if (melding.behandler.herId != null) {
            withIdent(
                factory.createXMLIdent()
                    .withId(melding.behandler.herId.toString())
                    .withTypeId(
                        factory.createXMLCV()
                            .withDN("Identifikator fra Helsetjenesteenhetsregisteret")
                            .withS(KODEVERK_FOR_PERSON_ID)
                            .withV("HER")
                    )
            )
        }
    }
