package no.nav.syfo.application.api

import io.ktor.application.*
import io.ktor.routing.*
import no.nav.syfo.api.registerDialogmeldingApi
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.api.authentication.installContentNegotiation
import no.nav.syfo.application.api.authentication.installStatusPages
import no.nav.syfo.oppfolgingsplan.OppfolgingsplanService

fun Application.apiModule(
    applicationState: ApplicationState,
) {
    installContentNegotiation()
    installStatusPages()

//    val mqSender = MQSender(environment)

    val oppfolgingsplanService = OppfolgingsplanService(
//        mqSender = mqSender
    )

    routing {
        registerPodApi(applicationState)
        registerPrometheusApi()
        registerDialogmeldingApi(
            oppfolgingsplanService = oppfolgingsplanService
        )

//        authenticate(JwtIssuerType.selvbetjening.name) {
//            registerOppfolgingsplanApi(
//                oppfolgingsplanService = oppfolgingsplanService,
//            )
//        }
    }
}
