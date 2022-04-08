package no.nav.syfo.behandler.database

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.application.database.toList
import no.nav.syfo.behandler.database.domain.PBehandlerKontor
import no.nav.syfo.behandler.domain.BehandlerKontor
import java.sql.*
import java.time.OffsetDateTime

const val queryCreateBehandlerKontor =
    """
        INSERT INTO BEHANDLER_KONTOR (
            id,
            partner_id,
            her_id,
            navn,
            adresse,
            postnummer,
            poststed,
            orgnummer,
            dialogmelding_enabled,
            created_at,
            updated_at
            ) VALUES (DEFAULT, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) 
            RETURNING ID;
    """

fun Connection.createBehandlerKontor(
    kontor: BehandlerKontor,
): Int {
    val now = OffsetDateTime.now()
    val behandlerKontorList = this.prepareStatement(queryCreateBehandlerKontor).use {
        it.setString(1, kontor.partnerId.toString())
        it.setString(2, kontor.herId?.toString())
        it.setString(3, kontor.navn)
        it.setString(4, kontor.adresse)
        it.setString(5, kontor.postnummer)
        it.setString(6, kontor.poststed)
        it.setString(7, kontor.orgnummer?.value)
        if (kontor.dialogmeldingEnabled) {
            it.setObject(8, now)
        } else {
            it.setNull(8, Types.TIMESTAMP_WITH_TIMEZONE)
        }
        it.setObject(9, now)
        it.setObject(10, now)
        it.executeQuery().toList { getInt("id") }
    }

    if (behandlerKontorList.size != 1) {
        throw SQLException("Creating BehandlerKontor failed, no rows affected.")
    }

    return behandlerKontorList.first()
}

const val queryUpdateDialogmeldingEnabled =
    """
        UPDATE BEHANDLER_KONTOR SET dialogmelding_enabled=? WHERE partner_id=?
    """

fun DatabaseInterface.updateDialogMeldingEnabledForPartnerId(partnerId: Int) {
    return this.connection.use { connection ->
        val rowCount = connection.prepareStatement(queryUpdateDialogmeldingEnabled)
            .use {
                it.setObject(1, OffsetDateTime.now())
                it.setString(2, partnerId.toString())
                it.executeUpdate()
            }
        if (rowCount != 1) {
            throw RuntimeException("No row in BEHANDLER_KONTOR with partner_id $partnerId")
        }
        connection.commit()
    }
}
const val queryGetBehandlerKontorForId =
    """
        SELECT * FROM BEHANDLER_KONTOR WHERE id = ?
    """

fun DatabaseInterface.getBehandlerKontorForId(id: Int): PBehandlerKontor {
    return this.connection.use { connection ->
        connection.prepareStatement(queryGetBehandlerKontorForId)
            .use {
                it.setInt(1, id)
                it.executeQuery().toList { toPBehandlerKontor() }
            }
    }.first()
}

const val queryGetBehandlerKontorForPartnerId =
    """
        SELECT * FROM BEHANDLER_KONTOR WHERE partner_id = ?
    """

fun Connection.getBehandlerKontorForPartnerId(partnerId: Int): PBehandlerKontor? {
    return prepareStatement(queryGetBehandlerKontorForPartnerId)
        .use {
            it.setString(1, partnerId.toString())
            it.executeQuery().toList { toPBehandlerKontor() }
        }.firstOrNull()
}

fun ResultSet.toPBehandlerKontor(): PBehandlerKontor =
    PBehandlerKontor(
        id = getInt("id"),
        partnerId = getString("partner_id"),
        herId = getString("her_id"),
        navn = getString("navn"),
        adresse = getString("adresse"),
        postnummer = getString("postnummer"),
        poststed = getString("poststed"),
        orgnummer = getString("orgnummer"),
        dialogmeldingEnabled = getObject("dialogmelding_enabled")?.let {
            getObject("dialogmelding_enabled", OffsetDateTime::class.java)
        },
        createdAt = getObject("created_at", OffsetDateTime::class.java),
        updatedAt = getObject("updated_at", OffsetDateTime::class.java),
    )