package no.nav.syfo.behandler.kafka.sykmelding

import java.time.LocalDate
import java.time.LocalDateTime

data class ReceivedSykmeldingDTO(
    val sykmelding: Sykmelding,
    val personNrPasient: String,
    val personNrLege: String,
    val legeHelsepersonellkategori: String?,
    val legeHprNr: String?,
    val navLogId: String,
    val msgId: String,
    val legekontorOrgNr: String?,
    val legekontorHerId: String?,
    val legekontorReshId: String?,
    val legekontorOrgName: String,
    val mottattDato: LocalDateTime,
    val rulesetVersion: String?,
    val partnerreferanse: String?,
    val fellesformat: String,
)

data class Sykmelding(
    val id: String,
    val msgId: String,
    val medisinskVurdering: MedisinskVurdering,
    val behandletTidspunkt: LocalDateTime,
    val behandler: Behandler,
    val avsenderSystem: AvsenderSystem,
    val syketilfelleStartDato: LocalDate?,
    val signaturDato: LocalDateTime,
    val navnFastlege: String?
)

data class MedisinskVurdering(
    val hovedDiagnose: Diagnose?,
    val biDiagnoser: List<Diagnose>,
)

data class Diagnose(
    val system: String,
    val kode: String,
    val tekst: String?
)

data class Behandler(
    val fornavn: String,
    val mellomnavn: String?,
    val etternavn: String,
    val aktoerId: String,
    val fnr: String,
    val hpr: String?,
    val her: String?,
    val adresse: Adresse,
    val tlf: String?
)

data class Adresse(
    val gate: String?,
    val postnummer: Int?,
    val kommune: String?,
    val postboks: String?,
    val land: String?
)

data class AvsenderSystem(
    val navn: String,
    val versjon: String
)
