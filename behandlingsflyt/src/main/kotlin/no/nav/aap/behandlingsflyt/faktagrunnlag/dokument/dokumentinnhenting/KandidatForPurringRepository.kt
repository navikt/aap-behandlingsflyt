package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.dokumentinnhenting

import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.repository.Repository
import no.nav.aap.komponenter.repository.RepositoryFactory
import java.time.LocalDate

interface KandidatForPurringRepository : Repository {
    fun finnKandidaterForPurring(dato: LocalDate = LocalDate.now()): List<BehandlingReferanse>
}

class KandidatForPurringRepositoryImpl(
    private val connection: DBConnection
) : KandidatForPurringRepository {
    override fun finnKandidaterForPurring(dato: LocalDate): List<BehandlingReferanse> {
        val query = """
            SELECT b.referanse
            FROM BEHANDLING b
             JOIN AVKLARINGSBEHOV a ON a.behandling_id = b.id
             JOIN (
                SELECT DISTINCT ON (AVKLARINGSBEHOV_ID) *
                FROM AVKLARINGSBEHOV_ENDRING
                ORDER BY AVKLARINGSBEHOV_ID, OPPRETTET_TID DESC
             ) ae ON ae.AVKLARINGSBEHOV_ID = a.id
            WHERE b.STATUS in ('${Status.UTREDES.name}', '${Status.OPPRETTET.name}')
            AND a.definisjon = '9003'
            AND ae.status = 'OPPRETTET'
            AND ae.frist >= ?
            AND ae.opprettet_tid::date = ?
        """.trimIndent()

        return connection.queryList(query) {
            setParams {
                setLocalDate(1, dato)
                setLocalDate(2, dato.minusWeeks(3).minusDays(1))
            }
            setRowMapper { row ->
                val behandlingReferanse = row.getUUID("referanse")
                BehandlingReferanse(behandlingReferanse)
            }
        }
    }

    companion object : RepositoryFactory<KandidatForPurringRepository> {
        override fun konstruer(connection: DBConnection) = KandidatForPurringRepositoryImpl(connection)
    }
}