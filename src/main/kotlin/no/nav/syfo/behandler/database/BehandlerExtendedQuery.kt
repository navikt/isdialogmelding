package no.nav.syfo.behandler.database

import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.application.database.toList
import no.nav.syfo.behandler.database.domain.PBehandler
import no.nav.syfo.behandler.database.domain.PBehandlerKontor
import no.nav.syfo.domain.Personident
import java.sql.ResultSet
import java.time.OffsetDateTime
import java.util.*

const val querySykmelderAndKontor =
    """
        SELECT b.id behandlerid, b.her_id behandlerherid, b.created_at behandlercreatedat, b.updated_at behandlerupdatedat, b.mottatt behandlermottatt, b.*, 
        k.id kontorid, k.her_id kontorherid, k.created_at kontorcreatedat, k.updated_at kontorupdatedat, k.mottatt kontormottatt, k.*
        FROM BEHANDLER AS b
        INNER JOIN BEHANDLER_ARBEIDSTAKER AS r ON r.behandler_id = b.id AND r.arbeidstaker_personident = ? 
        AND r.type = 'SYKMELDER'
        INNER JOIN BEHANDLER_KONTOR AS k ON k.id = b.kontor_id AND k.dialogmelding_enabled IS NOT NULL = ?
        ORDER BY r.mottatt DESC
    """

fun DatabaseInterface.getSykmeldereExtended(
    arbeidstakerIdent: Personident,
    dialogmeldingEnabledStatus: Boolean = true,
): List<Pair<PBehandler, PBehandlerKontor>> {
    return this.connection.use { connection ->
        connection.prepareStatement(querySykmelderAndKontor)
            .use {
                it.setString(1, arbeidstakerIdent.value)
                it.setBoolean(2, dialogmeldingEnabledStatus)
                it.executeQuery().toList {
                    val (pBehandler, pBehandlerKontor) = toPBehandlerAndPBehandlerKontor()
                    Pair(pBehandler, pBehandlerKontor)
                }
            }
    }
}

const val querySearchBehandler =
    """
SELECT b.id behandlerid, b.her_id behandlerherid, b.created_at behandlercreatedat, b.updated_at behandlerupdatedat, b.mottatt behandlermottatt, b.*, 
        k.id kontorid, k.her_id kontorherid, k.created_at kontorcreatedat, k.updated_at kontorupdatedat, k.mottatt kontormottatt, k.*
        FROM BEHANDLER AS b
        INNER JOIN BEHANDLER_KONTOR AS k ON (k.id = b.kontor_id)
        WHERE k.dialogmelding_enabled IS NOT NULL 
        AND b.invalidated IS NULL
        AND (b.fornavn ilike ? OR b.etternavn ilike ? 
            OR position(? IN k.navn)>0 OR position(UPPER(?) IN k.navn)>0 OR position(INITCAP(?) IN k.navn)>0 
            OR k.orgnummer = ?)
    """

fun DatabaseInterface.searchBehandler(
    searchStrings: List<String>,
): List<Pair<PBehandler, PBehandlerKontor>> {
    var results: List<Pair<PBehandler, PBehandlerKontor>> = emptyList()
    this.connection.use { connection ->
        connection.prepareStatement(querySearchBehandler).use {
            searchStrings.forEach { searchString ->
                it.setString(1, searchString + "%")
                it.setString(2, searchString + "%")
                it.setString(3, searchString)
                it.setString(4, searchString)
                it.setString(5, searchString)
                it.setString(6, searchString)
                val matches = it.executeQuery().toList { toPBehandlerAndPBehandlerKontor() }
                results = if (results.isEmpty()) matches else (results.intersect(matches).toList())
            }
        }
    }
    return results
}

fun ResultSet.toPBehandlerAndPBehandlerKontor(): Pair<PBehandler, PBehandlerKontor> {
    val pBehandler = PBehandler(
        id = getInt("behandlerid"),
        behandlerRef = UUID.fromString(getString("behandler_ref")),
        kategori = getString("kategori"),
        kontorId = getInt("kontor_id"),
        personident = getString("personident"),
        fornavn = getString("fornavn"),
        mellomnavn = getString("mellomnavn"),
        etternavn = getString("etternavn"),
        herId = getString("behandlerherid"),
        hprId = getString("hpr_id"),
        telefon = getString("telefon"),
        createdAt = getObject("behandlercreatedat", OffsetDateTime::class.java),
        updatedAt = getObject("behandlerupdatedat", OffsetDateTime::class.java),
        mottatt = getObject("behandlermottatt", OffsetDateTime::class.java),
    )

    val pBehandlerKontor = PBehandlerKontor(
        id = getInt("kontorid"),
        partnerId = getString("partner_id"),
        herId = getString("kontorherid"),
        navn = getString("navn"),
        adresse = getString("adresse"),
        postnummer = getString("postnummer"),
        poststed = getString("poststed"),
        orgnummer = getString("orgnummer"),
        dialogmeldingEnabled = getObject("dialogmelding_enabled")?.let {
            getObject("dialogmelding_enabled", OffsetDateTime::class.java)
        },
        dialogmeldingEnabledLocked = getBoolean("dialogmelding_enabled_locked"),
        system = getString("system"),
        createdAt = getObject("kontorcreatedat", OffsetDateTime::class.java),
        updatedAt = getObject("kontorupdatedat", OffsetDateTime::class.java),
        mottatt = getObject("kontormottatt", OffsetDateTime::class.java),
    )
    return Pair(pBehandler, pBehandlerKontor)
}
