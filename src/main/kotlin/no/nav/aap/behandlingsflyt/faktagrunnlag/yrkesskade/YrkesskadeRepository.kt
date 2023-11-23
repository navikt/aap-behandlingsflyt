package no.nav.aap.behandlingsflyt.faktagrunnlag.yrkesskade

import no.nav.aap.behandlingsflyt.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection

class YrkesskadeRepository(private val connection: DBConnection) {

    fun hentHvisEksisterer(behandlingId: BehandlingId): YrkesskadeGrunnlag? {
        return connection.queryFirstOrNull("""
            SELECT YRKESSKADE_ID
            FROM YRKESSKADE_GRUNNLAG
            WHERE AKTIV AND BEHANDLING_ID = ?
            """.trimIndent()) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper { row ->
                val id = row.getLong("YRKESSKADE_ID")
                YrkesskadeGrunnlag(
                    id = id,
                    behandlingId = behandlingId,
                    yrkesskader = Yrkesskader(hentYrkesskader(id))
                )
            }
        }
    }

    private fun hentYrkesskader(id: Long): List<Yrkesskade> {
        return connection.queryList("""
            SELECT p.REFERANSE AS REFERANSE, p.PERIODE AS PERIODE
            FROM YRKESSKADE y
            INNER JOIN YRKESSKADE_PERIODER p ON y.ID = p.YRKESSKADE_ID
            WHERE y.ID = ?
            """.trimIndent()) {
            setParams {
                setLong(1, id)
            }
            setRowMapper { row ->
                Yrkesskade(
                    ref = row.getString("REFERANSE"),
                    periode = row.getPeriode("PERIODE")
                )
            }
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
