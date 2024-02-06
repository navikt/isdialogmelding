package no.nav.syfo.testhelper.generator

import no.nav.syfo.behandler.domain.BehandlerKategori
import no.nav.syfo.behandler.fastlege.BehandlerKontorFraAdresseregisteretDTO
import no.nav.syfo.testhelper.UserConstants
import no.nav.syfo.testhelper.UserConstants.FASTLEGE_FNR
import no.nav.syfo.testhelper.UserConstants.HERID
import no.nav.syfo.testhelper.UserConstants.HPRID_INACTVE

fun generateBehandlerKontorResponse(
    kontorHerId: Int,
    aktiv: Boolean = true,
    behandlerHprIdInactive: Int,
) = BehandlerKontorFraAdresseregisteretDTO(
    aktiv = aktiv,
    herId = kontorHerId,
    navn = "Fastlegens kontor",
    besoksadresse = null,
    postadresse = generateBehandlerKontorAdresse(),
    telefon = "",
    epost = "",
    orgnummer = null,
    behandlere = listOf(generateBehandlerFraAdresseregisteret(behandlerHprIdInactive)),
)

fun generateBehandlerKontorAdresse() = BehandlerKontorFraAdresseregisteretDTO.Adresse(
    adresse = "Storgata 1",
    postnummer = "0651",
    poststed = "Oslo",
)

fun generateBehandlerFraAdresseregisteret(
    hprId: Int,
) = BehandlerKontorFraAdresseregisteretDTO.BehandlerFraAdresseregisteretDTO(
    aktiv = hprId != HPRID_INACTVE,
    fornavn = UserConstants.BEHANDLER_FORNAVN,
    mellomnavn = null,
    etternavn = UserConstants.BEHANDLER_ETTERNAVN,
    personIdent = FASTLEGE_FNR.value,
    herId = HERID,
    hprId = hprId,
    kategori = BehandlerKategori.LEGE.kategoriKode,
)
