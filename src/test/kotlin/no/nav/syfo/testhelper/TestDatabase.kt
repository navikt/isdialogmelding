package no.nav.syfo.testhelper

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.behandler.database.*
import no.nav.syfo.behandler.domain.*
import no.nav.syfo.domain.PersonIdentNumber
import org.flywaydb.core.Flyway
import java.sql.Connection
import java.util.*

class TestDatabase : DatabaseInterface {
    private val pg: EmbeddedPostgres

    override val connection: Connection
        get() = pg.postgresDatabase.connection.apply {
            autoCommit = false
        }

    init {
        pg = try {
            EmbeddedPostgres.start()
        } catch (e: Exception) {
            EmbeddedPostgres.builder().setLocaleConfig("locale", "en_US").start()
        }

        Flyway.configure().run {
            dataSource(pg.postgresDatabase).load().migrate()
        }
    }

    fun stop() {
        pg.close()
    }
}

class TestDatabaseNotResponding : DatabaseInterface {

    override val connection: Connection
        get() = throw Exception("Not working")

    fun stop() {
    }
}

fun DatabaseInterface.createBehandlerForArbeidstaker(
    behandler: Behandler,
    arbeidstakerPersonIdent: PersonIdentNumber,
): UUID {
    this.connection.use { connection ->
        val pBehandlerKontor = connection.getBehandlerKontor(behandler.kontor.partnerId)
        val kontorId = if (pBehandlerKontor != null) {
            pBehandlerKontor.id
        } else {
            connection.createBehandlerKontor(behandler.kontor)
        }
        val createdBehandler =
            connection.createBehandler(behandler, kontorId)
        connection.createBehandlerArbeidstakerRelasjon(
            BehandlerArbeidstakerRelasjon(
                type = BehandlerArbeidstakerRelasjonstype.FASTLEGE,
                arbeidstakerPersonident = arbeidstakerPersonIdent,
            ),
            createdBehandler.id
        )
        connection.commit()

        return createdBehandler.behandlerRef
    }
}

fun DatabaseInterface.dropData() {
    val queryList = listOf(
        """
        DELETE FROM BEHANDLER_DIALOGMELDING_BESTILLING
        """.trimIndent(),
        """
        DELETE FROM BEHANDLER_ARBEIDSTAKER
        """.trimIndent(),
        """
        DELETE FROM BEHANDLER
        """.trimIndent(),
        """
        DELETE FROM BEHANDLER_KONTOR
        """.trimIndent(),
    )
    this.connection.use { connection ->
        queryList.forEach { query ->
            connection.prepareStatement(query).execute()
        }
        connection.commit()
    }
}
