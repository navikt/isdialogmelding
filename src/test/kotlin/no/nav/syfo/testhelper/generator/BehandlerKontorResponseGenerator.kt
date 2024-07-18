package no.nav.syfo.testhelper.generator

import no.nav.syfo.behandler.domain.BehandlerKategori
import no.nav.syfo.behandler.fastlege.BehandlerKontorFraAdresseregisteretDTO
import no.nav.syfo.testhelper.UserConstants
import no.nav.syfo.testhelper.UserConstants.FASTLEGE_ANNEN_FNR
import no.nav.syfo.testhelper.UserConstants.FASTLEGE_FNR
import no.nav.syfo.testhelper.UserConstants.HERID
import no.nav.syfo.testhelper.UserConstants.HPRID
import no.nav.syfo.testhelper.UserConstants.HPRID_INACTIVE
import no.nav.syfo.testhelper.UserConstants.OTHER_HERID

fun generateBehandlerKontorResponse(
    kontorHerId: Int,
    aktiv: Boolean = true,
) = BehandlerKontorFraAdresseregisteretDTO(
    aktiv = aktiv,
    herId = kontorHerId,
    navn = "Fastlegens kontor",
    besoksadresse = null,
    postadresse = generateBehandlerKontorAdresse(),
    telefon = "",
    epost = "",
    orgnummer = null,
    behandlere = listOf(
        generateBehandlerFraAdresseregisteret(HPRID_INACTIVE),
        generateBehandlerFraAdresseregisteret(HPRID)
    ),
)

fun generateBehandlerKontorAdresse() = BehandlerKontorFraAdresseregisteretDTO.Adresse(
    adresse = "Storgata 1",
    postnummer = "0651",
    poststed = "Oslo",
)

fun generateBehandlerFraAdresseregisteret(
    hprId: Int,
) = BehandlerKontorFraAdresseregisteretDTO.BehandlerFraAdresseregisteretDTO(
    aktiv = hprId != HPRID_INACTIVE,
    fornavn = UserConstants.BEHANDLER_FORNAVN,
    mellomnavn = null,
    etternavn = UserConstants.BEHANDLER_ETTERNAVN,
    personIdent = if (hprId == HPRID) FASTLEGE_FNR.value else FASTLEGE_ANNEN_FNR.value,
    herId = if (hprId == HPRID) HERID else OTHER_HERID,
    hprId = hprId,
    kategori = BehandlerKategori.LEGE.kategoriKode,
)
