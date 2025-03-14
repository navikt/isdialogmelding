package no.nav.syfo.domain

data class Personident(val value: String) {
    init {
        if (!elevenDigits.matches(value)) {
            throw IllegalArgumentException("Value is not a valid Personident")
        }
    }
}

val elevenDigits = Regex("^\\d{11}\$")

fun Personident.isDNR() = this.value[0].digitToInt() > 3

fun Personident.isDnrMatchingFnr(other: Personident) =
    isDNR() && !other.isDNR() && value.substring(1, 6) == other.value.substring(1, 6)
