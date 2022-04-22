package no.nav.syfo.testhelper.generator

import no.nav.syfo.behandler.kafka.sykmelding.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

fun generateSykmeldingDTO(
    uuid: UUID,
    mottattTidspunkt: LocalDateTime = LocalDateTime.now(),
    behandletTidspunkt: LocalDateTime = LocalDateTime.now(),
    personNrPasient: String = "01010112345",
    personNrLege: String = "02020212345",
    behandlerFnr: String = "02020212345",
    herId: String = "123",
    hprId: String = "321",
    legeHelsepersonellkategori: String = "LE",
    partnerreferanse: String? = "123",
    avsenderSystemNavn: String = "EPJ-systemet",
    kontorHerId: String = "404",
) = ReceivedSykmeldingDTO(
    sykmelding = Sykmelding(
        id = "",
        msgId = "",
        medisinskVurdering = MedisinskVurdering(
            hovedDiagnose = null,
            biDiagnoser = emptyList(),
        ),
        behandletTidspunkt = behandletTidspunkt,
        behandler = Behandler(
            fornavn = "Anne",
            mellomnavn = "",
            etternavn = "Lege",
            fnr = behandlerFnr,
            hpr = hprId,
            her = herId,
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
            navn = avsenderSystemNavn,
            versjon = "1.0",
        ),
        syketilfelleStartDato = LocalDate.now(),
        signaturDato = LocalDateTime.now(),
        navnFastlege = "",
    ),
    personNrPasient = personNrPasient,
    personNrLege = personNrLege,
    legeHelsepersonellkategori = legeHelsepersonellkategori,
    legeHprNr = hprId,
    navLogId = "",
    msgId = uuid.toString(),
    legekontorOrgNr = null,
    legekontorHerId = kontorHerId,
    legekontorOrgName = "",
    mottattDato = mottattTidspunkt,
    partnerreferanse = partnerreferanse,
    fellesformat = "",
)
