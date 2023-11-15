package no.nav.aap.behandlingsflyt.faktagrunnlag.yrkesskade

import no.nav.aap.behandlingsflyt.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection

class YrkesskadeRepository(private val connection: DBConnection) {

    fun hentHvisEksisterer(behandlingId: BehandlingId): YrkesskadeGrunnlag? {
        return connection.queryFirstOrNull("SELECT ID FROM YRKESSKADE_GRUNNLAG WHERE AKTIV AND BEHANDLING_ID = ?") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper { row ->
                val id = row.getLong("ID")
                YrkesskadeGrunnlag(
                    id = id,
                    behandlingId = behandlingId,
                    yrkesskader = Yrkesskader(hentYrkesskader(id))
                )
            }
        }
    }

    private fun hentYrkesskader(id: Long): List<Yrkesskade> {
        return connection.queryList("SELECT REFERANSE, PERIODE FROM YRKESSKADE_PERIODER WHERE GRUNNLAG_ID = ?") {
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

        val id = connection.executeReturnKey("INSERT INTO YRKESSKADE_GRUNNLAG (BEHANDLING_ID) VALUES (?)") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
        }

        if (yrkesskader == null) {
            return
        }

        yrkesskader.yrkesskader.forEach { yrkesskade ->
            connection.execute("INSERT INTO YRKESSKADE_PERIODER (GRUNNLAG_ID, REFERANSE, PERIODE) VALUES (?, ?, ?::daterange)") {
                setParams {
                    setLong(1, id)
                    setString(2, yrkesskade.ref)
                    setPeriode(3, yrkesskade.periode)
                }
            }
        }
    }

    private fun deaktiverEksisterende(behandlingId: BehandlingId) {
        connection.execute("UPDATE YRKESSKADE_GRUNNLAG SET AKTIV = 'FALSE' WHERE AKTIV AND BEHANDLING_ID = ?") {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setResultValidator { rowsUpdated ->
                require(rowsUpdated == 1)
            }
        }
    }

    fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        val fraId =
            connection.queryFirstOrNull("SELECT ID FROM YRKESSKADE_GRUNNLAG WHERE AKTIV AND BEHANDLING_ID = ?") {
                setParams {
                    setLong(1, fraBehandling.toLong())
                }
                setRowMapper { row ->
                    row.getLong("ID")
                }
            }

        if (fraId == null) {
            return
        }

        val tilId = connection.executeReturnKey("INSERT INTO YRKESSKADE_GRUNNLAG (BEHANDLING_ID) VALUES (?)") {
            setParams {
                setLong(1, tilBehandling.toLong())
            }
        }

        connection.execute("INSERT INTO YRKESSKADE_PERIODER (GRUNNLAG_ID, REFERANSE, PERIODE) SELECT ?, REFERANSE, PERIODE FROM YRKESSKADE_PERIODER WHERE GRUNNLAG_ID = ?") {
            setParams {
                setLong(1, tilId)
                setLong(2, fraId)
            }
        }
    }
}
