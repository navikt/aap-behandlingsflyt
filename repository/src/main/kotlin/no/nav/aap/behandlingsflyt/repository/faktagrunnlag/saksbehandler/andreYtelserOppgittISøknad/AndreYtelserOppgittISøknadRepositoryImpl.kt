package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.andreYtelserOppgittISøknad

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.AndreUtbetalingerYtelser
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.AndreYtelserSøknad
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.andreYtelserOppgittISøknad.AndreYtelserOppgittISøknadRepository

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

    override fun lagre(behandlingId: BehandlingId, andreUtbetalinger: AndreYtelserSøknad) {

        val eksistererFraFør = hentHvisEksisterer(behandlingId)
        if (eksistererFraFør != null) {
            deaktiverGrunnlag(behandlingId)
        }
        lagreYtelser(behandlingId, andreUtbetalinger, connection)
    }


    private fun lagreYtelser(
        behandlingId: BehandlingId,
        andreUtbetalinger: AndreYtelserSøknad,
        connection: DBConnection
    ) {

        val stønadstyper = requireNotNull(andreUtbetalinger.stønad)


        val insertSvarQuery = """
        INSERT INTO ANDRE_YTELSER_SVAR_I_SOKNAD (ekstraLonn , afpKilder)
        VALUES (?,?) RETURNING id
    """.trimIndent()

        val andreYtelserId = connection.executeReturnKey(insertSvarQuery) {
            setParams {
                setBoolean(1, andreUtbetalinger.ekstraLønn)
                setString(2, andreUtbetalinger.afpKilder)
            }
        }

        val insertYtelseQuery = """
        INSERT INTO ANNEN_YTELSE_OPPGITT_I_SOKNAD (ytelse, andre_ytelser_id)
        VALUES (?,?)
        RETURNING id
    """.trimIndent()

        for (ytelse in stønadstyper) {
            connection.executeReturnKey(insertYtelseQuery) {
                setParams {
                    setEnumName(1, ytelse)
                    setLong(2, andreYtelserId)
                }
            }
        }

        val insertGrunnlagQuery = """
        INSERT INTO ANDRE_YTELSER_OPPGITT_I_SOKNAD_GRUNNLAG (behandling_id, andre_ytelser_id)
        VALUES (?,?)
        """.trimIndent()

        connection.executeReturnKey(insertGrunnlagQuery) {
            setParams {
                setLong(1, behandlingId.id)
                setLong(2, andreYtelserId)
            }
        }

    }

    private fun hentYtelseIderPÅBehandlingId(behandlingId: BehandlingId): Long? {
        val query = """
                select andre_ytelser_id from ANDRE_YTELSER_OPPGITT_I_SOKNAD_GRUNNLAG where behandling_id = ? and aktiv = true
                """.trimIndent()
        return connection.queryFirstOrNull(query)
        {
            setParams { setLong(1, behandlingId.id) }
            setRowMapper { row ->
                row.getLong("andre_ytelser_id")
            }
        }

    }


    private fun hentAktivYtelserGrunnlagId(behandlingId: BehandlingId): Long? = connection.queryFirstOrNull(
        """
                SELECT id
                FROM ANDRE_YTELSER_OPPGITT_I_SOKNAD_GRUNNLAG
                WHERE behandling_id = ? and aktiv is true
                 
                """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("behandling_id")
        }
    }


    private fun hentAlleGrunnlagIdPåAndreYtelseId(andreYtelserId: Long): List<Long> {

        val query = """
            SELECT id FROM ANDRE_YTELSER_OPPGITT_I_SOKNAD_GRUNNLAG WHERE andre_ytelser_id = ?
        """.trimIndent()

        return connection.queryList(query) {
            setParams { setLong(1, andreYtelserId) }
            setRowMapper { row ->
                row.getLong("id")
            }
        }
    }

    override fun slett(behandlingId: BehandlingId) {

        //skal slette alle grunnlag på en behandling?
        val ytelserId = hentYtelseIderPÅBehandlingId(behandlingId)
        if (ytelserId == null) return

        // er det flere grunnlag på samme ytelse id ?

        val alleGrunnlagPåYtelseId = hentAlleGrunnlagIdPåAndreYtelseId(ytelserId)

        //Skal slette alle??! :*/

        val kunEtGrunnlagPåDetteSvaret = if (alleGrunnlagPåYtelseId.size == 1) true else false


        //sletter de på behandling id
        connection.execute(
            """
            delete from ANDRE_YTELSER_OPPGITT_I_SOKNAD_GRUNNLAG where behandling_id = ?;
        """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.id)
            }
        }

        //La de andre ytelsene bli vis de linker til andre grunnlag
        if (kunEtGrunnlagPåDetteSvaret) {
            connection.execute(
                """
            delete from ANDRE_YTELSER_OPPGITT_I_SOKNAD_GRUNNLAG where andre_ytelser_id = ?; 
        """.trimIndent()
            ) {
                setParams {
                    setLong(1, ytelserId)
                }
            }

            connection.execute(
                """
            delete from ANDRE_YTELSER_SVAR_I_SOKNAD where id = ?;
        """.trimIndent()
            ) {
                setParams {
                    setLong(1, ytelserId)
                }
            }
        }
        log.info("Slettet ${alleGrunnlagPåYtelseId.size} rader fra ANDRE_YTELSER_OPPGITT_I_SOKNAD_GRUNNLAG")

    }


    private fun deaktiverGrunnlag(behandlingId: BehandlingId) {
        connection.execute("UPDATE ANDRE_YTELSER_OPPGITT_I_SOKNAD_GRUNNLAG SET aktiv = FALSE WHERE aktiv AND behandling_id = ?") {
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

        val fraYtelserId = hentYtelseIderPÅBehandlingId(fraBehandling)
        if (fraYtelserId == null) return
        val insertGrunnlagQuery = """
        INSERT INTO ANDRE_YTELSER_OPPGITT_I_SOKNAD_GRUNNLAG (behandling_id, andre_ytelser_id)
        VALUES (?,?)
        """.trimIndent()

        connection.executeReturnKey(insertGrunnlagQuery) {
            setParams {
                setLong(1, tilBehandling.id)
                setLong(2, fraYtelserId)
            }
        }


    }


    override fun hentHvisEksisterer(behandlingId: BehandlingId): AndreYtelserSøknad? {
        val query = """
        SELECT 
            ytelser.ekstraLonn AS ekstraLonn,
            ytelser.afpKilder as afpKilder,
            array_agg(ytelse.ytelse) AS ytelsestyper
        FROM 
            ANDRE_YTELSER_OPPGITT_I_SOKNAD_GRUNNLAG grunnlag
        JOIN 
            ANDRE_YTELSER_SVAR_I_SOKNAD ytelser ON ytelser.id = grunnlag.andre_ytelser_id
        JOIN 
            ANNEN_YTELSE_OPPGITT_I_SOKNAD ytelse ON ytelser.id = ytelse.andre_ytelser_id
        WHERE 
            grunnlag.behandling_id = ?
        AND grunnlag.aktiv = TRUE
        GROUP BY 
            grunnlag.id, ytelser.ekstraLonn, ytelser.afpKilder;

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

    private fun mapGrunnlag(row: Row): AndreYtelserSøknad {
        val ekstraLønn = row.getBoolean("ekstraLonn")
        val afpKilder = row.getStringOrNull("afpKilder")
        val sqlArray = row.getArray("ytelsestyper", String::class)
        val ytelser = sqlArray.mapNotNull { AndreUtbetalingerYtelser.fromString(it) }

        return AndreYtelserSøknad(
            afpKilder = afpKilder,
            ekstraLønn = ekstraLønn,
            stønad = ytelser
        )
    }


    override fun hent(behandlingId: BehandlingId): AndreYtelserSøknad {
        return requireNotNull(hentHvisEksisterer(behandlingId))
    }

}
