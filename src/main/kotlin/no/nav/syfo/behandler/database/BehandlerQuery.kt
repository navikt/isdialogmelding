package no.nav.syfo.behandler.database

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.application.database.toList
import no.nav.syfo.behandler.database.domain.*
import no.nav.syfo.behandler.domain.*
import no.nav.syfo.domain.PartnerId
import no.nav.syfo.domain.Personident
import java.sql.*
import java.time.OffsetDateTime
import java.util.*

fun Connection.createBehandler(
    behandler: Behandler,
    kontorId: Int,
): PBehandler {
    val now = OffsetDateTime.now()
    val behandlerList = this.prepareStatement(queryCreateBehandler).use {
        it.setString(1, behandler.behandlerRef.toString())
        it.setString(2, behandler.kategori.name)
        it.setInt(3, kontorId)
        it.setString(4, behandler.personident?.value)
        it.setString(5, behandler.fornavn)
        it.setString(6, behandler.mellomnavn)
        it.setString(7, behandler.etternavn)
        it.setString(8, behandler.herId?.toString())
        it.setString(9, behandler.hprId?.toString())
        it.setString(10, behandler.telefon)
        it.setObject(11, now)
        it.setObject(12, now)
        it.setObject(13, behandler.mottatt)
        it.setNull(14, Types.TIMESTAMP_WITH_TIMEZONE)
        it.executeQuery().toList { toPBehandler() }
    }

    if (behandlerList.size != 1) {
        throw SQLException("Creating Behandler failed, no rows affected.")
    }

    return behandlerList.first()
}

const val queryCreateBehandler =
    """
        INSERT INTO BEHANDLER (
            id,
            behandler_ref,
            kategori,
            kontor_id,
            personident,
            fornavn,
            mellomnavn,
            etternavn,
            her_id,
            hpr_id,
            telefon,
            created_at,
            updated_at,
            mottatt,
            invalidated
            ) VALUES (DEFAULT, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) 
            RETURNING
            id,
            behandler_ref,
            kategori,
            kontor_id,
            personident,
            fornavn,
            mellomnavn,
            etternavn,
            her_id,
            hpr_id,
            telefon,
            created_at,
            updated_at,
            mottatt,
            invalidated
    """

const val queryGetBehandlerByBehandlerPersonidentAndPartnerId =
    """
        SELECT B.* FROM BEHANDLER B INNER JOIN BEHANDLER_KONTOR K ON (K.id = B.kontor_id) 
        WHERE B.personident = ? and K.partner_id = ?
    """

fun DatabaseInterface.getBehandlerByBehandlerPersonidentAndPartnerId(behandlerPersonident: Personident, partnerId: PartnerId): PBehandler? {
    return this.connection.use { connection ->
        connection.prepareStatement(queryGetBehandlerByBehandlerPersonidentAndPartnerId)
            .use {
                it.setString(1, behandlerPersonident.value)
                it.setString(2, partnerId.toString())
                it.executeQuery().toList { toPBehandler() }
            }
    }.firstOrNull()
}

const val queryGetBehandlerByHprIdAndPartnerId =
    """
        SELECT B.* FROM BEHANDLER B INNER JOIN BEHANDLER_KONTOR K ON (K.id = B.kontor_id) 
        WHERE B.hpr_id = ? and K.partner_id = ?
    """

fun DatabaseInterface.getBehandlerByHprIdAndPartnerId(hprId: Int, partnerId: PartnerId): PBehandler? {
    return this.connection.use { connection ->
        connection.prepareStatement(queryGetBehandlerByHprIdAndPartnerId)
            .use {
                it.setString(1, hprId.toString())
                it.setString(2, partnerId.toString())
                it.executeQuery().toList { toPBehandler() }
            }
    }.firstOrNull()
}

const val queryGetBehandlerByHerIdAndPartnerId =
    """
        SELECT B.* FROM BEHANDLER B INNER JOIN BEHANDLER_KONTOR K ON (K.id = B.kontor_id) 
        WHERE B.her_id = ? and K.partner_id = ?
    """

fun DatabaseInterface.getBehandlerByHerIdAndPartnerId(herId: Int, partnerId: PartnerId): PBehandler? {
    return this.connection.use { connection ->
        connection.prepareStatement(queryGetBehandlerByHerIdAndPartnerId)
            .use {
                it.setString(1, herId.toString())
                it.setString(2, partnerId.toString())
                it.executeQuery().toList { toPBehandler() }
            }
    }.firstOrNull()
}

const val queryGetBehandlerById =
    """
        SELECT * FROM BEHANDLER WHERE id = ?
    """

fun DatabaseInterface.getBehandlerById(id: Int): PBehandler? {
    return this.connection.use { connection ->
        connection.prepareStatement(queryGetBehandlerById)
            .use {
                it.setInt(1, id)
                it.executeQuery().toList { toPBehandler() }
            }
    }.firstOrNull()
}

const val queryGetBehandlerByBehandlerRef =
    """
        SELECT * FROM BEHANDLER WHERE behandler_ref = ?
    """

fun DatabaseInterface.getBehandlerByBehandlerRef(behandlerRef: UUID): PBehandler? {
    return this.connection.use { connection ->
        connection.prepareStatement(queryGetBehandlerByBehandlerRef)
            .use {
                it.setString(1, behandlerRef.toString())
                it.executeQuery().toList { toPBehandler() }
            }
    }.firstOrNull()
}

const val queryGetBehandlerAndRelasjonstype =
    """
        SELECT BEHANDLER.*,BEHANDLER_ARBEIDSTAKER.type 
        FROM BEHANDLER
        INNER JOIN BEHANDLER_ARBEIDSTAKER ON BEHANDLER_ARBEIDSTAKER.behandler_id = BEHANDLER.id
        AND BEHANDLER_ARBEIDSTAKER.arbeidstaker_personident = ?
        ORDER BY BEHANDLER_ARBEIDSTAKER.updated_at DESC
    """

fun DatabaseInterface.getBehandlerAndRelasjonstypeList(arbeidstakerIdent: Personident): List<Pair<PBehandler, BehandlerArbeidstakerRelasjonstype>> {
    return this.connection.use { connection ->
        connection.prepareStatement(queryGetBehandlerAndRelasjonstype)
            .use {
                it.setString(1, arbeidstakerIdent.value)
                it.executeQuery().toList { Pair(toPBehandler(), BehandlerArbeidstakerRelasjonstype.valueOf(getString("type"))) }
            }
    }
}

fun DatabaseInterface.getBehandlerByArbeidstaker(personident: Personident): List<PBehandler> {
    return getBehandlerAndRelasjonstypeList(personident).map { it.first }
}

const val queryUpdateBehandlerIdenter =
    """
        UPDATE BEHANDLER
        SET hpr_id = COALESCE(hpr_id, ?),
        her_id = COALESCE(her_id, ?),
        updated_at = ?
        WHERE behandler_ref=?
    """

fun DatabaseInterface.updateBehandlerIdenter(behandlerRef: UUID, identer: Map<BehandleridentType, String>) {
    this.connection.use { connection ->
        connection.prepareStatement(queryUpdateBehandlerIdenter)
            .use {
                it.setString(1, identer[BehandleridentType.HPR])
                it.setString(2, identer[BehandleridentType.HER])
                it.setObject(3, OffsetDateTime.now())
                it.setString(4, behandlerRef.toString())
                it.executeUpdate()
            }
        connection.commit()
    }
}

const val queryInvalidateBehandler =
    """
        UPDATE BEHANDLER
        SET invalidated=?,
        updated_at=?
        WHERE behandler_ref=?
    """

fun DatabaseInterface.invalidateBehandler(behandlerRef: UUID) {
    this.connection.use { connection ->
        connection.invalidateBehandler(behandlerRef)
        connection.commit()
    }
}

fun Connection.invalidateBehandler(behandlerRef: UUID) {
    val now = OffsetDateTime.now()
    this.prepareStatement(queryInvalidateBehandler)
        .use {
            it.setObject(1, now)
            it.setObject(2, now)
            it.setString(3, behandlerRef.toString())
            it.executeUpdate()
        }
}

fun ResultSet.toPBehandler(): PBehandler =
    PBehandler(
        id = getInt("id"),
        behandlerRef = UUID.fromString(getString("behandler_ref")),
        kategori = getString("kategori"),
        kontorId = getInt("kontor_id"),
        personident = getString("personident"),
        fornavn = getString("fornavn"),
        mellomnavn = getString("mellomnavn"),
        etternavn = getString("etternavn"),
        herId = getString("her_id"),
        hprId = getString("hpr_id"),
        telefon = getString("telefon"),
        createdAt = getObject("created_at", OffsetDateTime::class.java),
        updatedAt = getObject("updated_at", OffsetDateTime::class.java),
        mottatt = getObject("mottatt", OffsetDateTime::class.java),
        invalidated = getObject("invalidated", OffsetDateTime::class.java),
    )
