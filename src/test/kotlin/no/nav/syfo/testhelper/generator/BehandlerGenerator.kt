package no.nav.syfo.testhelper.generator

import no.nav.syfo.behandler.domain.*
import no.nav.syfo.domain.*
import no.nav.syfo.testhelper.UserConstants
import java.time.OffsetDateTime
import java.util.UUID

fun generateBehandler(
    behandlerRef: UUID,
    partnerId: PartnerId,
    dialogmeldingEnabled: Boolean = true,
    dialogmeldingEnabledLocked: Boolean = false,
    kontornavn: String? = null,
    personident: Personident = UserConstants.FASTLEGE_FNR,
    herId: Int? = 77,
    hprId: Int = 9,
    orgnummer: String? = "123456789",
) = Behandler(
    behandlerRef = behandlerRef,
    kontor = BehandlerKontor(
        partnerId = partnerId,
        herId = 99,
        navn = kontornavn,
        adresse = "adresse",
        postnummer = "1234",
        poststed = "poststed",
        orgnummer = orgnummer?.let { Virksomhetsnummer(it) },
        dialogmeldingEnabled = dialogmeldingEnabled,
        dialogmeldingEnabledLocked = dialogmeldingEnabledLocked,
        system = null,
        mottatt = OffsetDateTime.now(),
    ),
    personident = personident,
    fornavn = "Dana",
    mellomnavn = "Katherine",
    etternavn = "Scully",
    herId = herId,
    hprId = hprId,
    telefon = null,
    kategori = BehandlerKategori.LEGE,
    mottatt = OffsetDateTime.now(),
)
