package no.nav.syfo.testhelper.generator

import no.nav.syfo.domain.Personident
import no.nav.syfo.identhendelse.kafka.IdentType
import no.nav.syfo.identhendelse.kafka.Identifikator
import no.nav.syfo.identhendelse.kafka.KafkaIdenthendelseDTO
import no.nav.syfo.testhelper.UserConstants

fun generateKafkaIdenthendelseDTO(
    personident: Personident = UserConstants.ARBEIDSTAKER_FNR,
    hasOldPersonident: Boolean,
): KafkaIdenthendelseDTO {
    val identifikatorer = mutableListOf(
        Identifikator(
            idnummer = personident.value,
            type = IdentType.FOLKEREGISTERIDENT,
            gjeldende = true,
        ),
        Identifikator(
            idnummer = "10$personident",
            type = IdentType.AKTORID,
            gjeldende = true
        ),
    )
    if (hasOldPersonident) {
        identifikatorer.addAll(
            listOf(
                Identifikator(
                    idnummer = UserConstants.ANNEN_ARBEIDSTAKER_FNR.value,
                    type = IdentType.FOLKEREGISTERIDENT,
                    gjeldende = false,
                ),
                Identifikator(
                    idnummer = "9${UserConstants.ANNEN_ARBEIDSTAKER_FNR.value.drop(1)}",
                    type = IdentType.FOLKEREGISTERIDENT,
                    gjeldende = false,
                ),
            )
        )
    }
    return KafkaIdenthendelseDTO(identifikatorer)
}
