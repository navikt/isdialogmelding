package no.nav.syfo.behandler.database

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.application.database.toList
import no.nav.syfo.behandler.database.domain.PBehandler
import no.nav.syfo.behandler.domain.Behandler
import no.nav.syfo.domain.PersonIdentNumber
import java.sql.*
import java.time.Instant
import java.util.*

fun Connection.createBehandler(
    behandler: Behandler,
    kontorId: Int,
): PBehandler {
    val now = Timestamp.from(Instant.now())
    val behandlerList = this.prepareStatement(queryCreateBehandler).use {
        it.setString(1, behandler.behandlerRef.toString())
        it.setString(2, "LEGE") // default value for now, will be changed later
        it.setInt(3, kontorId)
        it.setString(4, behandler.personident?.value)
        it.setString(5, behandler.fornavn)
        it.setString(6, behandler.mellomnavn)
        it.setString(7, behandler.etternavn)
        it.setString(8, behandler.herId?.toString())
        it.setString(9, behandler.hprId?.toString())
        it.setString(10, behandler.telefon)
        it.setTimestamp(11, now)
        it.setTimestamp(12, now)
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
            updated_at
            ) VALUES (DEFAULT, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) 
            RETURNING
            id,
            behandler_ref,
            kontor_id,
            personident,
            fornavn,
            mellomnavn,
            etternavn,
            her_id,
            hpr_id,
            telefon,
            created_at,
            updated_at
    """

const val queryGetBehandlerMedPersonIdentForPartnerId =
    """
        SELECT B.* FROM BEHANDLER B INNER JOIN BEHANDLER_KONTOR K ON (K.id = B.kontor_id) 
        WHERE B.personident = ? and K.partner_id = ?
    """

fun DatabaseInterface.getBehandlerMedPersonIdentForPartnerId(behandlerPersonIdent: PersonIdentNumber, partnerId: Int): PBehandler? {
    return this.connection.use { connection ->
        connection.prepareStatement(queryGetBehandlerMedPersonIdentForPartnerId)
            .use {
                it.setString(1, behandlerPersonIdent.value)
                it.setString(2, partnerId.toString())
                it.executeQuery().toList { toPBehandler() }
            }
    }.firstOrNull()
}

const val queryGetBehandlerMedHprIdForPartnerId =
    """
        SELECT B.* FROM BEHANDLER B INNER JOIN BEHANDLER_KONTOR K ON (K.id = B.kontor_id) 
        WHERE B.hpr_id = ? and K.partner_id = ?
    """

fun DatabaseInterface.getBehandlerMedHprIdForPartnerId(hprId: Int, partnerId: Int): PBehandler? {
    return this.connection.use { connection ->
        connection.prepareStatement(queryGetBehandlerMedHprIdForPartnerId)
            .use {
                it.setString(1, hprId.toString())
                it.setString(2, partnerId.toString())
                it.executeQuery().toList { toPBehandler() }
            }
    }.firstOrNull()
}

const val queryGetBehandlerMedHerIdForPartnerId =
    """
        SELECT B.* FROM BEHANDLER B INNER JOIN BEHANDLER_KONTOR K ON (K.id = B.kontor_id) 
        WHERE B.her_id = ? and K.partner_id = ?
    """

fun DatabaseInterface.getBehandlerMedHerIdForPartnerId(herId: Int, partnerId: Int): PBehandler? {
    return this.connection.use { connection ->
        connection.prepareStatement(queryGetBehandlerMedHerIdForPartnerId)
            .use {
                it.setString(1, herId.toString())
                it.setString(2, partnerId.toString())
                it.executeQuery().toList { toPBehandler() }
            }
    }.firstOrNull()
}

const val queryGetBehandlerForId =
    """
        SELECT * FROM BEHANDLER WHERE id = ?
    """

fun DatabaseInterface.getBehandlerForId(id: Int): PBehandler? {
    return this.connection.use { connection ->
        connection.prepareStatement(queryGetBehandlerForId)
            .use {
                it.setInt(1, id)
                it.executeQuery().toList { toPBehandler() }
            }
    }.firstOrNull()
}

const val queryGetBehandlerForUuid =
    """
        SELECT * FROM BEHANDLER WHERE behandler_ref = ?
    """

fun DatabaseInterface.getBehandlerForUuid(behandlerRef: UUID): PBehandler? {
    return this.connection.use { connection ->
        connection.prepareStatement(queryGetBehandlerForUuid)
            .use {
                it.setString(1, behandlerRef.toString())
                it.executeQuery().toList { toPBehandler() }
            }
    }.firstOrNull()
}

const val queryGetBehandlerForArbeidstakerPersonIdent =
    """
        SELECT BEHANDLER.* 
        FROM BEHANDLER
        INNER JOIN BEHANDLER_ARBEIDSTAKER ON BEHANDLER_ARBEIDSTAKER.behandler_id = BEHANDLER.id
        AND BEHANDLER_ARBEIDSTAKER.arbeidstaker_personident = ?
        ORDER BY BEHANDLER_ARBEIDSTAKER.created_at DESC
    """

fun DatabaseInterface.getBehandlerForArbeidstaker(personIdentNumber: PersonIdentNumber): List<PBehandler> {
    return this.connection.use { connection ->
        connection.prepareStatement(queryGetBehandlerForArbeidstakerPersonIdent)
            .use {
                it.setString(1, personIdentNumber.value)
                it.executeQuery().toList { toPBehandler() }
            }
    }
}

fun ResultSet.toPBehandler(): PBehandler =
    PBehandler(
        id = getInt("id"),
        behandlerRef = UUID.fromString(getString("behandler_ref")),
        kontorId = getInt("kontor_id"),
        personident = getString("personident"),
        fornavn = getString("fornavn"),
        mellomnavn = getString("mellomnavn"),
        etternavn = getString("etternavn"),
        herId = getString("her_id"),
        hprId = getString("hpr_id"),
        telefon = getString("telefon"),
        createdAt = getTimestamp("created_at").toLocalDateTime(),
        updatedAt = getTimestamp("updated_at").toLocalDateTime(),
    )
