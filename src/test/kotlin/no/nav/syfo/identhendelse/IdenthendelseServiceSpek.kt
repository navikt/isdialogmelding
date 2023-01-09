package no.nav.syfo.identhendelse

import io.ktor.server.testing.*
import kotlinx.coroutines.*
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.behandler.database.createBehandler
import no.nav.syfo.behandler.database.createBehandlerArbeidstakerRelasjon
import no.nav.syfo.behandler.database.createBehandlerDialogmeldingBestilling
import no.nav.syfo.behandler.database.createBehandlerKontor
import no.nav.syfo.behandler.domain.Arbeidstaker
import no.nav.syfo.behandler.domain.BehandlerArbeidstakerRelasjonstype
import no.nav.syfo.behandler.kafka.dialogmeldingtobehandlerbestilling.toDialogmeldingToBehandlerBestilling
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.pdl.PdlClient
import no.nav.syfo.domain.PartnerId
import no.nav.syfo.domain.Personident
import no.nav.syfo.identhendelse.database.getIdentCount
import no.nav.syfo.testhelper.ExternalMockEnvironment
import no.nav.syfo.testhelper.UserConstants
import no.nav.syfo.testhelper.dropData
import no.nav.syfo.testhelper.generator.*
import org.amshove.kluent.internal.assertFailsWith
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.OffsetDateTime
import java.util.*

object IdenthendelseServiceSpek : Spek({

    describe(IdenthendelseServiceSpek::class.java.simpleName) {

        with(TestApplicationEngine()) {
            start()

            val externalMockEnvironment = ExternalMockEnvironment.instance
            val database = externalMockEnvironment.database
            val azureAdClient = AzureAdClient(
                azureAppClientId = externalMockEnvironment.environment.aadAppClient,
                azureAppClientSecret = externalMockEnvironment.environment.azureAppClientSecret,
                azureOpenidConfigTokenEndpoint = externalMockEnvironment.environment.azureOpenidConfigTokenEndpoint,
            )
            val pdlClient = PdlClient(
                azureAdClient = azureAdClient,
                pdlClientId = externalMockEnvironment.environment.pdlClientId,
                pdlUrl = externalMockEnvironment.environment.pdlUrl,
            )

            val identhendelseService = IdenthendelseService(
                database = database,
                pdlClient = pdlClient,
            )

            beforeEachTest {
                database.dropData()
            }

            describe("Happy path") {
                it("Skal oppdatere gamle identer når person har fått ny ident") {
                    val kafkaIdenthendelseDTO = generateKafkaIdenthendelseDTO(hasOldPersonident = true)
                    val newIdent = kafkaIdenthendelseDTO.getActivePersonident()!!
                    val oldIdenter = kafkaIdenthendelseDTO.getInactivePersonidenter()

                    populateDatabase(oldIdenter.first(), database)

                    runBlocking {
                        identhendelseService.handleIdenthendelse(kafkaIdenthendelseDTO)
                    }

                    val newIdentOccurrences = database.getIdentCount(listOf(newIdent))
                    newIdentOccurrences shouldBeEqualTo 2
                }

                it("Skal oppdatere gamle identer når person har fått ny ident, men kun tabeller som har en forekomst av gamle identer") {
                    val kafkaIdenthendelseDTO = generateKafkaIdenthendelseDTO(hasOldPersonident = true)
                    val newIdent = kafkaIdenthendelseDTO.getActivePersonident()!!
                    val oldIdenter = kafkaIdenthendelseDTO.getInactivePersonidenter()

                    populateDatabase(
                        oldIdent = oldIdenter.first(),
                        database = database,
                        updateInAllTables = false,
                    )

                    runBlocking {
                        identhendelseService.handleIdenthendelse(kafkaIdenthendelseDTO)
                    }

                    val newIdentOccurrences = database.getIdentCount(listOf(newIdent))
                    newIdentOccurrences shouldBeEqualTo 1
                }
            }

            describe("Unhappy path") {
                it("Skal kaste feil hvis PDL ikke har oppdatert identen") {
                    val kafkaIdenthendelseDTO = generateKafkaIdenthendelseDTO(
                        personident = UserConstants.TREDJE_ARBEIDSTAKER_FNR,
                        hasOldPersonident = true,
                    )
                    val oldIdent = kafkaIdenthendelseDTO.getInactivePersonidenter().first()

                    populateDatabase(oldIdent, database)

                    assertFailsWith(IllegalStateException::class) {
                        identhendelseService.handleIdenthendelse(kafkaIdenthendelseDTO)
                    }
                }
            }
        }
    }
})

fun populateDatabase(oldIdent: Personident, database: DatabaseInterface, updateInAllTables: Boolean = true) {
    val behandlerRef = UUID.randomUUID()
    val behandler = generateBehandler(
        behandlerRef = behandlerRef,
        partnerId = PartnerId(1),
        dialogmeldingEnabled = false,
        personident = oldIdent,
    )
    database.connection.use { connection ->
        val kontorId = connection.createBehandlerKontor(behandler.kontor)
        val createdBehandler = connection.createBehandler(behandler, kontorId)
        connection.createBehandlerArbeidstakerRelasjon(
            arbeidstaker = Arbeidstaker(
                arbeidstakerPersonident = oldIdent,
                mottatt = OffsetDateTime.now(),
            ),
            relasjonstype = BehandlerArbeidstakerRelasjonstype.FASTLEGE,
            behandlerId = createdBehandler.id,
        )

        if (updateInAllTables) {
            val dialogmeldingToBehandlerBestillingDTO = generateDialogmeldingToBehandlerBestillingDTO(
                uuid = UUID.randomUUID(),
                behandlerRef = behandlerRef,
                arbeidstakerPersonident = oldIdent,
            )
            val dialogmeldingToBehandlerBestilling = dialogmeldingToBehandlerBestillingDTO.toDialogmeldingToBehandlerBestilling(behandler)

            connection.createBehandlerDialogmeldingBestilling(
                dialogmeldingToBehandlerBestilling = dialogmeldingToBehandlerBestilling,
                behandlerId = createdBehandler.id,
            )
        }
        connection.commit()
    }
}
