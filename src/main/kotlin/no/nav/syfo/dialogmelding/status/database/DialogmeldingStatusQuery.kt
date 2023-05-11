package no.nav.syfo.dialogmelding.status.database

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.application.database.toList
import no.nav.syfo.dialogmelding.status.domain.DialogmeldingStatus
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.time.OffsetDateTime
import java.util.*

const val queryCreateDialogmeldingStatus = """
   INSERT INTO DIALOGMELDING_STATUS (
    id,
    uuid,
    bestilling_id,
    status,
    tekst,
    created_at,
    updated_at
    ) VALUES (DEFAULT, ?, ? ,?, ?, ?, ?) RETURNING id 
"""

fun DatabaseInterface.createDialogmeldingStatus(
    dialogmeldingStatus: DialogmeldingStatus,
    bestillingId: Int,
    connection: Connection?,
) {
    if (connection != null) {
        connection.createDialogmeldingStatus(
            dialogmeldingStatus = dialogmeldingStatus,
            bestillingId = bestillingId,
        )
    } else {
        this.connection.use {
            it.createDialogmeldingStatus(
                dialogmeldingStatus = dialogmeldingStatus,
                bestillingId = bestillingId,
            )
            it.commit()
        }
    }
}

private fun Connection.createDialogmeldingStatus(dialogmeldingStatus: DialogmeldingStatus, bestillingId: Int) {
    val idList = this.prepareStatement(queryCreateDialogmeldingStatus).use {
        it.setString(1, dialogmeldingStatus.uuid.toString())
        it.setInt(2, bestillingId)
        it.setString(3, dialogmeldingStatus.status.name)
        it.setString(4, dialogmeldingStatus.tekst)
        it.setObject(5, dialogmeldingStatus.createdAt)
        it.setObject(6, OffsetDateTime.now())
        it.executeQuery().toList { getInt("id") }
    }

    if (idList.size != 1) {
        throw SQLException("Creating DIALOGMELDING_STATUS failed, no rows affected.")
    }
}

const val queryGetDialogmeldingStatusNotPublished = """
    SELECT * FROM DIALOGMELDING_STATUS WHERE published_at IS NULL ORDER BY created_at ASC LIMIT 100 
"""

fun DatabaseInterface.getDialogmeldingStatusNotPublished(): List<PDialogmeldingStatus> =
    this.connection.use { connection ->
        connection.prepareStatement(queryGetDialogmeldingStatusNotPublished).use {
            it.executeQuery().toList { toPDialogmeldingStatus() }
        }
    }

fun ResultSet.toPDialogmeldingStatus(): PDialogmeldingStatus =
    PDialogmeldingStatus(
        id = getInt("id"),
        uuid = UUID.fromString(getString("uuid")),
        bestillingId = getInt("bestilling_id"),
        status = getString("status"),
        tekst = getString("tekst"),
        createdAt = getObject("created_at", OffsetDateTime::class.java),
        updatedAt = getObject("updated_at", OffsetDateTime::class.java),
        publishedAt = getObject("published_at", OffsetDateTime::class.java)
    )
