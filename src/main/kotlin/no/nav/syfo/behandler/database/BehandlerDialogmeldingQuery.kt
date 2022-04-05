package no.nav.syfo.behandler.database

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.application.database.toList
import no.nav.syfo.behandler.database.domain.PBehandlerDialogmelding
import no.nav.syfo.behandler.domain.Behandler
import no.nav.syfo.domain.PersonIdentNumber
import java.sql.*
import java.time.Instant
import java.util.*

fun Connection.createBehandlerDialogmelding(
    behandler: Behandler,
    kontorId: Int,
): PBehandlerDialogmelding {
    val now = Timestamp.from(Instant.now())
    val behandlerDialogmeldingList = this.prepareStatement(queryCreateBehandlerDialogmelding).use {
        it.setString(1, behandler.behandlerRef.toString())
        it.setInt(2, kontorId)
        it.setString(3, behandler.personident?.value)
        it.setString(4, behandler.fornavn)
        it.setString(5, behandler.mellomnavn)
        it.setString(6, behandler.etternavn)
        it.setString(7, behandler.herId?.toString())
        it.setString(8, behandler.hprId?.toString())
        it.setString(9, behandler.telefon)
        it.setTimestamp(10, now)
        it.setTimestamp(11, now)
        it.executeQuery().toList { toPBehandlerDialogmelding() }
    }

    if (behandlerDialogmeldingList.size != 1) {
        throw SQLException("Creating BehandlerDialogmelding failed, no rows affected.")
    }

    return behandlerDialogmeldingList.first()
}

const val queryCreateBehandlerDialogmelding =
    """
        INSERT INTO BEHANDLER_DIALOGMELDING (
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
            ) VALUES (DEFAULT, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) 
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

const val queryGetBehandlerDialogmeldingMedPersonIdentForPartnerId =
    """
        SELECT B.* FROM BEHANDLER_DIALOGMELDING B INNER JOIN BEHANDLER_DIALOGMELDING_KONTOR K ON (K.id = B.kontor_id) 
        WHERE B.personident = ? and K.partner_id = ?
    """

fun DatabaseInterface.getBehandlerDialogmeldingMedPersonIdentForPartnerId(behandlerPersonIdent: PersonIdentNumber, partnerId: Int): PBehandlerDialogmelding? {
    return this.connection.use { connection ->
        connection.prepareStatement(queryGetBehandlerDialogmeldingMedPersonIdentForPartnerId)
            .use {
                it.setString(1, behandlerPersonIdent.value)
                it.setString(2, partnerId.toString())
                it.executeQuery().toList { toPBehandlerDialogmelding() }
            }
    }.firstOrNull()
}

const val queryGetBehandlerDialogmeldingMedHprIdForPartnerId =
    """
        SELECT B.* FROM BEHANDLER_DIALOGMELDING B INNER JOIN BEHANDLER_DIALOGMELDING_KONTOR K ON (K.id = B.kontor_id) 
        WHERE B.hpr_id = ? and K.partner_id = ?
    """

fun DatabaseInterface.getBehandlerDialogmeldingMedHprIdForPartnerId(hprId: Int, partnerId: Int): PBehandlerDialogmelding? {
    return this.connection.use { connection ->
        connection.prepareStatement(queryGetBehandlerDialogmeldingMedHprIdForPartnerId)
            .use {
                it.setString(1, hprId.toString())
                it.setString(2, partnerId.toString())
                it.executeQuery().toList { toPBehandlerDialogmelding() }
            }
    }.firstOrNull()
}

const val queryGetBehandlerDialogmeldingMedHerIdForPartnerId =
    """
        SELECT B.* FROM BEHANDLER_DIALOGMELDING B INNER JOIN BEHANDLER_DIALOGMELDING_KONTOR K ON (K.id = B.kontor_id) 
        WHERE B.her_id = ? and K.partner_id = ?
    """

fun DatabaseInterface.getBehandlerDialogmeldingMedHerIdForPartnerId(herId: Int, partnerId: Int): PBehandlerDialogmelding? {
    return this.connection.use { connection ->
        connection.prepareStatement(queryGetBehandlerDialogmeldingMedHerIdForPartnerId)
            .use {
                it.setString(1, herId.toString())
                it.setString(2, partnerId.toString())
                it.executeQuery().toList { toPBehandlerDialogmelding() }
            }
    }.firstOrNull()
}

const val queryGetBehandlerDialogmeldingForId =
    """
        SELECT * FROM BEHANDLER_DIALOGMELDING WHERE id = ?
    """

fun DatabaseInterface.getBehandlerDialogmeldingForId(id: Int): PBehandlerDialogmelding? {
    return this.connection.use { connection ->
        connection.prepareStatement(queryGetBehandlerDialogmeldingForId)
            .use {
                it.setInt(1, id)
                it.executeQuery().toList { toPBehandlerDialogmelding() }
            }
    }.firstOrNull()
}

const val queryGetBehandlerDialogmeldingForUuid =
    """
        SELECT * FROM BEHANDLER_DIALOGMELDING WHERE behandler_ref = ?
    """

fun DatabaseInterface.getBehandlerDialogmeldingForUuid(behandlerRef: UUID): PBehandlerDialogmelding? {
    return this.connection.use { connection ->
        connection.prepareStatement(queryGetBehandlerDialogmeldingForUuid)
            .use {
                it.setString(1, behandlerRef.toString())
                it.executeQuery().toList { toPBehandlerDialogmelding() }
            }
    }.firstOrNull()
}

const val queryGetBehandlerDialogmeldingForArbeidstakerPersonIdent =
    """
        SELECT BEHANDLER_DIALOGMELDING.* 
        FROM BEHANDLER_DIALOGMELDING
        INNER JOIN BEHANDLER_DIALOGMELDING_ARBEIDSTAKER ON BEHANDLER_DIALOGMELDING_ARBEIDSTAKER.behandler_dialogmelding_id = BEHANDLER_DIALOGMELDING.id
        AND BEHANDLER_DIALOGMELDING_ARBEIDSTAKER.arbeidstaker_personident = ?
        ORDER BY BEHANDLER_DIALOGMELDING_ARBEIDSTAKER.created_at DESC
    """

fun DatabaseInterface.getBehandlerDialogmeldingForArbeidstaker(personIdentNumber: PersonIdentNumber): List<PBehandlerDialogmelding> {
    return this.connection.use { connection ->
        connection.prepareStatement(queryGetBehandlerDialogmeldingForArbeidstakerPersonIdent)
            .use {
                it.setString(1, personIdentNumber.value)
                it.executeQuery().toList { toPBehandlerDialogmelding() }
            }
    }
}

fun ResultSet.toPBehandlerDialogmelding(): PBehandlerDialogmelding =
    PBehandlerDialogmelding(
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
