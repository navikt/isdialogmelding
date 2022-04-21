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
            fornavn = "Anne",
            mellomnavn = "",
            etternavn = "Lege",
            fnr = "02020212345",
            hpr = "321",
            her = "123",
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
            navn = "EPJ-systemet",
            versjon = "1.0",
        ),
        syketilfelleStartDato = LocalDate.now(),
        signaturDato = LocalDateTime.now(),
        navnFastlege = "",
    ),
    personNrPasient = "01010112345",
    personNrLege = "02020212345",
    legeHelsepersonellkategori = "LE",
    legeHprNr = null,
    navLogId = "",
    msgId = uuid.toString(),
    legekontorOrgNr = null,
    legekontorHerId = null,
    legekontorOrgName = "",
    mottattDato = LocalDateTime.now(),
    partnerreferanse = "123",
    fellesformat = "",
)
