package no.nav.syfo.domain

data class Personident(val value: String) {
    init {
        if (!elevenDigits.matches(value)) {
            throw IllegalArgumentException("Value is not a valid Personident")
        }
    }
}

val elevenDigits = Regex("^\\d{11}\$")
