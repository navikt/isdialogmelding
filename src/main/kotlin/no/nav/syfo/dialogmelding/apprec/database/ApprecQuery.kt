package no.nav.syfo.dialogmelding.apprec.database

import no.nav.syfo.application.database.*
import no.nav.syfo.dialogmelding.apprec.database.domain.PApprec
import no.nav.syfo.dialogmelding.apprec.domain.Apprec
import java.sql.*
import java.time.OffsetDateTime
import java.util.UUID

const val queryCreateApprec =
    """
        INSERT INTO Apprec (
            id,
            uuid,
            bestilling_id,
            status_kode,
            status_tekst,
            feil_kode,
            feil_tekst,             
            created_at,
            updated_at
        ) VALUES (DEFAULT, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id
    """

fun Connection.createApprec(
    apprec: Apprec,
    bestillingId: Int,
) {
    val now = OffsetDateTime.now()
    val idList = this.prepareStatement(queryCreateApprec).use {
        it.setString(1, apprec.uuid.toString())
        it.setInt(2, bestillingId)
        it.setString(3, apprec.statusKode.v)
        it.setString(4, apprec.statusTekst)
        it.setString(5, apprec.feilKode)
        it.setString(6, apprec.feilTekst)
        it.setObject(7, now)
        it.setObject(8, now)
        it.executeQuery().toList { getInt("id") }
    }

    if (idList.size != 1) {
        throw SQLException("Creating APPREC failed, no rows affected.")
    }
}

const val queryGetApprec =
    """
        SELECT * FROM Apprec WHERE uuid=?
    """

fun DatabaseInterface.getApprec(uuid: UUID): PApprec? {
    return this.connection.use {
        it.prepareStatement(queryGetApprec).use {
            it.setString(1, uuid.toString())
            it.executeQuery().toList { toPApprec() }.firstOrNull()
        }
    }
}

fun ResultSet.toPApprec(): PApprec =
    PApprec(
        id = getInt("id"),
        uuid = UUID.fromString(getString("uuid")),
        bestillingId = getInt("bestilling_id"),
        statusKode = getString("status_kode"),
        statusTekst = getString("status_tekst"),
        feilKode = getString("feil_kode"),
        feilTekst = getString("feil_tekst"),
        createdAt = getObject("created_at", OffsetDateTime::class.java),
        updatedAt = getObject("updated_at", OffsetDateTime::class.java),
    )
