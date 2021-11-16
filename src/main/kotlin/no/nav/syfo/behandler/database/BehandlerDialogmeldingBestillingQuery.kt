package no.nav.syfo.behandler.database

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.application.database.toList
import no.nav.syfo.behandler.database.domain.PBehandlerDialogMeldingBestilling
import no.nav.syfo.behandler.domain.BehandlerDialogmeldingBestilling
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
    behandlerDialogmeldingBestilling: BehandlerDialogmeldingBestilling,
    behandlerId: Int,
) {
    val now = Timestamp.from(Instant.now())
    val idList = this.prepareStatement(queryCreateBehandlerDialogmeldingBestilling).use {
        it.setString(1, behandlerDialogmeldingBestilling.uuid.toString())
        it.setInt(2, behandlerId)
        it.setString(3, behandlerDialogmeldingBestilling.arbeidstakerPersonIdent.value)
        it.setString(4, behandlerDialogmeldingBestilling.parentUuid?.toString())
        it.setString(5, behandlerDialogmeldingBestilling.conversationUuid.toString())
        it.setString(6, behandlerDialogmeldingBestilling.type.name)
        it.setInt(7, behandlerDialogmeldingBestilling.kode.value)
        it.setString(8, behandlerDialogmeldingBestilling.tekst)
        it.setBytes(9, behandlerDialogmeldingBestilling.vedlegg)
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

const val queryGetBehandlerDialogmeldingBestilling =
    """
        SELECT * FROM BEHANDLER_DIALOGMELDING_BESTILLING WHERE uuid = ?
    """

fun Connection.getBehandlerDialogmeldingBestilling(uuid: UUID): PBehandlerDialogMeldingBestilling? {
    return this.prepareStatement(queryGetBehandlerDialogmeldingBestilling)
        .use {
            it.setString(1, uuid.toString())
            it.executeQuery().toList { toPBehandlerDialogmeldingBestilling() }
        }.firstOrNull()
}

const val queryGetBehandlerDialogmeldingBestillingNotSendt =
    """
        SELECT * FROM BEHANDLER_DIALOGMELDING_BESTILLING WHERE sendt is NULL
    """

fun DatabaseInterface.getBehandlerDialogmeldingBestillingNotSendt(): List<PBehandlerDialogMeldingBestilling> {
    return this.connection.use { connection ->
        connection.prepareStatement(queryGetBehandlerDialogmeldingBestillingNotSendt)
            .use {
                it.executeQuery().toList { toPBehandlerDialogmeldingBestilling() }
            }
    }
}

fun ResultSet.toPBehandlerDialogmeldingBestilling(): PBehandlerDialogMeldingBestilling =
    PBehandlerDialogMeldingBestilling(
        id = getInt("id"),
        uuid = UUID.fromString(getString("uuid")),
        behandlerId = getInt("behandler_id"),
        arbeidstakerPersonIdent = getString("arbeidstaker_personident"),
        parentUuid = getString("parent")?.let { UUID.fromString(it) },
        conversationUuid = UUID.fromString(getString("conversation")),
        type = getString("type"),
        kode = getInt("kode"),
        tekst = getString("type"),
        vedlegg = getBytes("vedlegg"),
    )
