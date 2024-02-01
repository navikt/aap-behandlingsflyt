package no.nav.aap.behandlingsflyt.faktagrunnlag.yrkesskade

import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.verdityper.Periode

class YrkesskadeRepository(private val connection: DBConnection) {

    fun hentHvisEksisterer(behandlingId: BehandlingId): YrkesskadeGrunnlag? {
        return connection.queryList(
            """
            SELECT y.ID AS YRKESSKADE_ID, p.REFERANSE, p.PERIODE
            FROM YRKESSKADE_GRUNNLAG g
            INNER JOIN YRKESSKADE y ON g.YRKESSKADE_ID = y.ID
            INNER JOIN YRKESSKADE_PERIODER p ON y.ID = p.YRKESSKADE_ID
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
                    periode = row.getPeriode("PERIODE")
                )
            }
        }
            .grupperOgMapTilGrunnlag(behandlingId)
            .firstOrNull()
    }

    private data class YrkesskadeInternal(
        val id: Long,
        val ref: String,
        val periode: Periode
    )

    private fun Iterable<YrkesskadeInternal>.grupperOgMapTilGrunnlag(behandlingId: BehandlingId): List<YrkesskadeGrunnlag> {
        return this
            .groupBy(YrkesskadeInternal::id) { yrkesskade ->
                Yrkesskade(
                    ref = yrkesskade.ref,
                    periode = yrkesskade.periode
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

    fun lagre(behandlingId: BehandlingId, yrkesskader: Yrkesskader?) {
        val yrkesskadeGrunnlag = hentHvisEksisterer(behandlingId)

        if (yrkesskadeGrunnlag?.yrkesskader == yrkesskader) return

        if (yrkesskadeGrunnlag != null) {
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
            connection.execute("INSERT INTO YRKESSKADE_PERIODER (YRKESSKADE_ID, REFERANSE, PERIODE) VALUES (?, ?, ?::daterange)") {
                setParams {
                    setLong(1, yrkesskadeId)
                    setString(2, yrkesskade.ref)
                    setPeriode(3, yrkesskade.periode)
                }
            }
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

    fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        require(fraBehandling != tilBehandling)
        connection.execute("INSERT INTO YRKESSKADE_GRUNNLAG (BEHANDLING_ID, YRKESSKADE_ID) SELECT ?, YRKESSKADE_ID FROM YRKESSKADE_GRUNNLAG WHERE AKTIV AND BEHANDLING_ID = ?") {
            setParams {
                setLong(1, tilBehandling.toLong())
                setLong(2, fraBehandling.toLong())
            }
        }
    }
}
