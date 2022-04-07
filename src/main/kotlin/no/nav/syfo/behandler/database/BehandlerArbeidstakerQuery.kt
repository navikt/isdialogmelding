package no.nav.syfo.behandler.database

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.application.database.toList
import no.nav.syfo.behandler.database.domain.PBehandlerArbeidstaker
import no.nav.syfo.behandler.domain.BehandlerArbeidstakerRelasjon
import no.nav.syfo.domain.PersonIdentNumber
import java.sql.*
import java.time.Instant
import java.util.UUID

const val queryCreateBehandlerArbeidstakerRelasjon =
    """
        INSERT INTO BEHANDLER_ARBEIDSTAKER (
            id,
            uuid,
            type,
            arbeidstaker_personident,
            created_at,
            behandler_id
            ) VALUES (DEFAULT, ?, ?, ?, ?, ?) RETURNING id
    """

fun Connection.createBehandlerArbeidstakerRelasjon(
    behandlerArbeidstakerRelasjon: BehandlerArbeidstakerRelasjon,
    behandlerId: Int,
) {
    val uuid = UUID.randomUUID()
    val idList = this.prepareStatement(queryCreateBehandlerArbeidstakerRelasjon).use {
        it.setString(1, uuid.toString())
        it.setString(2, behandlerArbeidstakerRelasjon.type.name)
        it.setString(3, behandlerArbeidstakerRelasjon.arbeidstakerPersonident.value)
        it.setTimestamp(4, Timestamp.from(Instant.now()))
        it.setInt(5, behandlerId)
        it.executeQuery().toList { getInt("id") }
    }

    if (idList.size != 1) {
        throw SQLException("Creating BEHANDLER_ARBEIDSTAKER failed, no rows affected.")
    }
}

const val queryGetBehandlerArbeidstakerRelasjon =
    """
        SELECT BEHANDLER_ARBEIDSTAKER.* 
        FROM BEHANDLER
        INNER JOIN BEHANDLER_ARBEIDSTAKER ON (BEHANDLER_ARBEIDSTAKER.behandler_id = BEHANDLER.id)
        WHERE BEHANDLER_ARBEIDSTAKER.arbeidstaker_personident = ?
        AND BEHANDLER.behandler_ref = ?
        ORDER BY BEHANDLER_ARBEIDSTAKER.created_at DESC
    """

fun DatabaseInterface.getBehandlerArbeidstakerRelasjon(
    personIdentNumber: PersonIdentNumber,
    behandlerRef: UUID,
): PBehandlerArbeidstaker {
    val pBehandlerArbeidstakerListe = this.connection.use { connection ->
        connection.prepareStatement(queryGetBehandlerArbeidstakerRelasjon)
            .use {
                it.setString(1, personIdentNumber.value)
                it.setString(2, behandlerRef.toString())
                it.executeQuery().toList { toPBehandlerArbeidstaker() }
            }
    }
    return pBehandlerArbeidstakerListe.first()
}

fun ResultSet.toPBehandlerArbeidstaker(): PBehandlerArbeidstaker =
    PBehandlerArbeidstaker(
        id = getInt("id"),
        type = getString("type"),
        arbeidstakerPersonident = getString("arbeidstaker_personident"),
        createdAt = getTimestamp("created_at").toLocalDateTime(),
    )
