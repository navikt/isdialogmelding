package no.nav.syfo.behandler.database

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.application.database.toList
import no.nav.syfo.behandler.database.domain.PBehandlerKontor
import no.nav.syfo.behandler.domain.BehandlerKontor
import no.nav.syfo.domain.PartnerId
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
            dialogmelding_enabled_locked,
            system,
            created_at,
            updated_at,
            mottatt
            ) VALUES (DEFAULT, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) 
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
        it.setBoolean(9, kontor.dialogmeldingEnabledLocked)
        it.setString(10, kontor.system)
        it.setObject(11, now)
        it.setObject(12, now)
        it.setObject(13, kontor.mottatt)
        it.executeQuery().toList { getInt("id") }
    }

    if (behandlerKontorList.size != 1) {
        throw SQLException("Creating BehandlerKontor failed, no rows affected.")
    }

    return behandlerKontorList.first()
}

const val queryUpdateBehandlerKontorDialogmeldingEnabled =
    """
        UPDATE BEHANDLER_KONTOR SET dialogmelding_enabled=? WHERE partner_id=? AND NOT dialogmelding_enabled_locked
    """

fun DatabaseInterface.updateBehandlerKontorDialogmeldingEnabled(partnerId: PartnerId): Boolean {
    return this.connection.use { connection ->
        val rowCount = connection.prepareStatement(queryUpdateBehandlerKontorDialogmeldingEnabled)
            .use {
                it.setObject(1, OffsetDateTime.now())
                it.setString(2, partnerId.toString())
                it.executeUpdate()
            }
        connection.commit()

        rowCount > 0
    }
}

const val queryUpdateBehandlerKontorSystem =
    """
        UPDATE BEHANDLER_KONTOR SET system=?,updated_at=?,mottatt=? WHERE partner_id=?
    """

fun Connection.updateBehandlerKontorSystem(partnerId: PartnerId, kontor: BehandlerKontor) {
    val rowCount = prepareStatement(queryUpdateBehandlerKontorSystem)
        .use {
            it.setString(1, kontor.system)
            it.setObject(2, OffsetDateTime.now())
            it.setObject(3, kontor.mottatt)
            it.setString(4, partnerId.toString())
            it.executeUpdate()
        }
    if (rowCount != 1) {
        throw RuntimeException("No row in BEHANDLER_KONTOR with partner_id $partnerId")
    }
}

const val queryUpdateBehandlerKontorAddress =
    """
        UPDATE BEHANDLER_KONTOR SET adresse=?,postnummer=?,poststed=?,updated_at=?,mottatt=? WHERE partner_id=?
    """

fun Connection.updateBehandlerKontorAddress(partnerId: PartnerId, kontor: BehandlerKontor) {
    val rowCount = prepareStatement(queryUpdateBehandlerKontorAddress)
        .use {
            it.setString(1, kontor.adresse)
            it.setString(2, kontor.postnummer)
            it.setString(3, kontor.poststed)
            it.setObject(4, OffsetDateTime.now())
            it.setObject(5, kontor.mottatt)
            it.setString(6, partnerId.toString())
            it.executeUpdate()
        }
    if (rowCount != 1) {
        throw RuntimeException("No row in BEHANDLER_KONTOR with partner_id $partnerId")
    }
}

const val queryGetBehandlerKontorById =
    """
        SELECT * FROM BEHANDLER_KONTOR WHERE id = ?
    """

fun DatabaseInterface.getBehandlerKontorById(id: Int): PBehandlerKontor {
    return this.connection.use { connection ->
        connection.prepareStatement(queryGetBehandlerKontorById)
            .use {
                it.setInt(1, id)
                it.executeQuery().toList { toPBehandlerKontor() }
            }
    }.first()
}

const val queryGetBehandlerKontor =
    """
        SELECT * FROM BEHANDLER_KONTOR WHERE partner_id = ?
    """

fun Connection.getBehandlerKontor(partnerId: PartnerId): PBehandlerKontor? {
    return prepareStatement(queryGetBehandlerKontor)
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
        dialogmeldingEnabledLocked = getBoolean("dialogmelding_enabled_locked"),
        system = getString("system"),
        createdAt = getObject("created_at", OffsetDateTime::class.java),
        updatedAt = getObject("updated_at", OffsetDateTime::class.java),
        mottatt = getObject("mottatt", OffsetDateTime::class.java),
    )
