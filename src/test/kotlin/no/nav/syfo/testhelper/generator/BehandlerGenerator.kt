package no.nav.syfo.testhelper.generator

import no.nav.syfo.behandler.domain.*
import no.nav.syfo.domain.*
import java.time.OffsetDateTime
import java.util.UUID

fun generateBehandler(behandlerRef: UUID, partnerId: PartnerId) = Behandler(
    behandlerRef = behandlerRef,
    kontor = BehandlerKontor(
        partnerId = partnerId,
        herId = 99,
        navn = null,
        adresse = "adresse",
        postnummer = "1234",
        poststed = "poststed",
        orgnummer = Virksomhetsnummer("123456789"),
        dialogmeldingEnabled = true,
        system = null,
        kildeTidspunkt = OffsetDateTime.now(),
    ),
    personident = PersonIdentNumber("12125678911"),
    fornavn = "Dana",
    mellomnavn = "Katherine",
    etternavn = "Scully",
    herId = 77,
    hprId = 9,
    telefon = null,
    kategori = BehandlerKategori.LEGE,
    kildeTidspunkt = OffsetDateTime.now(),
)
