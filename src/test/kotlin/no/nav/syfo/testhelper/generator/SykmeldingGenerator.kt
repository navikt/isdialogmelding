package no.nav.syfo.testhelper.generator

import no.nav.syfo.behandler.kafka.sykmelding.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

fun generateSykmeldingDTO(uuid: UUID) = ReceivedSykmeldingDTO(
    sykmelding = Sykmelding(
        id = "",
        msgId = "",
        medisinskVurdering = MedisinskVurdering(
            hovedDiagnose = null,
            biDiagnoser = emptyList(),
        ),
        behandletTidspunkt = LocalDateTime.now(),
        behandler = Behandler(
            fornavn = "",
            mellomnavn = "",
            etternavn = "",
            fnr = "",
            hpr = "",
            her = "",
            adresse = Adresse(
                gate = "",
                postnummer = 0,
                kommune = "",
                postboks = "",
                land = "",
            ),
            tlf = "",
        ),
        avsenderSystem = AvsenderSystem(
            navn = "",
            versjon = "",
        ),
        syketilfelleStartDato = LocalDate.now(),
        signaturDato = LocalDateTime.now(),
        navnFastlege = "",
    ),
    personNrPasient = "",
    personNrLege = "",
    legeHelsepersonellkategori = null,
    legeHprNr = null,
    navLogId = "",
    msgId = uuid.toString(),
    legekontorOrgNr = null,
    legekontorHerId = null,
    legekontorOrgName = "",
    mottattDato = LocalDateTime.now(),
    partnerreferanse = null,
    fellesformat = "",
)
