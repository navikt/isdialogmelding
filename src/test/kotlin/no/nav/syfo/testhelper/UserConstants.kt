package no.nav.syfo.testhelper

import no.nav.syfo.domain.PersonIdentNumber

object UserConstants {
    val ARBEIDSTAKER_FNR = PersonIdentNumber("12345678912")
    val ANNEN_ARBEIDSTAKER_FNR = PersonIdentNumber(ARBEIDSTAKER_FNR.value.replace("2", "8"))
    val ARBEIDSTAKER_ANNEN_FASTLEGE_HERID_FNR = PersonIdentNumber(ARBEIDSTAKER_FNR.value.replace("2", "7"))
    val ARBEIDSTAKER_ANNEN_FASTLEGE_SAMME_PARTNERINFO_FNR = PersonIdentNumber(ARBEIDSTAKER_FNR.value.replace("2", "9"))
    val ARBEIDSTAKER_VEILEDER_NO_ACCESS = PersonIdentNumber(ARBEIDSTAKER_FNR.value.replace("2", "1"))
    val ARBEIDSTAKER_UTEN_FASTLEGE_FNR = PersonIdentNumber(ARBEIDSTAKER_FNR.value.replace("2", "4"))
    val ARBEIDSTAKER_FASTLEGE_UTEN_FORELDREENHET_FNR = PersonIdentNumber(ARBEIDSTAKER_FNR.value.replace("2", "3"))
    val ARBEIDSTAKER_FASTLEGE_UTEN_PARTNERINFO_FNR = PersonIdentNumber(ARBEIDSTAKER_FNR.value.replace("2", "6"))
    val ARBEIDSTAKER_FASTLEGE_UTEN_FNR_HPRID_HERID_FNR = PersonIdentNumber(ARBEIDSTAKER_FNR.value.replace("5", "1"))
    val ARBEIDSTAKER_FASTLEGE_UTEN_FNR_FNR = PersonIdentNumber(ARBEIDSTAKER_FNR.value.replace("5", "2"))
    val ARBEIDSTAKER_FASTLEGE_UTEN_HPRID_FNR = PersonIdentNumber(ARBEIDSTAKER_FNR.value.replace("5", "3"))
    val ARBEIDSTAKER_FASTLEGE_UTEN_HERID_FNR = PersonIdentNumber(ARBEIDSTAKER_FNR.value.replace("5", "4"))

    val PERSON_FORNAVN = "Fornavn"
    val PERSON_MELLOMNAVN = "Mellomnavn"
    val PERSON_ETTERNAVN = "Etternavn"

    const val HERID = 404
    const val OTHER_HERID = 604
    const val HERID_UTEN_PARTNERINFO = 504
    const val PARTNERID = 321
    const val OTHER_PARTNERID = 456
    const val VEILEDER_IDENT = "Z999999"

    val FASTLEGE_FNR = PersonIdentNumber("12125678911")
    val FASTLEGE_ANNEN_FNR = PersonIdentNumber(FASTLEGE_FNR.value.replace("2", "4"))
}
