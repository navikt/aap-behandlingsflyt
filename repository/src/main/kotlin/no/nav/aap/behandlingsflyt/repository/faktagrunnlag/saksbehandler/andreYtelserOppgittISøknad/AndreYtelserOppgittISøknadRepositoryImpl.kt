package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.andreYtelserOppgittISøknad

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.andreYtelserOppgittISøknad.AndreYtelserOppgittISøknadRepository
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.AndreUtbetalinger
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.AndreUtbetalingerYtelser
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.lookup.repository.Factory
import org.slf4j.LoggerFactory

class AndreYtelserOppgittISøknadRepositoryImpl(private val connection: DBConnection) :
    AndreYtelserOppgittISøknadRepository {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object : Factory<AndreYtelserOppgittISøknadRepositoryImpl> {
        override fun konstruer(connection: DBConnection): AndreYtelserOppgittISøknadRepositoryImpl {
            return AndreYtelserOppgittISøknadRepositoryImpl(connection)
        }
    }

    override fun lagre(behandlingId: BehandlingId, andreUtbetalinger: AndreUtbetalinger) {

        val eksistererFraFør = hentHvisEksisterer(behandlingId)
        if (eksistererFraFør != null) {
            deaktiverGrunnlag(behandlingId)
        }
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
    INSERT INTO ANDRE_YTELSER_OPPGITT_I_SØKNAD_GRUNNLAG (behandling_id, lonn ) 
    VALUES (?, ?) RETURNING id
""".trimIndent()

        val insertYtelserQuery = """
    INSERT INTO ANDRE_YTELSER_OPPGITT_I_SØKNAD (ytelse_grunnlag, ytelse_type) 
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

    private fun hentAlleGrunnlagIderPÅBehandlingId(behandlingId: BehandlingId) : List<Long> {

        val query = """
                    SELECT id
                    FROM ANDRE_YTELSER_OPPGITT_I_SØKNAD_GRUNNLAG
                    WHERE behandling_id = ? 
                 
                """.trimIndent()

        return connection.queryList(query)
        {
            setParams { setLong(1, behandlingId.id) }
            setRowMapper { row ->
                row.getLong("id")
            }
        }

    }


    private fun hentAktivYtelserGrunnlagId(behandlingId: BehandlingId) : Long? = connection.queryFirstOrNull(
    """
                    SELECT id
                    FROM ANDRE_YTELSER_OPPGITT_I_SØKNAD_GRUNNLAG
                    WHERE behandling_id = ? and aktiv is true
                 
                """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("behandling_id")
        }
    }

    override fun slett(behandlingId: BehandlingId) {

        val ytelserGrunnlagId = hentAlleGrunnlagIderPÅBehandlingId(behandlingId)
        if (ytelserGrunnlagId.isNullOrEmpty()) {
            log.warn("Prøvde å slette fra ANDRE_YTELSER_OPPGITT_I_SØKNAD_GRUNNLAG med behandling_id $behandlingId men ingen funnet")
        }


        connection.executeBatch("""
            delete from ANDRE_YTELSER_OPPGITT_I_SØKNAD where ytelse_grunnlag = ?;
        """.trimIndent(), ytelserGrunnlagId) {
            setParams {
                setLong(1, it)
            }
        }

        connection.executeBatch("""
            delete from ANDRE_YTELSER_OPPGITT_I_SØKNAD_GRUNNLAG where id = ?; 
        """.trimIndent(), ytelserGrunnlagId) {
            setParams {
                setLong(1, it)
            }
        }



        log.info("Slettet rader fra ANDRE_YTELSER_OPPGITT_I_SØKNAD_GRUNNLAG")

    }


    private fun deaktiverGrunnlag(behandlingId: BehandlingId) {
            connection.execute("UPDATE ANDRE_YTELSER_OPPGITT_I_SØKNAD_GRUNNLAG SET aktive = FALSE WHERE aktive AND behandling_id = ?") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setResultValidator { rowsUpdated ->
                require(rowsUpdated == 1)
            }
        }

    }


    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        require(fraBehandling != tilBehandling)
        require(hentHvisEksisterer(fraBehandling) != null)

        connection.execute(
            """
        WITH inserted_grunnlag AS (
            INSERT INTO ANDRE_YTELSER_OPPGITT_I_SØKNAD_GRUNNLAG (
                behandling_id,
                lonn,
                aktiv,
                opprettet_tid
            )
            SELECT
                ?,
                lonn,
                aktiv,
                opprettet_tid
            FROM ANDRE_YTELSER_OPPGITT_I_SØKNAD_GRUNNLAG
            WHERE aktiv = true AND behandling_id = ?
            RETURNING id, behandling_id
        )
        INSERT INTO ANDRE_YTELSER_OPPGITT_I_SØKNAD (
            ytelse_grunnlag,
            ytelse_type
        )
        SELECT
            inserted_grunnlag.id,
            ay.ytelse_type
        FROM ANDRE_YTELSER_OPPGITT_I_SØKNAD ay
        JOIN ANDRE_YTELSER_OPPGITT_I_SØKNAD_GRUNNLAG ayg
            ON ay.ytelse_grunnlag = ayg.id
        JOIN inserted_grunnlag
            ON ayg.behandling_id = ?
        WHERE ayg.behandling_id = ?
        """.trimIndent()
        ) {
            setParams {
                setLong(1, tilBehandling.toLong())
                setLong(2, fraBehandling.toLong())
                setLong(3, fraBehandling.toLong())
                setLong(4, fraBehandling.toLong())
            }
        }
    }


    override fun hentHvisEksisterer(behandlingId: BehandlingId): AndreUtbetalinger? {
        val query = """
        SELECT 
            grunnlag.lonn AS lonn,
            array_agg(ytg.ytelse_type) AS ytelsestyper
        FROM 
            ANDRE_YTELSER_OPPGITT_I_SØKNAD_GRUNNLAG grunnlag
        LEFT JOIN 
            ANDRE_YTELSER_OPPGITT_I_SØKNAD ytg ON grunnlag.id = ytg.ytelse_grunnlag
        WHERE 
            grunnlag.behandling_id = ? and grunnlag.aktiv = true
        GROUP BY grunnlag.id, grunnlag.lonn;
    """.trimIndent()

        return connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper { row ->
                if (row == null)  null
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
