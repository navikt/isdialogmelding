package no.nav.syfo.testhelper.generator

import no.nav.syfo.behandler.domain.BehandlerKategori
import no.nav.syfo.behandler.fastlege.BehandlerKontorFraAdresseregisteretDTO
import no.nav.syfo.testhelper.UserConstants
import no.nav.syfo.testhelper.UserConstants.FASTLEGE_FNR
import no.nav.syfo.testhelper.UserConstants.HERID
import no.nav.syfo.testhelper.UserConstants.HPRID

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
    behandlere = listOf(generateBehandlerFraAdresseregisteret()),
)

fun generateBehandlerKontorAdresse() = BehandlerKontorFraAdresseregisteretDTO.Adresse(
    adresse = "Storgata 1",
    postnummer = "0651",
    poststed = "Oslo",
)

fun generateBehandlerFraAdresseregisteret() = BehandlerKontorFraAdresseregisteretDTO.BehandlerFraAdresseregisteretDTO(
    aktiv = true,
    fornavn = UserConstants.BEHANDLER_FORNAVN,
    mellomnavn = null,
    etternavn = UserConstants.BEHANDLER_ETTERNAVN,
    personIdent = FASTLEGE_FNR.value,
    herId = HERID,
    hprId = HPRID,
    kategori = BehandlerKategori.LEGE.kategoriKode,
)
