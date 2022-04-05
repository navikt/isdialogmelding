package no.nav.syfo.behandler.database

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.application.database.toList
import no.nav.syfo.behandler.database.domain.PBehandlerDialogmeldingKontor
import no.nav.syfo.behandler.domain.BehandlerKontor
import java.sql.*
import java.time.Instant

const val queryCreateBehandlerDialogmeldingKontor =
    """
        INSERT INTO BEHANDLER_DIALOGMELDING_KONTOR (
            id,
            partner_id,
            her_id,
            navn,
            adresse,
            postnummer,
            poststed,
            orgnummer,
            created_at,
            updated_at
            ) VALUES (DEFAULT, ?, ?, ?, ?, ?, ?, ?, ?, ?) 
            RETURNING ID;
    """

fun Connection.createBehandlerDialogmeldingKontor(
    kontor: BehandlerKontor,
): Int {
    val now = Timestamp.from(Instant.now())
    val behandlerDialogmeldingKontorList = this.prepareStatement(queryCreateBehandlerDialogmeldingKontor).use {
        it.setString(1, kontor.partnerId.toString())
        it.setString(2, kontor.herId?.toString())
        it.setString(3, kontor.navn)
        it.setString(4, kontor.adresse)
        it.setString(5, kontor.postnummer)
        it.setString(6, kontor.poststed)
        it.setString(7, kontor.orgnummer?.value)
        it.setTimestamp(8, now)
        it.setTimestamp(9, now)
        it.executeQuery().toList { getInt("id") }
    }

    if (behandlerDialogmeldingKontorList.size != 1) {
        throw SQLException("Creating BehandlerDialogmeldingKontor failed, no rows affected.")
    }

    return behandlerDialogmeldingKontorList.first()
}

const val queryGetBehandlerDialogmeldingKontorForId =
    """
        SELECT * FROM BEHANDLER_DIALOGMELDING_KONTOR WHERE id = ?
    """

fun DatabaseInterface.getBehandlerDialogmeldingKontorForId(id: Int): PBehandlerDialogmeldingKontor {
    return this.connection.use { connection ->
        connection.prepareStatement(queryGetBehandlerDialogmeldingKontorForId)
            .use {
                it.setInt(1, id)
                it.executeQuery().toList { toPBehandlerDialogmeldingKontor() }
            }
    }.first()
}

const val queryGetBehandlerDialogmeldingKontorForPartnerId =
    """
        SELECT * FROM BEHANDLER_DIALOGMELDING_KONTOR WHERE partner_id = ?
    """

fun Connection.getBehandlerDialogmeldingKontorForPartnerId(partnerId: Int): PBehandlerDialogmeldingKontor? {
    return prepareStatement(queryGetBehandlerDialogmeldingKontorForPartnerId)
        .use {
            it.setString(1, partnerId.toString())
            it.executeQuery().toList { toPBehandlerDialogmeldingKontor() }
        }.firstOrNull()
}

fun ResultSet.toPBehandlerDialogmeldingKontor(): PBehandlerDialogmeldingKontor =
    PBehandlerDialogmeldingKontor(
        id = getInt("id"),
        partnerId = getString("partner_id"),
        herId = getString("her_id")?.toInt(),
        navn = getString("navn"),
        adresse = getString("adresse"),
        postnummer = getString("postnummer"),
        poststed = getString("poststed"),
        orgnummer = getString("orgnummer"),
        createdAt = getTimestamp("created_at").toLocalDateTime(),
        updatedAt = getTimestamp("updated_at").toLocalDateTime(),
    )
