package no.nav.syfo.testhelper.generator

import no.nav.syfo.behandler.domain.Behandler
import no.nav.syfo.behandler.domain.BehandlerType
import java.util.UUID

fun generateBehandler(behandlerRef: UUID, partnerId: Int) = Behandler(
    type = BehandlerType.FASTLEGE,
    behandlerRef = behandlerRef,
    personident = null,
    fornavn = "Leif",
    mellomnavn = null,
    etternavn = "Lege",
    herId = null,
    parentHerId = null,
    partnerId = partnerId,
    hprId = null,
    kontor = null,
    adresse = null,
    postnummer = null,
    poststed = null,
    orgnummer = null,
    telefon = null,
)
