package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.arena

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.arena.ArenaMaksdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.arena.ArenaMaksdatoGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.arena.ArenaMaksdatoRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.arena.ArenaVedtakStatus
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
        val eksisterende = hentHvisEksisterer(behandlingId)
        if (eksisterende != null && eksisterende.arenaMaksdato == arenaMaksdato) return

        connection.execute("UPDATE arena_maksdato_grunnlag SET aktiv = FALSE WHERE aktiv AND behandling_id = ?") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
        }

        val arenaMaksdatoId = arenaMaksdato?.let { lagreArenaMaksdato(it) }

        connection.execute(
            """
                INSERT INTO arena_maksdato_grunnlag (behandling_id, arena_maksdato_id, opprettet_tid)
                VALUES (?, ?, ?)
            """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, arenaMaksdatoId)
                setInstant(3, Instant.now())
            }
        }
    }

    private fun lagreArenaMaksdato(arenaMaksdato: ArenaMaksdato): Long = connection.executeReturnKey(
        """
            INSERT INTO arena_maksdato (arena_saksnummer, vedtak_status, maksdato, vedtak_til, vedtak_id,
                                        har_forlengelse_etter_11_12, opprettet_tid)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()
    ) {
        setParams {
            setString(1, arenaMaksdato.arenaSaksnummer)
            setEnumName(2, arenaMaksdato.vedtakStatus)
            setLocalDate(3, arenaMaksdato.maksdato)
            setLocalDate(4, arenaMaksdato.vedtakTil)
            setLong(5, arenaMaksdato.vedtakId.toLong())
            setBoolean(6, arenaMaksdato.harForlengelseEtter11_12)
            setInstant(7, Instant.now())
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): ArenaMaksdatoGrunnlag? = connection.queryFirstOrNull(
        """
            SELECT g.arena_maksdato_id, m.arena_saksnummer, m.vedtak_status, m.maksdato, m.vedtak_til, m.vedtak_id,
                   m.har_forlengelse_etter_11_12
            FROM arena_maksdato_grunnlag g
                     LEFT JOIN arena_maksdato m ON m.id = g.arena_maksdato_id
            WHERE g.behandling_id = ?
              AND g.aktiv
        """.trimIndent()
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
                vedtakStatus = row.getEnumOrNull<ArenaVedtakStatus>("vedtak_status"),
                maksdato = row.getLocalDateOrNull("maksdato"),
                vedtakTil = row.getLocalDateOrNull("vedtak_til"),
                vedtakId = row.getInt("vedtak_id"),
                harForlengelseEtter11_12 = row.getBooleanOrNull("har_forlengelse_etter_11_12"),
            )
        }
        return ArenaMaksdatoGrunnlag(arenaMaksdato, row.getLongOrNull("arena_maksdato_id"))
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        require(fraBehandling != tilBehandling)

        connection.execute(
            """
                INSERT INTO arena_maksdato_grunnlag (behandling_id, arena_maksdato_id, opprettet_tid)
                SELECT ?, arena_maksdato_id, ?
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
        val arenaMaksdatoIder = connection.queryList(
            """
                SELECT arena_maksdato_id
                FROM arena_maksdato_grunnlag
                WHERE behandling_id = ? AND arena_maksdato_id IS NOT NULL
            """.trimIndent()
        ) {
            setParams { setLong(1, behandlingId.toLong()) }
            setRowMapper { it.getLong("arena_maksdato_id") }
        }

        connection.execute("DELETE FROM arena_maksdato_grunnlag WHERE behandling_id = ?") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
        }

        // Radene kan deles med andre behandlinger via kopier, og stønadsperiodevurderinger kan peke på dem.
        // Slett bare de som ingen lenger peker på.
        connection.execute(
            """
                DELETE FROM arena_maksdato m
                WHERE m.id = ANY(?::bigint[])
                  AND NOT EXISTS (SELECT 1 FROM arena_maksdato_grunnlag g WHERE g.arena_maksdato_id = m.id)
                  AND NOT EXISTS (
                      SELECT 1 FROM stonadsperiode_vurdering v WHERE v.kant_i_kant_arena_maksdato_id = m.id
                  )
            """.trimIndent()
        ) {
            setParams {
                setLongArray(1, arenaMaksdatoIder.distinct())
            }
        }
    }
}
