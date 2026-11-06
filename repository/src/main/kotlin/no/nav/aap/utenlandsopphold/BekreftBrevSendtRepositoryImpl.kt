package no.nav.aap.utenlandsopphold

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.Factory

class BekreftBrevSendtRepositoryImpl(private val connection: DBConnection) : BekreftBrevSendtRepository {

    companion object : Factory<BekreftBrevSendtRepositoryImpl> {
        override fun konstruer(connection: DBConnection): BekreftBrevSendtRepositoryImpl {
            return BekreftBrevSendtRepositoryImpl(connection)
        }
    }

    override fun lagre(behandlingId: BehandlingId, bekreftelse: BekreftBrevSendtBekreftelse) {
        connection.execute("UPDATE BEKREFT_BREV_SENDT_GRUNNLAG SET AKTIV = FALSE WHERE BEHANDLING_ID = ? AND AKTIV") {
            setParams { setLong(1, behandlingId.toLong()) }
        }

        val bekreftelseId = connection.executeReturnKey(
            """
            INSERT INTO BEKREFT_BREV_SENDT_BEKREFTELSE (BREV_SENDT, BEKREFTET_AV, BEKREFTET_TIDSPUNKT)
            VALUES (?, ?, ?)
            """.trimIndent()
        ) {
            setParams {
                setBoolean(1, bekreftelse.brevSendt)
                setString(2, bekreftelse.bekreftetAv)
                setLocalDateTime(3, bekreftelse.bekreftetTidspunkt)
            }
        }

        connection.execute("INSERT INTO BEKREFT_BREV_SENDT_GRUNNLAG (BEHANDLING_ID, BEKREFTELSE_ID) VALUES (?, ?)") {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, bekreftelseId)
            }
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): BekreftBrevSendtGrunnlag? {
        return connection.queryFirstOrNull(
            """
            SELECT b.*
            FROM BEKREFT_BREV_SENDT_GRUNNLAG g
                     JOIN BEKREFT_BREV_SENDT_BEKREFTELSE b ON g.BEKREFTELSE_ID = b.ID
            WHERE g.BEHANDLING_ID = ? AND g.AKTIV
            """.trimIndent()
        ) {
            setParams { setLong(1, behandlingId.toLong()) }
            setRowMapper {
                BekreftBrevSendtGrunnlag(
                    BekreftBrevSendtBekreftelse(
                        brevSendt = it.getBoolean("BREV_SENDT"),
                        bekreftetAv = it.getString("BEKREFTET_AV"),
                        bekreftetTidspunkt = it.getLocalDateTime("BEKREFTET_TIDSPUNKT"),
                    )
                )
            }
        }
    }

    // Behandlingen er uavhengig av andre behandlinger, så grunnlaget kopieres ikke
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
    }

    override fun slett(behandlingId: BehandlingId) {
        val bekreftelseIder = connection.queryList(
            "SELECT BEKREFTELSE_ID FROM BEKREFT_BREV_SENDT_GRUNNLAG WHERE BEHANDLING_ID = ?"
        ) {
            setParams { setLong(1, behandlingId.toLong()) }
            setRowMapper { it.getLong("BEKREFTELSE_ID") }
        }

        connection.execute("DELETE FROM BEKREFT_BREV_SENDT_GRUNNLAG WHERE BEHANDLING_ID = ?") {
            setParams { setLong(1, behandlingId.toLong()) }
        }

        connection.executeBatch("DELETE FROM BEKREFT_BREV_SENDT_BEKREFTELSE WHERE ID = ?", bekreftelseIder) {
            setParams { setLong(1, it) }
        }
    }
}
