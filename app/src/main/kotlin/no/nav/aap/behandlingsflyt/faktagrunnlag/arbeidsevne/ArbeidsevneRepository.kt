package no.nav.aap.behandlingsflyt.faktagrunnlag.arbeidsevne

import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.verdityper.Prosent

class ArbeidsevneRepository (private val connection: DBConnection) {

    fun hentHvisEksisterer(behandlingId: BehandlingId): ArbeidsevneGrunnlag? {
        return connection.queryFirstOrNull(
            """
            SELECT g.ID, a.BEGRUNNELSE, a.ANDEL_AV_NEDSETTELSE
            FROM ARBEIDSEVNE_GRUNNLAG g
            INNER JOIN ARBEIDSEVNE a ON g.ARBEIDSEVNE_ID = a.ID
            WHERE g.AKTIV AND g.BEHANDLING_ID = ?
            """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper { row ->
                ArbeidsevneGrunnlag(
                    id = row.getLong("ID"),
                    behandlingId = behandlingId,
                    vurdering = Arbeidsevne(
                        begrunnelse = row.getString("BEGRUNNELSE"),
                        andelNedsattArbeidsevne = Prosent(row.getInt("ANDEL_AV_NEDSETTELSE"))
                    )
                )
            }
        }
    }

    fun lagre(behandlingId: BehandlingId, arbeidsevne: Arbeidsevne) {
        val eksisterendeArbeidsevneGrunnlag = hentHvisEksisterer(behandlingId)

        if (eksisterendeArbeidsevneGrunnlag?.vurdering == arbeidsevne) return

        if (eksisterendeArbeidsevneGrunnlag != null) {
            deaktiverEksisterende(behandlingId)
        }

        val arbeidsevneId =
            connection.executeReturnKey("INSERT INTO ARBEIDSEVNE (BEGRUNNELSE, ANDEL_AV_NEDSETTELSE) VALUES (?, ?)") {
                setParams {
                    setString(1, arbeidsevne.begrunnelse)
                    setInt(2, arbeidsevne.andelNedsattArbeidsevne.prosentverdi())
                }
            }

        connection.execute("INSERT INTO ARBEIDSEVNE_GRUNNLAG (BEHANDLING_ID, ARBEIDSEVNE_ID) VALUES (?, ?)") {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, arbeidsevneId)
            }
        }
    }

    private fun deaktiverEksisterende(behandlingId: BehandlingId) {
        connection.execute("UPDATE ARBEIDSEVNE_GRUNNLAG SET AKTIV = FALSE WHERE AKTIV AND BEHANDLING_ID = ?") {
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
        connection.execute("INSERT INTO ARBEIDSEVNE_GRUNNLAG (BEHANDLING_ID, ARBEIDSEVNE_ID) SELECT ?, ARBEIDSEVNE_ID FROM ARBEIDSEVNE_GRUNNLAG WHERE AKTIV AND BEHANDLING_ID = ?") {
            setParams {
                setLong(1, tilBehandling.toLong())
                setLong(2, fraBehandling.toLong())
            }
        }
    }
}
