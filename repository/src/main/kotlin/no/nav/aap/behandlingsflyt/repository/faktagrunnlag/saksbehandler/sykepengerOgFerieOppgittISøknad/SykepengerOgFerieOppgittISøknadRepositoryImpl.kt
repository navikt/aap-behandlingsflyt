package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.saksbehandler.sykepengerOgFerieOppgittISøknad

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykepengerOgFerieOppgittISøknad.SykepengerOgFerieOppgittISøknadRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykepengerOgFerieOppgittISøknad.SykepengerOgFerieSøknad
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.Factory
import org.slf4j.LoggerFactory

class SykepengerOgFerieOppgittISøknadRepositoryImpl(private val connection: DBConnection) :
    SykepengerOgFerieOppgittISøknadRepository {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object : Factory<SykepengerOgFerieOppgittISøknadRepositoryImpl> {
        override fun konstruer(connection: DBConnection): SykepengerOgFerieOppgittISøknadRepositoryImpl {
            return SykepengerOgFerieOppgittISøknadRepositoryImpl(connection)
        }
    }

    override fun lagre(behandlingId: BehandlingId, sykepengerOgFerie: SykepengerOgFerieSøknad) {
        if (hentHvisEksisterer(behandlingId) != null) {
            deaktiverGrunnlag(behandlingId)
        }
        lagreSykepengerOgFerie(behandlingId, sykepengerOgFerie)
    }

    private fun lagreSykepengerOgFerie(behandlingId: BehandlingId, sykepengerOgFerie: SykepengerOgFerieSøknad) {
        val insertSvarQuery = """
            INSERT INTO SYKEPENGER_OG_FERIE_SVAR_I_SØKNAD (mottar_sykepenger, ferie_dager)
            VALUES (?, ?) RETURNING id
        """.trimIndent()

        val svarId = connection.executeReturnKey(insertSvarQuery) {
            setParams {
                setBoolean(1, sykepengerOgFerie.mottarSykepenger)
                setInt(2, sykepengerOgFerie.ferieDager)
            }
        }

        val insertPeriodeQuery = """
            INSERT INTO SYKEPENGE_FERIEPERIODE_I_SØKNAD (sykepenger_ferie_id, fra_dato, til_dato)
            VALUES (?, ?, ?)
        """.trimIndent()

        for (periode in sykepengerOgFerie.feriePerioder) {
            connection.executeReturnKey(insertPeriodeQuery) {
                setParams {
                    setLong(1, svarId)
                    setLocalDate(2, periode.fom)
                    setLocalDate(3, periode.tom)
                }
            }
        }

        val insertGrunnlagQuery = """
            INSERT INTO SYKEPENGER_OG_FERIE_OPPGITT_I_SØKNAD_GRUNNLAG (behandling_id, sykepenger_ferie_id)
            VALUES (?, ?)
        """.trimIndent()

        connection.executeReturnKey(insertGrunnlagQuery) {
            setParams {
                setLong(1, behandlingId.id)
                setLong(2, svarId)
            }
        }
    }

    private fun hentSvarIdPåBehandlingId(behandlingId: BehandlingId): Long? {
        val query = """
            SELECT sykepenger_ferie_id FROM SYKEPENGER_OG_FERIE_OPPGITT_I_SØKNAD_GRUNNLAG
            WHERE behandling_id = ? AND aktiv = true
        """.trimIndent()

        return connection.queryFirstOrNull(query) {
            setParams { setLong(1, behandlingId.id) }
            setRowMapper { row -> row.getLong("sykepenger_ferie_id") }
        }
    }

    private fun hentAlleGrunnlagIdPåSvarId(svarId: Long): List<Long> {
        val query = """
            SELECT id FROM SYKEPENGER_OG_FERIE_OPPGITT_I_SØKNAD_GRUNNLAG WHERE sykepenger_ferie_id = ?
        """.trimIndent()

        return connection.queryList(query) {
            setParams { setLong(1, svarId) }
            setRowMapper { row -> row.getLong("id") }
        }
    }

    override fun slett(behandlingId: BehandlingId) {
        val svarId = hentSvarIdPåBehandlingId(behandlingId) ?: return
        val kunEttGrunnlagPåDetteSvaret = hentAlleGrunnlagIdPåSvarId(svarId).size == 1

        connection.execute("DELETE FROM SYKEPENGER_OG_FERIE_OPPGITT_I_SØKNAD_GRUNNLAG WHERE behandling_id = ?") {
            setParams { setLong(1, behandlingId.id) }
        }

        if (kunEttGrunnlagPåDetteSvaret) {
            connection.execute("DELETE FROM SYKEPENGE_FERIEPERIODE_I_SØKNAD WHERE sykepenger_ferie_id = ?") {
                setParams { setLong(1, svarId) }
            }
            connection.execute("DELETE FROM SYKEPENGER_OG_FERIE_SVAR_I_SØKNAD WHERE id = ?") {
                setParams { setLong(1, svarId) }
            }
        }

        log.info("Slettet sykepenger og ferie oppgitt i søknad for behandling $behandlingId")
    }

    private fun deaktiverGrunnlag(behandlingId: BehandlingId) {
        connection.execute("UPDATE SYKEPENGER_OG_FERIE_OPPGITT_I_SØKNAD_GRUNNLAG SET aktiv = FALSE WHERE aktiv AND behandling_id = ?") {
            setParams { setLong(1, behandlingId.toLong()) }
            setResultValidator { rowsUpdated -> require(rowsUpdated == 1) }
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        require(fraBehandling != tilBehandling)

        val svarId = hentSvarIdPåBehandlingId(fraBehandling) ?: return
        connection.executeReturnKey(
            """
            INSERT INTO SYKEPENGER_OG_FERIE_OPPGITT_I_SØKNAD_GRUNNLAG (behandling_id, sykepenger_ferie_id)
            VALUES (?, ?)
            """.trimIndent()
        ) {
            setParams {
                setLong(1, tilBehandling.id)
                setLong(2, svarId)
            }
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): SykepengerOgFerieSøknad? {
        val svarId = hentSvarIdPåBehandlingId(behandlingId) ?: return null

        val query = """
            SELECT mottar_sykepenger, ferie_dager FROM SYKEPENGER_OG_FERIE_SVAR_I_SØKNAD WHERE id = ?
        """.trimIndent()

        val svar: SykepengerOgFerieSøknad = connection.queryFirstOrNull(query) {
            setParams { setLong(1, svarId) }
            setRowMapper { row -> mapSvar(row) }
        } ?: return null

        return svar.copy(feriePerioder = hentFeriePerioder(svarId))
    }

    private fun hentFeriePerioder(svarId: Long): List<Periode> {
        val query = """
            SELECT fra_dato, til_dato FROM SYKEPENGE_FERIEPERIODE_I_SØKNAD WHERE sykepenger_ferie_id = ?
        """.trimIndent()

        return connection.queryList(query) {
            setParams { setLong(1, svarId) }
            setRowMapper { row -> Periode(row.getLocalDate("fra_dato"), row.getLocalDate("til_dato")) }
        }
    }

    private fun mapSvar(row: Row): SykepengerOgFerieSøknad {
        return SykepengerOgFerieSøknad(
            mottarSykepenger = row.getBoolean("mottar_sykepenger"),
            feriePerioder = emptyList(),
            ferieDager = row.getIntOrNull("ferie_dager"),
        )
    }
}
