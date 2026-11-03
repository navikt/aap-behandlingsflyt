package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.arena

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.arena.ArenaMaksdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.arena.ArenaMaksdatoGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.arena.ArenaMaksdatoRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.lookup.repository.Factory
import java.time.Instant

class ArenaMaksdatoRepositoryImpl(private val connection: DBConnection) : ArenaMaksdatoRepository {

    companion object : Factory<ArenaMaksdatoRepository> {
        override fun konstruer(connection: DBConnection): ArenaMaksdatoRepository {
            return ArenaMaksdatoRepositoryImpl(connection)
        }
    }

    override fun lagre(behandlingId: BehandlingId, arenaMaksdato: ArenaMaksdato?) {
        val nyttGrunnlag = ArenaMaksdatoGrunnlag(arenaMaksdato)
        if (hentHvisEksisterer(behandlingId) == nyttGrunnlag) return

        connection.execute("UPDATE arena_maksdato_grunnlag SET aktiv = FALSE WHERE aktiv AND behandling_id = ?") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
        }

        connection.execute(
            """
                INSERT INTO arena_maksdato_grunnlag
                    (behandling_id, arena_saksnummer, status, maksdato, vedtak_til, vedtak_id, opprettet_tid)
                VALUES (?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
                setString(2, arenaMaksdato?.arenaSaksnummer)
                setString(3, arenaMaksdato?.status)
                setLocalDate(4, arenaMaksdato?.maksdato)
                setLocalDate(5, arenaMaksdato?.vedtakTil)
                setLong(6, arenaMaksdato?.vedtakId?.toLong())
                setInstant(7, Instant.now())
            }
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): ArenaMaksdatoGrunnlag? = connection.queryFirstOrNull(
        "SELECT * FROM arena_maksdato_grunnlag WHERE behandling_id = ? AND aktiv = TRUE"
    ) {
        setParams {
            setLong(1, behandlingId.toLong())
        }
        setRowMapper(::mapGrunnlag)
    }

    private fun mapGrunnlag(row: Row): ArenaMaksdatoGrunnlag {
        val arenaSaksnummer = row.getStringOrNull("arena_saksnummer")
        val arenaMaksdato = if (arenaSaksnummer == null) {
            null
        } else {
            ArenaMaksdato(
                arenaSaksnummer = arenaSaksnummer,
                status = row.getString("status"),
                maksdato = row.getLocalDateOrNull("maksdato"),
                vedtakTil = row.getLocalDateOrNull("vedtak_til"),
                vedtakId = row.getInt("vedtak_id"),
            )
        }
        return ArenaMaksdatoGrunnlag(arenaMaksdato)
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        require(fraBehandling != tilBehandling)
        if (hentHvisEksisterer(fraBehandling) == null) return

        connection.execute(
            """
                INSERT INTO arena_maksdato_grunnlag
                    (behandling_id, arena_saksnummer, status, maksdato, vedtak_til, vedtak_id, opprettet_tid)
                SELECT ?, arena_saksnummer, status, maksdato, vedtak_til, vedtak_id, ?
                FROM arena_maksdato_grunnlag
                WHERE aktiv AND behandling_id = ?
            """.trimIndent()
        ) {
            setParams {
                setLong(1, tilBehandling.toLong())
                setInstant(2, Instant.now())
                setLong(3, fraBehandling.toLong())
            }
        }
    }

    override fun slett(behandlingId: BehandlingId) {
        connection.execute("DELETE FROM arena_maksdato_grunnlag WHERE behandling_id = ?") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
        }
    }
}
