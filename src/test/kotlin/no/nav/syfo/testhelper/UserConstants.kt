package no.nav.syfo.testhelper

import no.nav.syfo.domain.PartnerId
import no.nav.syfo.domain.Personident

object UserConstants {
    val ARBEIDSTAKER_FNR = Personident("12345678912")
    val ANNEN_ARBEIDSTAKER_FNR = Personident(ARBEIDSTAKER_FNR.value.replace("2", "8"))
    val TREDJE_ARBEIDSTAKER_FNR = Personident("12345678913")
    val ARBEIDSTAKER_FNR_WITH_ERROR = Personident("12345678666")
    val ARBEIDSTAKER_VEILEDER_NO_ACCESS = Personident(ARBEIDSTAKER_FNR.value.replace("2", "1"))
    val ARBEIDSTAKER_FNR_OPPFOLGINGSPLAN = Personident("01010112345")

    val ARBEIDSTAKER_UTEN_FASTLEGE_FNR = Personident(ARBEIDSTAKER_FNR.value.replace("2", "4"))
    val ARBEIDSTAKER_MED_FASTLEGE_UTEN_FORELDREENHET = Personident(ARBEIDSTAKER_FNR.value.replace("2", "3"))
    val ARBEIDSTAKER_MED_FASTLEGE_UTEN_PARTNERINFO = Personident(ARBEIDSTAKER_FNR.value.replace("2", "6"))
    val ARBEIDSTAKER_MED_FASTLEGE_MED_FLERE_PARTNERINFO = Personident(ARBEIDSTAKER_FNR.value.replace("2", "8"))
    val ARBEIDSTAKER_MED_FASTLEGE_UTEN_FNR_HPRID_HERID = Personident(ARBEIDSTAKER_FNR.value.replace("5", "1"))
    val ARBEIDSTAKER_MED_VIKARFASTLEGE = Personident(ARBEIDSTAKER_FNR.value.replace("5", "2"))

    val NARMESTELEDER_FNR = Personident(ARBEIDSTAKER_FNR.value.replace("5", "9"))

    val PARTNERID = PartnerId(321)
    val OTHER_PARTNERID = PartnerId(456)

    const val PERSON_FORNAVN = "Fornavn"
    const val PERSON_MELLOMNAVN = "Mellomnavn"
    const val PERSON_ETTERNAVN = "Etternavn"
    const val KONTOR_NAVN = "Legekontoret"

    const val HERID = 404
    const val HPRID = 1337
    const val HPRID_INACTIVE = 1338
    const val HPRID_UNKNOWN = 1339
    const val OTHER_HERID = 604
    const val OTHER_HPRID = 804
    const val HERID_UTEN_PARTNERINFO = 504
    const val HERID_MED_FLERE_PARTNERINFO = 704
    const val HERID_NOT_ACTIVE = 80434
    const val HERID_KONTOR_OK = 80433

    const val VEILEDER_IDENT = "Z999999"

    val FASTLEGE_FNR = Personident("12125678911")
    val FASTLEGE_FNR_SUSPENDERT = Personident("12125678912")
    val FASTLEGE_DNR = Personident("52125678911")
    val FASTLEGE_ANNEN_FNR = Personident(FASTLEGE_FNR.value.replace("2", "4"))

    val BEHANDLER_FORNAVN = "Behandler"
    val BEHANDLER_ETTERNAVN = "BehandlerEtternavn"
}
