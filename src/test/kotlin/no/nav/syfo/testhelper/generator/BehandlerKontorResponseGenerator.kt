package no.nav.syfo.testhelper.generator

import no.nav.syfo.behandler.fastlege.BehandlerKontorFraAdresseregisteretDTO

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
    behandlere = emptyList(),
)

fun generateBehandlerKontorAdresse() = BehandlerKontorFraAdresseregisteretDTO.Adresse(
    adresse = "Storgata 1",
    postnummer = "0651",
    poststed = "Oslo",
)
