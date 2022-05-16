package no.nav.syfo.behandler.database

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.application.database.toList
import no.nav.syfo.behandler.database.domain.PDialogmeldingToBehandlerBestilling
import no.nav.syfo.behandler.domain.DialogmeldingToBehandlerBestilling
import java.sql.*
import java.time.Instant
import java.util.UUID

const val queryCreateBehandlerDialogmeldingBestilling =
    """
        INSERT INTO BEHANDLER_DIALOGMELDING_BESTILLING (
            id,
            uuid,
            behandler_id,
            arbeidstaker_personident,
            parent,
            conversation,
            type,
            kode,
            tekst,
            vedlegg,
            sendt,
            sendt_tries,
            created_at,
            updated_at
            ) VALUES (DEFAULT, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id
    """

fun Connection.createBehandlerDialogmeldingBestilling(
    dialogmeldingToBehandlerBestilling: DialogmeldingToBehandlerBestilling,
    behandlerId: Int,
) {
    val now = Timestamp.from(Instant.now())
    val idList = this.prepareStatement(queryCreateBehandlerDialogmeldingBestilling).use {
        it.setString(1, dialogmeldingToBehandlerBestilling.uuid.toString())
        it.setInt(2, behandlerId)
        it.setString(3, dialogmeldingToBehandlerBestilling.arbeidstakerPersonident.value)
        it.setString(4, dialogmeldingToBehandlerBestilling.parentRef)
        it.setString(5, dialogmeldingToBehandlerBestilling.conversationUuid.toString())
        it.setString(6, dialogmeldingToBehandlerBestilling.type.name)
        it.setInt(7, dialogmeldingToBehandlerBestilling.kode.value)
        it.setString(8, dialogmeldingToBehandlerBestilling.tekst)
        it.setBytes(9, dialogmeldingToBehandlerBestilling.vedlegg)
        it.setTimestamp(10, null)
        it.setInt(11, 0)
        it.setTimestamp(12, now)
        it.setTimestamp(13, now)
        it.executeQuery().toList { getInt("id") }
    }

    if (idList.size != 1) {
        throw SQLException("Creating BehandlerDialogmeldingBestilling failed, no rows affected.")
    }
}

const val querySetBehandlerDialogmeldingBestillingSendt =
    """
        UPDATE BEHANDLER_DIALOGMELDING_BESTILLING SET sendt=?, sendt_tries=sendt_tries+1, updated_at=? WHERE uuid = ?
    """

fun DatabaseInterface.setBehandlerDialogmeldingBestillingSendt(
    uuid: UUID,
) {
    this.connection.use { connection ->
        val now = Timestamp.from(Instant.now())
        connection.prepareStatement(querySetBehandlerDialogmeldingBestillingSendt).use { ps ->
            ps.setTimestamp(1, now)
            ps.setTimestamp(2, now)
            ps.setString(3, uuid.toString())
            ps.execute()
        }
        connection.commit()
    }
}

const val queryIncrementBehandlerDialogmeldingBestillingSendtTries =
    """
        UPDATE BEHANDLER_DIALOGMELDING_BESTILLING SET sendt_tries=sendt_tries+1,updated_at=? WHERE uuid = ?
    """

fun DatabaseInterface.incrementDialogmeldingBestillingSendtTries(
    uuid: UUID,
) {
    this.connection.use { connection ->
        connection.prepareStatement(queryIncrementBehandlerDialogmeldingBestillingSendtTries).use { ps ->
            ps.setTimestamp(1, Timestamp.from(Instant.now()))
            ps.setString(2, uuid.toString())
            ps.execute()
        }
        connection.commit()
    }
}

const val queryGetBestillinger =
    """
        SELECT * FROM BEHANDLER_DIALOGMELDING_BESTILLING WHERE uuid = ?
    """

fun Connection.getBestillinger(uuid: UUID): PDialogmeldingToBehandlerBestilling? {
    return this.prepareStatement(queryGetBestillinger)
        .use {
            it.setString(1, uuid.toString())
            it.executeQuery().toList { toPBehandlerDialogmeldingBestilling() }
        }.firstOrNull()
}

const val queryGetBestillingerNotSent =
    """
        SELECT * FROM BEHANDLER_DIALOGMELDING_BESTILLING WHERE sendt is NULL ORDER BY id LIMIT 50
    """

fun DatabaseInterface.getDialogmeldingToBehandlerBestillingNotSendt(): List<PDialogmeldingToBehandlerBestilling> {
    return this.connection.use { connection ->
        connection.prepareStatement(queryGetBestillingerNotSent)
            .use {
                it.executeQuery().toList { toPBehandlerDialogmeldingBestilling() }
            }
    }
}

fun ResultSet.toPBehandlerDialogmeldingBestilling(): PDialogmeldingToBehandlerBestilling =
    PDialogmeldingToBehandlerBestilling(
        id = getInt("id"),
        uuid = UUID.fromString(getString("uuid")),
        behandlerId = getInt("behandler_id"),
        arbeidstakerPersonident = getString("arbeidstaker_personident"),
        parentRef = getString("parent"),
        conversationUuid = UUID.fromString(getString("conversation")),
        type = getString("type"),
        kode = getInt("kode"),
        tekst = getString("tekst"),
        vedlegg = getBytes("vedlegg"),
        sendt = getTimestamp("sendt"),
        sendtTries = getInt("sendt_tries"),
    )
