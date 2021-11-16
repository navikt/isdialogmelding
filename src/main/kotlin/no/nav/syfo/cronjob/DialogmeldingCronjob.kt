package no.nav.syfo.cronjob

interface DialogmeldingCronjob {
    suspend fun run()
    val initialDelayMinutes: Long
    val intervalDelayMinutes: Long
}
