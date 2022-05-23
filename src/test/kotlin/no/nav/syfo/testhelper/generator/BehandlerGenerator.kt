package no.nav.syfo.testhelper.generator

import no.nav.syfo.behandler.domain.*
import no.nav.syfo.domain.*
import java.time.OffsetDateTime
import java.util.UUID

fun generateBehandler(
    behandlerRef: UUID,
    partnerId: PartnerId,
    dialogmeldingEnabled: Boolean = true,
    personident: Personident = Personident("12125678911"),
    herId: Int? = 77,
    hprId: Int = 9,
) = Behandler(
    behandlerRef = behandlerRef,
    kontor = BehandlerKontor(
        partnerId = partnerId,
        herId = 99,
        navn = null,
        adresse = "adresse",
        postnummer = "1234",
        poststed = "poststed",
        orgnummer = Virksomhetsnummer("123456789"),
        dialogmeldingEnabled = dialogmeldingEnabled,
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
