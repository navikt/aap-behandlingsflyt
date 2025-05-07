package no.nav.aap.behandlingsflyt.repository.faktagrunnlag.register.yrkesskade

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.Yrkesskade
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.YrkesskadeGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.YrkesskadeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.yrkesskade.Yrkesskader
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.Factory
import java.time.LocalDate

class YrkesskadeRepositoryImpl(private val connection: DBConnection) : YrkesskadeRepository {
    companion object : Factory<YrkesskadeRepositoryImpl> {
        override fun konstruer(connection: DBConnection): YrkesskadeRepositoryImpl {
            return YrkesskadeRepositoryImpl(connection)
        }
    }

    private fun eksistererGrunnlag(behandlingId: BehandlingId): Boolean {
        return connection.queryFirstOrNull(
            """
            SELECT id FROM YRKESSKADE_GRUNNLAG g WHERE g.AKTIV AND g.BEHANDLING_ID = ?
        """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper {
                it.getLong("id")
            }
        } != null
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): YrkesskadeGrunnlag? {
        return connection.queryList(
            """
            SELECT y.ID AS YRKESSKADE_ID, p.REFERANSE, p.SKADEDATO
            FROM YRKESSKADE_GRUNNLAG g
            INNER JOIN YRKESSKADE y ON g.YRKESSKADE_ID = y.ID
            INNER JOIN YRKESSKADE_DATO p ON y.ID = p.YRKESSKADE_ID
            WHERE g.AKTIV AND g.BEHANDLING_ID = ?
            """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper { row ->
                YrkesskadeInternal(
                    id = row.getLong("YRKESSKADE_ID"),
                    ref = row.getString("REFERANSE"),
                    skadedato = row.getLocalDate("SKADEDATO")
                )
            }
        }.grupperOgMapTilGrunnlag(behandlingId)
            .firstOrNull()
    }

    private data class YrkesskadeInternal(
        val id: Long,
        val ref: String,
        val skadedato: LocalDate
    )

    private fun Iterable<YrkesskadeInternal>.grupperOgMapTilGrunnlag(behandlingId: BehandlingId): List<YrkesskadeGrunnlag> {
        return this
            .groupBy(YrkesskadeInternal::id) { yrkesskade ->
                Yrkesskade(
                    ref = yrkesskade.ref,
                    skadedato = yrkesskade.skadedato
                )
            }
            .map { (yrkesskadeId, yrkesskader) ->
                YrkesskadeGrunnlag(
                    id = yrkesskadeId,
                    behandlingId = behandlingId,
                    yrkesskader = Yrkesskader(yrkesskader)
                )
            }
    }

    override fun lagre(behandlingId: BehandlingId, yrkesskader: Yrkesskader?) {
        val yrkesskadeGrunnlag = hentHvisEksisterer(behandlingId)

        if (yrkesskadeGrunnlag?.yrkesskader == yrkesskader) return

        if (eksistererGrunnlag(behandlingId)) {
            deaktiverEksisterende(behandlingId)
        }

        val yrkesskadeId = connection.executeReturnKey("INSERT INTO YRKESSKADE DEFAULT VALUES")

        connection.execute("INSERT INTO YRKESSKADE_GRUNNLAG (BEHANDLING_ID, YRKESSKADE_ID) VALUES (?, ?)") {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, yrkesskadeId)
            }
        }

        if (yrkesskader == null) {
            return
        }

        yrkesskader.yrkesskader.forEach { yrkesskade ->
            connection.execute("INSERT INTO YRKESSKADE_DATO (YRKESSKADE_ID, REFERANSE, SKADEDATO) VALUES (?, ?, ?)") {
                setParams {
                    setLong(1, yrkesskadeId)
                    setString(2, yrkesskade.ref)
                    setLocalDate(3, yrkesskade.skadedato)
                }
            }
        }
    }

    override fun slett(behandlingId: BehandlingId) {

        val yrkesskadeIds = getYrkesskadeIds(behandlingId)

        connection.execute("""
            delete from yrkesskade_dato where id = ANY(?::bigint[]);
            delete from yrkesskade where bistand_vurderinger_id = ANY(?::bigint[]);
            delete from yrkesskade_grunnlag where behandling_id = ? 
        """.trimIndent()) {
            setParams {
                setLongArray(1, yrkesskadeIds)
                setLongArray(2, yrkesskadeIds)
                setLong(3, behandlingId.id)
            }
        }
    }

    private fun getYrkesskadeIds(behandlingId: BehandlingId): List<Long> = connection.queryList(
        """
                    SELECT yrkesskade_id
                    FROM yrkesskade_grunnlag
                    WHERE behandling_id = ?
                 
                """.trimIndent()
    ) {
        setParams { setLong(1, behandlingId.id) }
        setRowMapper { row ->
            row.getLong("yrkesskade_id")
        }
    }

    private fun deaktiverEksisterende(behandlingId: BehandlingId) {
        connection.execute("UPDATE YRKESSKADE_GRUNNLAG SET AKTIV = FALSE WHERE AKTIV AND BEHANDLING_ID = ?") {
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
        connection.execute("INSERT INTO YRKESSKADE_GRUNNLAG (BEHANDLING_ID, YRKESSKADE_ID) SELECT ?, YRKESSKADE_ID FROM YRKESSKADE_GRUNNLAG WHERE AKTIV AND BEHANDLING_ID = ?") {
            setParams {
                setLong(1, tilBehandling.toLong())
                setLong(2, fraBehandling.toLong())
            }
        }
    }
}
