package no.nav.syfo.identhendelse

import kotlinx.coroutines.runBlocking
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.behandler.database.createBehandler
import no.nav.syfo.behandler.database.createBehandlerArbeidstakerRelasjon
import no.nav.syfo.behandler.database.createBehandlerKontor
import no.nav.syfo.behandler.domain.Arbeidstaker
import no.nav.syfo.behandler.domain.BehandlerArbeidstakerRelasjonstype
import no.nav.syfo.client.azuread.AzureAdClient
import no.nav.syfo.client.pdl.PdlClient
import no.nav.syfo.dialogmelding.bestilling.database.createBehandlerDialogmeldingBestilling
import no.nav.syfo.dialogmelding.bestilling.kafka.toDialogmeldingToBehandlerBestilling
import no.nav.syfo.domain.PartnerId
import no.nav.syfo.domain.Personident
import no.nav.syfo.identhendelse.database.getIdentCount
import no.nav.syfo.testhelper.ExternalMockEnvironment
import no.nav.syfo.testhelper.UserConstants
import no.nav.syfo.testhelper.dropData
import no.nav.syfo.testhelper.generator.generateBehandler
import no.nav.syfo.testhelper.generator.generateDialogmeldingToBehandlerBestillingDTO
import no.nav.syfo.testhelper.generator.generateKafkaIdenthendelseDTO
import no.nav.syfo.testhelper.getBehandlerArbeidstakerRelasjoner
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.OffsetDateTime
import java.util.*

class IdenthendelseServiceTest {

    private val externalMockEnvironment = ExternalMockEnvironment.instance
    private val database = externalMockEnvironment.database
    private val azureAdClient = AzureAdClient(
        azureAppClientId = externalMockEnvironment.environment.aadAppClient,
        azureAppClientSecret = externalMockEnvironment.environment.azureAppClientSecret,
        azureOpenidConfigTokenEndpoint = externalMockEnvironment.environment.azureOpenidConfigTokenEndpoint,
        httpClient = externalMockEnvironment.mockHttpClient,
    )
    private val pdlClient = PdlClient(
        azureAdClient = azureAdClient,
        pdlClientId = externalMockEnvironment.environment.pdlClientId,
        pdlUrl = externalMockEnvironment.environment.pdlUrl,
        httpClient = externalMockEnvironment.mockHttpClient,
    )

    private val identhendelseService = IdenthendelseService(
        database = database,
        pdlClient = pdlClient,
    )

    @AfterEach
    fun afterEach() {
        database.dropData()
    }

    @Nested
    @DisplayName("Happy path")
    inner class HappyPath {
        @Test
        fun `Skal oppdatere gamle identer når person har fått ny ident`() {
            val kafkaIdenthendelseDTO = generateKafkaIdenthendelseDTO(hasOldPersonident = true)
            val newIdent = kafkaIdenthendelseDTO.getActivePersonident()!!
            val oldIdenter = kafkaIdenthendelseDTO.getInactivePersonidenter()

            populateDatabase(oldIdenter.first(), database)

            val oldBehandlerArbeidstaker = database.getBehandlerArbeidstakerRelasjoner(oldIdenter.first())
            val oldIdentUpdatedAt = oldBehandlerArbeidstaker.first().updatedAt

            runBlocking {
                identhendelseService.handleIdenthendelse(kafkaIdenthendelseDTO)
            }

            val newIdentOccurrences = database.getIdentCount(listOf(newIdent))
            assertEquals(2, newIdentOccurrences)
            val newBehandlerArbeidstaker = database.getBehandlerArbeidstakerRelasjoner(newIdent)
            assertTrue(newBehandlerArbeidstaker.first().updatedAt > oldIdentUpdatedAt)
        }

        @Test
        fun `Skal oppdatere gamle identer når person har fått ny ident, men kun tabeller som har en forekomst av gamle identer`() {
            val kafkaIdenthendelseDTO = generateKafkaIdenthendelseDTO(hasOldPersonident = true)
            val newIdent = kafkaIdenthendelseDTO.getActivePersonident()!!
            val oldIdenter = kafkaIdenthendelseDTO.getInactivePersonidenter()

            populateDatabase(
                oldIdent = oldIdenter.first(),
                database = database,
                updateInAllTables = false,
            )

            val oldBehandlerArbeidstaker = database.getBehandlerArbeidstakerRelasjoner(oldIdenter.first())
            val oldIdentUpdatedAt = oldBehandlerArbeidstaker.first().updatedAt

            runBlocking {
                identhendelseService.handleIdenthendelse(kafkaIdenthendelseDTO)
            }

            val newIdentOccurrences = database.getIdentCount(listOf(newIdent))
            assertEquals(1, newIdentOccurrences)
            val newBehandlerArbeidstaker = database.getBehandlerArbeidstakerRelasjoner(newIdent)
            assertTrue(newBehandlerArbeidstaker.first().updatedAt > oldIdentUpdatedAt)
        }
    }

    @Nested
    @DisplayName("Unhappy path")
    inner class UnhappyPath {
        @Test
        fun `Skal kaste feil hvis PDL ikke har oppdatert identen`() {
            val kafkaIdenthendelseDTO = generateKafkaIdenthendelseDTO(
                personident = UserConstants.TREDJE_ARBEIDSTAKER_FNR,
                hasOldPersonident = true,
            )
            val oldIdent = kafkaIdenthendelseDTO.getInactivePersonidenter().first()

            populateDatabase(oldIdent, database)

            assertThrows<IllegalStateException> {
                identhendelseService.handleIdenthendelse(kafkaIdenthendelseDTO)
            }
        }

        @Test
        fun `Skal kaste RuntimeException hvis PDL gir en not_found ved henting av identer`() {
            val kafkaIdenthendelseDTO = generateKafkaIdenthendelseDTO(
                personident = UserConstants.ARBEIDSTAKER_FNR_WITH_ERROR,
                hasOldPersonident = true,
            )
            val oldIdent = kafkaIdenthendelseDTO.getInactivePersonidenter().first()

            populateDatabase(oldIdent, database)

            assertThrows<RuntimeException> {
                identhendelseService.handleIdenthendelse(kafkaIdenthendelseDTO)
            }
        }
    }
}

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
