package no.nav.syfo.application

import kotlinx.coroutines.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val log: Logger = LoggerFactory.getLogger("no.nav.syfo.application")

fun launchBackgroundTask(
    applicationState: ApplicationState,
    finallyNotReady: Boolean = true,
    action: suspend CoroutineScope.() -> Unit,
): Job = GlobalScope.launch {
    try {
        action()
    } catch (ex: Exception) {
        log.error("Exception received while launching background task. Terminating application.", ex)
    } finally {
        if (finallyNotReady) {
            applicationState.alive = false
            applicationState.ready = false
        } else {
            log.info("Background task exited normally")
        }
    }
}
