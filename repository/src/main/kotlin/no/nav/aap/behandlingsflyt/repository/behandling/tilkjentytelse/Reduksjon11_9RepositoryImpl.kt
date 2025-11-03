package no.nav.aap.behandlingsflyt.repository.behandling.tilkjentytelse

import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.Reduksjon11_9
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.Reduksjon11_9Repository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.lookup.repository.Factory
import org.slf4j.LoggerFactory

class Reduksjon11_9RepositoryImpl(private val connection: DBConnection) :
    Reduksjon11_9Repository {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object : Factory<Reduksjon11_9RepositoryImpl> {
        override fun konstruer(connection: DBConnection): Reduksjon11_9RepositoryImpl {
            return Reduksjon11_9RepositoryImpl(connection)
        }
    }

    override fun hent(behandlingId: BehandlingId): List<Reduksjon11_9> {
        val reduksjoner11_9 = connection.queryList(
            """
            SELECT * FROM reduksjon_11_9 WHERE reduksjon_11_9_grunnlag_id IN (SELECT ID FROM reduksjon_11_9_grunnlag WHERE BEHANDLING_ID=? AND AKTIV=TRUE)
        """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper {
                Reduksjon11_9(
                    dagsats = Beløp(it.getInt("DAGSATS")),
                    dato = it.getLocalDate("dato"),
                )
            }
        }

        return reduksjoner11_9
    }

    override fun lagre(
        behandlingId: BehandlingId,
        reduksjoner: List<Reduksjon11_9>
    ) {

        val eksisterendeReduksjoner = hent(behandlingId)
        if (eksisterendeReduksjoner.toSet() == reduksjoner.toSet()) {
            return
        }

        deaktiverEksisterende(behandlingId)

        val grunnlagId = connection.executeReturnKey(
            """
            insert into reduksjon_11_9_grunnlag (behandling_id, aktiv) values (?, true)
        """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
        }

        reduksjoner.forEach { reduksjon ->
            lagreReduksjon(grunnlagId, reduksjon)
        }
    }

    private fun lagreReduksjon(grunnlagId: Long, reduksjon: Reduksjon11_9) {
        connection.execute(
            """
            insert into reduksjon_11_9 (reduksjon_11_9_grunnlag_id, dato, dagsats )
            values (?, ?, ?)
        """.trimIndent()
        ) {
            setParams {
                setLong(1, grunnlagId)
                setLocalDate(2, reduksjon.dato)
                setBigDecimal(3, reduksjon.dagsats.verdi())
            }
        }
    }


    private fun deaktiverEksisterende(behandlingId: BehandlingId) {
        connection.execute("update reduksjon_11_9_grunnlag set aktiv = false where aktiv and behandling_id = ?") {
            setParams {
                setLong(1, behandlingId.id)
            }
        }
    }


    override fun kopier(
        fraBehandling: BehandlingId,
        tilBehandling: BehandlingId
    ) {
        val eksisterendeGrunnlag = hent(fraBehandling)
        if (eksisterendeGrunnlag.isEmpty()) {
            return
        }

        lagre(tilBehandling, eksisterendeGrunnlag)
    }

    override fun slett(behandlingId: BehandlingId) {
        val deletedRows = connection.executeReturnUpdated(
            """
            delete from reduksjon_11_9 where reduksjon_11_9_grunnlag_id in (select id from reduksjon_11_9_grunnlag where behandling_id = ?);
            delete from reduksjon_11_9_grunnlag where behandling_id = ? 
        """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.id)
                setLong(2, behandlingId.id)
            }
        }
        log.info("Slettet $deletedRows rader fra tilkjent_periode")
    }
}