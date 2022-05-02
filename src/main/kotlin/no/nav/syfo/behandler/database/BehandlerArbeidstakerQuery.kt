package no.nav.syfo.behandler.database

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.application.database.toList
import no.nav.syfo.behandler.database.domain.PBehandlerArbeidstaker
import no.nav.syfo.behandler.domain.BehandlerArbeidstakerRelasjon
import no.nav.syfo.domain.PersonIdentNumber
import java.sql.*
import java.time.*
import java.util.UUID

const val queryCreateBehandlerArbeidstakerRelasjon =
    """
        INSERT INTO BEHANDLER_ARBEIDSTAKER (
            id,
            uuid,
            type,
            arbeidstaker_personident,
            created_at,
            updated_at,
            behandler_id,
            kilde_tidspunkt
            ) VALUES (DEFAULT, ?, ?, ?, ?, ?, ?, ?) RETURNING id
    """

fun Connection.createBehandlerArbeidstakerRelasjon(
    behandlerArbeidstakerRelasjon: BehandlerArbeidstakerRelasjon,
    behandlerId: Int,
) {
    val uuid = UUID.randomUUID()
    val now = OffsetDateTime.now()
    val idList = this.prepareStatement(queryCreateBehandlerArbeidstakerRelasjon).use {
        it.setString(1, uuid.toString())
        it.setString(2, behandlerArbeidstakerRelasjon.type.name)
        it.setString(3, behandlerArbeidstakerRelasjon.arbeidstakerPersonident.value)
        it.setObject(4, now)
        it.setObject(5, now)
        it.setInt(6, behandlerId)
        it.setObject(7, behandlerArbeidstakerRelasjon.kildeTidspunkt)
        it.executeQuery().toList { getInt("id") }
    }

    if (idList.size != 1) {
        throw SQLException("Creating BEHANDLER_ARBEIDSTAKER failed, no rows affected.")
    }
}

const val queryUpdateBehandlerArbeidstakerRelasjon =
    """
        UPDATE BEHANDLER_ARBEIDSTAKER SET kilde_tidspunkt=?, updated_at=? WHERE 
        arbeidstaker_personident=? AND behandler_id=? AND type=? AND kilde_tidspunkt<?
    """

fun DatabaseInterface.updateBehandlerArbeidstakerRelasjon(
    behandlerArbeidstakerRelasjon: BehandlerArbeidstakerRelasjon,
    behandlerId: Int,
) {
    this.connection.use { connection ->
        connection.prepareStatement(queryUpdateBehandlerArbeidstakerRelasjon).use {
            it.setObject(1, behandlerArbeidstakerRelasjon.kildeTidspunkt)
            it.setObject(2, OffsetDateTime.now())
            it.setString(3, behandlerArbeidstakerRelasjon.arbeidstakerPersonident.value)
            it.setInt(4, behandlerId)
            it.setString(5, behandlerArbeidstakerRelasjon.type.name)
            it.setObject(6, behandlerArbeidstakerRelasjon.kildeTidspunkt)
            it.executeUpdate()
        }
        connection.commit()
    }
}

const val queryGetBehandlerArbeidstakerRelasjon =
    """
        SELECT BEHANDLER_ARBEIDSTAKER.* 
        FROM BEHANDLER
        INNER JOIN BEHANDLER_ARBEIDSTAKER ON (BEHANDLER_ARBEIDSTAKER.behandler_id = BEHANDLER.id)
        WHERE BEHANDLER_ARBEIDSTAKER.arbeidstaker_personident = ?
        AND BEHANDLER.behandler_ref = ?
        ORDER BY BEHANDLER_ARBEIDSTAKER.updated_at DESC
    """

fun DatabaseInterface.getBehandlerArbeidstakerRelasjon(
    personIdentNumber: PersonIdentNumber,
    behandlerRef: UUID,
): List<PBehandlerArbeidstaker> {
    val pBehandlerArbeidstakerListe = this.connection.use { connection ->
        connection.prepareStatement(queryGetBehandlerArbeidstakerRelasjon)
            .use {
                it.setString(1, personIdentNumber.value)
                it.setString(2, behandlerRef.toString())
                it.executeQuery().toList { toPBehandlerArbeidstaker() }
            }
    }
    return pBehandlerArbeidstakerListe
}

fun ResultSet.toPBehandlerArbeidstaker(): PBehandlerArbeidstaker =
    PBehandlerArbeidstaker(
        id = getInt("id"),
        type = getString("type"),
        arbeidstakerPersonident = getString("arbeidstaker_personident"),
        createdAt = getObject("created_at", OffsetDateTime::class.java),
        updatedAt = getObject("updated_at", OffsetDateTime::class.java),
        kildeTidspunkt = getObject("kilde_tidspunkt", OffsetDateTime::class.java),
    )
