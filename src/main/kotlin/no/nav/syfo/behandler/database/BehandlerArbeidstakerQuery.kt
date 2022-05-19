package no.nav.syfo.behandler.database

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.application.database.toList
import no.nav.syfo.behandler.database.domain.PBehandlerArbeidstaker
import no.nav.syfo.behandler.domain.*
import no.nav.syfo.domain.Personident
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
            mottatt
            ) VALUES (DEFAULT, ?, ?, ?, ?, ?, ?, ?) RETURNING id
    """

fun Connection.createBehandlerArbeidstakerRelasjon(
    arbeidstaker: Arbeidstaker,
    relasjonstype: BehandlerArbeidstakerRelasjonstype,
    behandlerId: Int,
) {
    val uuid = UUID.randomUUID()
    val now = OffsetDateTime.now()
    val idList = this.prepareStatement(queryCreateBehandlerArbeidstakerRelasjon).use {
        it.setString(1, uuid.toString())
        it.setString(2, relasjonstype.name)
        it.setString(3, arbeidstaker.arbeidstakerPersonident.value)
        it.setObject(4, now)
        it.setObject(5, now)
        it.setInt(6, behandlerId)
        it.setObject(7, arbeidstaker.mottatt)
        it.executeQuery().toList { getInt("id") }
    }

    if (idList.size != 1) {
        throw SQLException("Creating BEHANDLER_ARBEIDSTAKER failed, no rows affected.")
    }
}

const val queryUpdateBehandlerArbeidstakerRelasjon =
    """
        UPDATE BEHANDLER_ARBEIDSTAKER SET mottatt=?, updated_at=? WHERE 
        arbeidstaker_personident=? AND behandler_id=? AND type=? AND mottatt<?
    """

fun DatabaseInterface.updateBehandlerArbeidstakerRelasjon(
    arbeidstaker: Arbeidstaker,
    relasjonstype: BehandlerArbeidstakerRelasjonstype,
    behandlerId: Int,
) {
    this.connection.use { connection ->
        connection.prepareStatement(queryUpdateBehandlerArbeidstakerRelasjon).use {
            it.setObject(1, arbeidstaker.mottatt)
            it.setObject(2, OffsetDateTime.now())
            it.setString(3, arbeidstaker.arbeidstakerPersonident.value)
            it.setInt(4, behandlerId)
            it.setString(5, relasjonstype.name)
            it.setObject(6, arbeidstaker.mottatt)
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
    personident: Personident,
    behandlerRef: UUID,
): List<PBehandlerArbeidstaker> {
    val pBehandlerArbeidstakerListe = this.connection.use { connection ->
        connection.prepareStatement(queryGetBehandlerArbeidstakerRelasjon)
            .use {
                it.setString(1, personident.value)
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
        mottatt = getObject("mottatt", OffsetDateTime::class.java),
    )
