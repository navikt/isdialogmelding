package no.nav.syfo.identhendelse.database

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.application.database.toList
import no.nav.syfo.domain.Personident
import java.sql.Connection
import java.sql.PreparedStatement

const val queryUpdateBehandler =
    """
        UPDATE BEHANDLER
        SET personident = ?
        WHERE personident = ?
    """

fun Connection.updateBehandler(
    nyPersonident: Personident,
    inactiveIdenter: List<Personident>,
    commit: Boolean = false,
): Int {
    return this.updateIdent(
        query = queryUpdateBehandler,
        nyPersonident = nyPersonident,
        inactiveIdenter = inactiveIdenter,
        commit = commit,
    )
}

const val queryBehandlerArbeidstaker =
    """
        UPDATE BEHANDLER_ARBEIDSTAKER
        SET arbeidstaker_personident = ?
        WHERE arbeidstaker_personident = ?
    """

fun Connection.updateBehandlerArbeidstaker(
    nyPersonident: Personident,
    inactiveIdenter: List<Personident>,
    commit: Boolean = false,
): Int {
    return this.updateIdent(
        query = queryBehandlerArbeidstaker,
        nyPersonident = nyPersonident,
        inactiveIdenter = inactiveIdenter,
        commit = commit,
    )
}

const val queryUpdateBehandlerDialogmeldingBestilling =
    """
        UPDATE BEHANDLER_DIALOGMELDING_BESTILLING
        SET arbeidstaker_personident = ?
        WHERE arbeidstaker_personident = ?
    """

fun Connection.updateBehandlerDialogmeldingBestilling(
    nyPersonident: Personident,
    inactiveIdenter: List<Personident>,
    commit: Boolean = false,
): Int {
    return this.updateIdent(
        query = queryUpdateBehandlerDialogmeldingBestilling,
        nyPersonident = nyPersonident,
        inactiveIdenter = inactiveIdenter,
        commit = commit,
    )
}

private fun Connection.updateIdent(
    query: String,
    nyPersonident: Personident,
    inactiveIdenter: List<Personident>,
    commit: Boolean = false,
): Int {
    var updatedRows = 0
    this.prepareStatement(query).use {
        inactiveIdenter.forEach { inactiveIdent ->
            it.setString(1, nyPersonident.value)
            it.setString(2, inactiveIdent.value)
            updatedRows += it.executeUpdate()
        }
    }
    if (commit) {
        this.commit()
    }
    return updatedRows
}

const val queryGetIdentCount =
    """
        SELECT COUNT(*)
        FROM (
            SELECT personident FROM BEHANDLER
            UNION ALL
            SELECT arbeidstaker_personident as personident FROM BEHANDLER_ARBEIDSTAKER
            UNION ALL
            SELECT arbeidstaker_personident as personident FROM BEHANDLER_DIALOGMELDING_BESTILLING
        ) identer
        WHERE personident = ?
    """

fun DatabaseInterface.getIdentCount(
    identer: List<Personident>,
): Int =
    this.connection.use { connection ->
        connection.prepareStatement(queryGetIdentCount).use<PreparedStatement, Int> {
            var count = 0
            identer.forEach { ident ->
                it.setString(1, ident.value)
                count += it.executeQuery().toList { getInt(1) }.firstOrNull() ?: 0
            }
            return count
        }
    }
