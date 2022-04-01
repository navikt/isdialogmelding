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
): PBehandlerDialogmelding {
    val now = Timestamp.from(Instant.now())
    val behandlerDialogmeldingList = this.prepareStatement(queryCreateBehandlerDialogmelding).use {
        it.setString(1, behandler.behandlerRef.toString())
        it.setString(2, behandler.personident?.value)
        it.setString(3, behandler.fornavn)
        it.setString(4, behandler.mellomnavn)
        it.setString(5, behandler.etternavn)
        it.setString(6, behandler.partnerId.toString())
        it.setString(7, behandler.herId?.toString())
        it.setString(8, behandler.parentHerId?.toString())
        it.setString(9, behandler.hprId?.toString())
        it.setString(10, behandler.kontor)
        it.setString(11, behandler.adresse)
        it.setString(12, behandler.postnummer)
        it.setString(13, behandler.poststed)
        it.setString(14, behandler.orgnummer?.value)
        it.setString(15, behandler.telefon)
        it.setTimestamp(16, now)
        it.setTimestamp(17, now)
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
            personident,
            fornavn,
            mellomnavn,
            etternavn,
            partner_id,
            her_id,
            parent_her_id,
            hpr_id,
            kontor,
            adresse,
            postnummer,
            poststed,
            orgnummer,
            telefon,
            created_at,
            updated_at
            ) VALUES (DEFAULT, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) 
            RETURNING
            id,
            behandler_ref,
            personident,
            fornavn,
            mellomnavn,
            etternavn,
            partner_id,
            her_id,
            parent_her_id,
            hpr_id,
            kontor,
            adresse,
            postnummer,
            poststed,
            orgnummer,
            telefon,
            created_at,
            updated_at
    """

const val queryGetBehandlerDialogmeldingMedPersonIdentForPartnerId =
    """
        SELECT * FROM BEHANDLER_DIALOGMELDING WHERE personident = ? and partner_id = ?
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
        SELECT * FROM BEHANDLER_DIALOGMELDING WHERE hpr_id = ? and partner_id = ?
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
        SELECT * FROM BEHANDLER_DIALOGMELDING WHERE her_id = ? and partner_id = ?
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
        personident = getString("personident"),
        fornavn = getString("fornavn"),
        mellomnavn = getString("mellomnavn"),
        etternavn = getString("etternavn"),
        partnerId = getString("partner_id"),
        herId = getString("her_id"),
        parentHerId = getString("parent_her_id"),
        hprId = getString("hpr_id"),
        kontor = getString("kontor"),
        adresse = getString("adresse"),
        postnummer = getString("postnummer"),
        poststed = getString("poststed"),
        orgnummer = getString("orgnummer"),
        telefon = getString("telefon"),
        createdAt = getTimestamp("created_at").toLocalDateTime(),
        updatedAt = getTimestamp("updated_at").toLocalDateTime(),
    )
