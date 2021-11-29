package no.nav.syfo

import com.typesafe.config.ConfigFactory
import io.ktor.application.*
import io.ktor.config.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.Environment
import no.nav.syfo.application.api.apiModule
import no.nav.syfo.application.api.authentication.getWellKnown
import no.nav.syfo.application.database.applicationDatabase
import no.nav.syfo.application.database.databaseModule
import no.nav.syfo.application.mq.MQSender
import no.nav.syfo.behandler.BehandlerDialogmeldingService
import no.nav.syfo.behandler.kafka.launchKafkaTask
import no.nav.syfo.cronjob.cronjobModule
import no.nav.syfo.dialogmelding.DialogmeldingService
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

const val applicationPort = 8080

fun main() {
    val applicationState = ApplicationState(
        alive = true,
        ready = false
    )
    val logger = LoggerFactory.getLogger("ktor.application")
    val environment = Environment()
    val mqSender = MQSender(environment)
    val wellKnownInternalAzureAD = getWellKnown(environment.azureAppWellKnownUrl)

    val applicationEngineEnvironment = applicationEngineEnvironment {
        log = logger
        config = HoconApplicationConfig(ConfigFactory.load())

        connector {
            port = applicationPort
        }

        module {
            databaseModule(environment = environment)
            apiModule(
                applicationState = applicationState,
                database = applicationDatabase,
                environment = environment,
                mqSender = mqSender,
                wellKnownInternalAzureAD = wellKnownInternalAzureAD,
            )
        }
    }

    applicationEngineEnvironment.monitor.subscribe(ApplicationStarted) {
        applicationState.ready = true
        logger.info("Application is ready")
        if (environment.toggleKafkaProcessingEnabled) {

            val behandlerDialogmeldingService = BehandlerDialogmeldingService(
                database = applicationDatabase,
            )

            launchKafkaTask(
                applicationState = applicationState,
                applicationEnvironmentKafka = environment.kafka,
                behandlerDialogmeldingService = behandlerDialogmeldingService,
            )
            val dialogmeldingService = DialogmeldingService(
                behandlerDialogmeldingService = behandlerDialogmeldingService,
                mqSender = mqSender,
            )
            cronjobModule(
                applicationState = applicationState,
                environment = environment,
                mqSender = mqSender,
                behandlerDialogmeldingService = behandlerDialogmeldingService,
                dialogmeldingService = dialogmeldingService,
            )
        } else {
            logger.info("Kafka-processing and cronjob disabled")
        }
    }

    val server = embeddedServer(
        factory = Netty,
        environment = applicationEngineEnvironment,
    )

    Runtime.getRuntime().addShutdownHook(
        Thread {
            server.stop(10, 10, TimeUnit.SECONDS)
        }
    )

    server.start(wait = false)
}
