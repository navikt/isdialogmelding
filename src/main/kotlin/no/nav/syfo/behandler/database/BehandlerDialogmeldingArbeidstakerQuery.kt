package no.nav.syfo.behandler.database

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.application.database.toList
import no.nav.syfo.behandler.database.domain.PBehandlerDialogmeldingArbeidstaker
import no.nav.syfo.behandler.domain.BehandlerDialogmeldingArbeidstaker
import no.nav.syfo.domain.PersonIdentNumber
import java.sql.*
import java.time.Instant
import java.util.UUID

const val queryCreateBehandlerDialogmeldingArbeidstaker =
    """
        INSERT INTO BEHANDLER_DIALOGMELDING_ARBEIDSTAKER (
            id,
            uuid,
            type,
            arbeidstaker_personident,
            created_at,
            behandler_dialogmelding_id
            ) VALUES (DEFAULT, ?, ?, ?, ?, ?) RETURNING id
    """

fun Connection.createBehandlerDialogmeldingArbeidstaker(
    behandlerDialogmeldingArbeidstaker: BehandlerDialogmeldingArbeidstaker,
    behandlerDialogmeldingId: Int,
) {
    val uuid = UUID.randomUUID()
    val idList = this.prepareStatement(queryCreateBehandlerDialogmeldingArbeidstaker).use {
        it.setString(1, uuid.toString())
        it.setString(2, behandlerDialogmeldingArbeidstaker.type.name)
        it.setString(3, behandlerDialogmeldingArbeidstaker.arbeidstakerPersonident.value)
        it.setTimestamp(4, Timestamp.from(Instant.now()))
        it.setInt(5, behandlerDialogmeldingId)
        it.executeQuery().toList { getInt("id") }
    }

    if (idList.size != 1) {
        throw SQLException("Creating BehandlerDialogmeldingArbeidstaker failed, no rows affected.")
    }
}

const val queryGetBehandlerDialogmeldingArbeidstaker =
    """
        SELECT BEHANDLER_DIALOGMELDING_ARBEIDSTAKER.* 
        FROM BEHANDLER_DIALOGMELDING
        INNER JOIN BEHANDLER_DIALOGMELDING_ARBEIDSTAKER ON (BEHANDLER_DIALOGMELDING_ARBEIDSTAKER.behandler_dialogmelding_id = BEHANDLER_DIALOGMELDING.id)
        WHERE BEHANDLER_DIALOGMELDING_ARBEIDSTAKER.arbeidstaker_personident = ?
        AND BEHANDLER_DIALOGMELDING.behandler_ref = ?
        ORDER BY BEHANDLER_DIALOGMELDING_ARBEIDSTAKER.created_at DESC
    """

fun DatabaseInterface.getBehandlerDialogmeldingArbeidstaker(
    personIdentNumber: PersonIdentNumber,
    behandlerRef: UUID,
): PBehandlerDialogmeldingArbeidstaker {
    val pBehandlerDialogmeldingArbeidstakerListe = this.connection.use { connection ->
        connection.prepareStatement(queryGetBehandlerDialogmeldingArbeidstaker)
            .use {
                it.setString(1, personIdentNumber.value)
                it.setString(2, behandlerRef.toString())
                it.executeQuery().toList { toPBehandlerDialogmeldingArbeidstaker() }
            }
    }
    return pBehandlerDialogmeldingArbeidstakerListe.first()
}

fun ResultSet.toPBehandlerDialogmeldingArbeidstaker(): PBehandlerDialogmeldingArbeidstaker =
    PBehandlerDialogmeldingArbeidstaker(
        id = getInt("id"),
        type = getString("type"),
        arbeidstakerPersonident = getString("arbeidstaker_personident"),
        createdAt = getTimestamp("created_at").toLocalDateTime(),
    )
