package no.nav.syfo.dialogmelding.bestilling.database

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.application.database.toList
import no.nav.syfo.dialogmelding.bestilling.domain.DialogmeldingToBehandlerBestilling
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
            kodeverk,
            kode,
            tekst,
            vedlegg,
            kilde,
            sendt,
            sendt_tries,
            created_at,
            updated_at
            ) VALUES (DEFAULT, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id
    """

fun Connection.createBehandlerDialogmeldingBestilling(
    dialogmeldingToBehandlerBestilling: DialogmeldingToBehandlerBestilling,
    behandlerId: Int,
): Int {
    val now = Timestamp.from(Instant.now())
    val idList = this.prepareStatement(queryCreateBehandlerDialogmeldingBestilling).use {
        it.setString(1, dialogmeldingToBehandlerBestilling.uuid.toString())
        it.setInt(2, behandlerId)
        it.setString(3, dialogmeldingToBehandlerBestilling.arbeidstakerPersonident.value)
        it.setString(4, dialogmeldingToBehandlerBestilling.parentRef)
        it.setString(5, dialogmeldingToBehandlerBestilling.conversationUuid.toString())
        it.setString(6, dialogmeldingToBehandlerBestilling.type.name)
        it.setString(7, dialogmeldingToBehandlerBestilling.kodeverk?.name)
        it.setInt(8, dialogmeldingToBehandlerBestilling.kode.value)
        it.setString(9, dialogmeldingToBehandlerBestilling.tekst)
        it.setBytes(10, dialogmeldingToBehandlerBestilling.vedlegg)
        it.setString(11, dialogmeldingToBehandlerBestilling.kilde)
        it.setTimestamp(12, null)
        it.setInt(13, 0)
        it.setTimestamp(14, now)
        it.setTimestamp(15, now)
        it.executeQuery().toList { getInt("id") }
    }

    if (idList.size != 1) {
        throw SQLException("Creating BehandlerDialogmeldingBestilling failed, no rows affected.")
    }
    return idList.first()
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

fun Connection.getBestilling(uuid: UUID): PDialogmeldingToBehandlerBestilling? {
    return this.prepareStatement(queryGetBestillinger)
        .use {
            it.setString(1, uuid.toString())
            it.executeQuery().toList { toPBehandlerDialogmeldingBestilling() }
        }.firstOrNull()
}

fun DatabaseInterface.getBestilling(uuid: UUID): PDialogmeldingToBehandlerBestilling? {
    return this.connection.use { connection ->
        connection.getBestilling(uuid)
    }
}

const val queryGetBestilling =
    """
        SELECT * FROM BEHANDLER_DIALOGMELDING_BESTILLING WHERE id = ?
    """

fun DatabaseInterface.getBestilling(id: Int): PDialogmeldingToBehandlerBestilling? {
    return this.connection.use { connection ->
        connection.prepareStatement(queryGetBestilling).use { it ->
            it.setInt(1, id)
            it.executeQuery().toList { toPBehandlerDialogmeldingBestilling() }
        }.firstOrNull()
    }
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
        kodeverk = getString("kodeverk"),
        kode = getInt("kode"),
        tekst = getString("tekst"),
        vedlegg = getBytes("vedlegg"),
        kilde = getString("kilde"),
        sendt = getTimestamp("sendt"),
        sendtTries = getInt("sendt_tries"),
    )
