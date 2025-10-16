package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.andreYtelser

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.andreYtelser.AndreYtelserRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.OppgittStudent
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentVurdering
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.AndreUtbetalinger
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.AndreUtbetalingerYtelser
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.lookup.repository.Factory
import org.slf4j.LoggerFactory

class AndreYtelserRepositoryImpl(private val connection: DBConnection) : AndreYtelserRepository {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object : Factory<AndreYtelserRepositoryImpl> {
        override fun konstruer(connection: DBConnection): AndreYtelserRepositoryImpl {
            return AndreYtelserRepositoryImpl(connection)
        }
    }

    override fun lagre(behandlingId: BehandlingId, andreUtbetalinger: AndreUtbetalinger) {
        lagreYtelser(behandlingId, andreUtbetalinger, connection)
    }


    private fun lagreYtelser(
        behandlingId: BehandlingId,
        andreUtbetalinger: AndreUtbetalinger,
        connection: DBConnection
    ) {
        val insertYtelseTypeQuery = """
    INSERT INTO YTELSE (type) VALUES (?) ON CONFLICT DO NOTHING
""".trimIndent()

        val insertGrunnlagQuery = """
    INSERT INTO ANDRE_YTELSER_GRUNNLAG (behandling_id, lonn) 
    VALUES (?, ?) RETURNING id
""".trimIndent()

        val insertYtelserQuery = """
    INSERT INTO YTELSER (ytelse_grunnlag, ytelse_type) 
    VALUES (?, ?) ON CONFLICT DO NOTHING
""".trimIndent()

        val stønadstyper = requireNotNull(andreUtbetalinger.stønad)

        connection.executeBatch(insertYtelseTypeQuery,stønadstyper){
            setParams {
                setString(1, it.toString())
            }
        }

        val grunnlagId = connection.executeReturnKey(insertGrunnlagQuery) {
            setParams {
                setLong(1, behandlingId.id)
                setBoolean(2, andreUtbetalinger.lønn)
            }
        }

        connection.executeBatch(insertYtelserQuery,stønadstyper){
            setParams {
                setLong(1, grunnlagId)
                setString(2, it.toString())
            }
        }
    }


    override fun slett(behandlingId: BehandlingId) {

        //TODO
    }


    private fun deaktiverGrunnlag(behandlingId: BehandlingId) {
        //TODO
    }


    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        val hentHvisEksisterer = hentHvisEksisterer(fraBehandling)
        //TODO
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): AndreUtbetalinger? {
        val query = """
        SELECT 
            grunnlag.lonn AS lonn,
            array_agg(ytg.ytelse_type) AS ytelsestyper
        FROM 
            ANDRE_YTELSER_GRUNNLAG grunnlag
        LEFT JOIN 
            YTELSER ytg ON grunnlag.id = ytg.ytelse_grunnlag
        WHERE 
            grunnlag.behandling_id = ?
        GROUP BY grunnlag.id, grunnlag.lonn;
    """.trimIndent()

        return connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper { row ->
                mapGrunnlag(row)
            }
        }
    }

    private fun mapGrunnlag(row: Row): AndreUtbetalinger {
        val lønn = row.getBoolean("lonn")
        val sqlArray = row.getArray("ytelsestyper", String::class)
        val ytelser = sqlArray.map { AndreUtbetalingerYtelser.fromDb(it) }

        return AndreUtbetalinger(
            lønn = lønn,
            stønad = ytelser
        )
    }


    override fun hent(behandlingId: BehandlingId): AndreUtbetalinger {
        return requireNotNull(hentHvisEksisterer(behandlingId))
    }

}
