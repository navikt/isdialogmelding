package no.nav.syfo

import com.typesafe.config.ConfigFactory
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.Environment
import no.nav.syfo.application.api.apiModule
import no.nav.syfo.application.api.authentication.getWellKnown
import no.nav.syfo.application.database.applicationDatabase
import no.nav.syfo.application.database.databaseModule
import no.nav.syfo.application.mq.MQSender
import no.nav.syfo.behandler.BehandlerService
import no.nav.syfo.behandler.DialogmeldingToBehandlerService
import no.nav.syfo.behandler.fastlege.FastlegeClient
import no.nav.syfo.behandler.kafka.*
import no.nav.syfo.behandler.partnerinfo.PartnerinfoClient
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.pdl.PdlClient
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
    val wellKnownInternalIdportenTokenX = getWellKnown(environment.idportenTokenXWellKnownUrl)
    val azureAdClient = AzureAdClient(
        azureAppClientId = environment.aadAppClient,
        azureAppClientSecret = environment.azureAppClientSecret,
        azureOpenidConfigTokenEndpoint = environment.azureOpenidConfigTokenEndpoint,
    )
    val fastlegeClient = FastlegeClient(
        azureAdClient = azureAdClient,
        fastlegeRestClientId = environment.fastlegeRestClientId,
        fastlegeRestUrl = environment.fastlegeRestUrl,
    )
    val partnerinfoClient = PartnerinfoClient(
        azureAdClient = azureAdClient,
        syfoPartnerinfoClientId = environment.syfoPartnerinfoClientId,
        syfoPartnerinfoUrl = environment.syfoPartnerinfoUrl,
    )
    lateinit var behandlerService: BehandlerService

    val applicationEngineEnvironment = applicationEngineEnvironment {
        log = logger
        config = HoconApplicationConfig(ConfigFactory.load())

        connector {
            port = applicationPort
        }

        module {
            databaseModule(environment = environment)

            behandlerService = BehandlerService(
                fastlegeClient = fastlegeClient,
                partnerinfoClient = partnerinfoClient,
                database = applicationDatabase,
                environment.toggleSykmeldingbehandlere,
            )

            apiModule(
                applicationState = applicationState,
                database = applicationDatabase,
                environment = environment,
                mqSender = mqSender,
                wellKnownInternalAzureAD = wellKnownInternalAzureAD,
                wellKnownInternalIdportenTokenX = wellKnownInternalIdportenTokenX,
                azureAdClient = azureAdClient,
                behandlerService = behandlerService,
            )
        }
    }

    applicationEngineEnvironment.monitor.subscribe(ApplicationStarted) {
        applicationState.ready = true
        logger.info("Application is ready, running Java VM ${Runtime.version()}")
        val pdlClient = PdlClient(
            azureAdClient = azureAdClient,
            pdlClientId = environment.pdlClientId,
            pdlUrl = environment.pdlUrl,
        )
        val dialogmeldingToBehandlerService = DialogmeldingToBehandlerService(
            database = applicationDatabase,
            pdlClient = pdlClient,
        )
        launchKafkaTaskDialogmeldingBestilling(
            applicationState = applicationState,
            applicationEnvironmentKafka = environment.kafka,
            dialogmeldingToBehandlerService = dialogmeldingToBehandlerService,
        )
        val dialogmeldingService = DialogmeldingService(
            dialogmeldingToBehandlerService = dialogmeldingToBehandlerService,
            mqSender = mqSender,
        )
        cronjobModule(
            applicationState = applicationState,
            environment = environment,
            mqSender = mqSender,
            dialogmeldingToBehandlerService = dialogmeldingToBehandlerService,
            dialogmeldingService = dialogmeldingService,
        )
        if (environment.toggleKafkaProcessingSykmeldingEnabled) {
            launchKafkaTaskSykmelding(
                applicationState = applicationState,
                applicationEnvironmentKafka = environment.kafka,
                behandlerService = behandlerService,
            )
        }
        if (environment.toggleKafkaProcessingDialogmeldingEnabled) {
            launchKafkaTaskDialogmeldingFromBehandler(
                applicationState = applicationState,
                applicationEnvironmentKafka = environment.kafka,
                database = applicationDatabase,
            )
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

    server.start(wait = true)
}
