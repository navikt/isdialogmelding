package no.nav.syfo.behandler.database

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.application.database.toList
import no.nav.syfo.behandler.database.domain.PBehandlerDialogmelding
import no.nav.syfo.behandler.domain.Behandler
import no.nav.syfo.domain.PersonIdentNumber
import java.sql.*
import java.time.Instant
import java.util.*

const val queryCreateBehandlerDialogmeldingArbeidstaker =
    """
        INSERT INTO BEHANDLER_DIALOGMELDING_ARBEIDSTAKER (
            id,
            uuid,
            arbeidstaker_personident,
            created_at,
            behandler_dialogmelding_id
            ) VALUES (DEFAULT, ?, ?, ?, ?) RETURNING id
    """

fun Connection.createBehandlerDialogmeldingArbeidstaker(
    personIdentNumber: PersonIdentNumber,
    behandlerDialogmeldingId: Int,
) {
    val uuid = UUID.randomUUID()
    val idList = this.prepareStatement(queryCreateBehandlerDialogmeldingArbeidstaker).use {
        it.setString(1, uuid.toString())
        it.setString(2, personIdentNumber.value)
        it.setTimestamp(3, Timestamp.from(Instant.now()))
        it.setInt(4, behandlerDialogmeldingId)
        it.executeQuery().toList { getInt("id") }
    }

    if (idList.size != 1) {
        throw SQLException("Creating BehandlerDialogmeldingArbeidstaker failed, no rows affected.")
    }
}

fun Connection.createBehandlerDialogmelding(
    behandler: Behandler,
): PBehandlerDialogmelding {
    val now = Timestamp.from(Instant.now())
    val behandlerDialogmeldingList = this.prepareStatement(queryCreateBehandlerDialogmelding).use {
        it.setString(1, behandler.behandlerRef.toString())
        it.setString(2, behandler.type.name)
        it.setString(3, behandler.personident?.value)
        it.setString(4, behandler.fornavn)
        it.setString(5, behandler.mellomnavn)
        it.setString(6, behandler.etternavn)
        it.setString(7, behandler.partnerId.toString())
        it.setString(8, behandler.herId.toString())
        it.setString(9, behandler.parentHerId.toString())
        it.setString(10, behandler.hprId.toString())
        it.setString(11, behandler.kontor)
        it.setString(12, behandler.adresse)
        it.setString(13, behandler.postnummer)
        it.setString(14, behandler.poststed)
        it.setString(15, behandler.orgnummer?.value)
        it.setString(16, behandler.telefon)
        it.setTimestamp(17, now)
        it.setTimestamp(18, now)
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
            type,
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
            ) VALUES (DEFAULT, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) 
            RETURNING
            id,
            behandler_ref,
            type,
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

const val queryGetBehandlerDialogmeldingForPartnerId =
    """
        SELECT * FROM BEHANDLER_DIALOGMELDING WHERE partner_id = ?
    """

fun DatabaseInterface.getBehandlerDialogmelding(partnerId: Int): PBehandlerDialogmelding? {
    return this.connection.use { connection ->
        connection.prepareStatement(queryGetBehandlerDialogmeldingForPartnerId)
            .use {
                it.setString(1, partnerId.toString())
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
        type = getString("type"),
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
