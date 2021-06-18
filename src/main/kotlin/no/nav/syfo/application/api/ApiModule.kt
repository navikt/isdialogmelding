package no.nav.syfo.application.api

import io.ktor.application.*
import io.ktor.routing.*
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.Environment
import no.nav.syfo.application.api.authentication.installContentNegotiation
import no.nav.syfo.application.api.authentication.installStatusPages
import no.nav.syfo.oppfolgingsplan.registerOppfolgingsplanApi

fun Application.apiModule(
    applicationState: ApplicationState,
//    mqSender: MQSenderInterface,
    environment: Environment,
) {
    installContentNegotiation()
//    installJwtAuthentication(
//        jwtIssuerList = listOf(
//            JwtIssuer(
//                accectedAudienceList = environment.loginserviceIdportenAudience,
//                jwtIssuerType = JwtIssuerType.selvbetjening,
//                wellKnown = wellKnownSelvbetjening,
//            ),
//            JwtIssuer(
//                accectedAudienceList = listOf(environment.loginserviceClientId),
//                jwtIssuerType = JwtIssuerType.veileder,
//                wellKnown = wellKnownVeileder,
//            )
//        ),
//    )
    installStatusPages()

//    val oppfolgingsplanService = OppfolgingsplanService(
//        env = environment,
//        mqSender = mqSender
//    )

    routing {
        registerPodApi(applicationState)
        registerPrometheusApi()
        registerOppfolgingsplanApi(
//            oppfolgingsplanservice = oppfolgingsplanService
        )

//        authenticate(JwtIssuerType.selvbetjening.name) {
//            registerOppfolgingsplanApi(
//                oppfolgingsplanService = oppfolgingsplanService,
//            )
//        }
    }
}
