package no.nav.syfo.testhelper.generator

import no.nav.syfo.behandler.domain.Behandler
import no.nav.syfo.behandler.domain.BehandlerKontor
import no.nav.syfo.domain.PersonIdentNumber
import no.nav.syfo.domain.Virksomhetsnummer
import java.util.UUID

fun generateBehandler(behandlerRef: UUID, partnerId: Int) = Behandler(
    behandlerRef = behandlerRef,
    kontor = BehandlerKontor(
        partnerId = partnerId,
        herId = 99,
        navn = null,
        adresse = "adresse",
        postnummer = "1234",
        poststed = "poststed",
        orgnummer = Virksomhetsnummer("123456789"),
    ),
    personident = PersonIdentNumber("12125678911"),
    fornavn = "Dana",
    mellomnavn = "Katherine",
    etternavn = "Scully",
    herId = 77,
    hprId = 9,
    telefon = null,
)
