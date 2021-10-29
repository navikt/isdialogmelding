package no.nav.syfo.application.database

import io.ktor.application.*
import no.nav.syfo.application.Environment
import no.nav.syfo.application.isDev
import no.nav.syfo.application.isProd

lateinit var applicationDatabase: DatabaseInterface

fun Application.databaseModule(
    environment: Environment,
) {
    isDev {
        applicationDatabase = Database(
            DatabaseConfig(
                jdbcUrl = "jdbc:postgresql://localhost:5432/isdialogmelding_dev",
                password = "password",
                username = "username",
            )
        )
    }

    isProd {
        applicationDatabase = Database(
            DatabaseConfig(
                jdbcUrl = environment.jdbcUrl(),
                username = environment.isdialogmeldingDbUsername,
                password = environment.isdialogmeldingDbPassword,
            )
        )
    }
}
