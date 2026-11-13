package no.nav.aap.utenlandsopphold

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.Factory

class SøknadUtenlandsoppholdRepositoryImpl(private val connection: DBConnection) : SøknadUtenlandsoppholdRepository {

    companion object : Factory<SøknadUtenlandsoppholdRepositoryImpl> {
        override fun konstruer(connection: DBConnection): SøknadUtenlandsoppholdRepositoryImpl {
            return SøknadUtenlandsoppholdRepositoryImpl(connection)
        }
    }

    override fun lagre(behandlingId: BehandlingId, vurdering: SøknadUtenlandsoppholdVurdering) {
        connection.execute("UPDATE SOKNAD_UTENLANDSOPPHOLD_GRUNNLAG SET AKTIV = FALSE WHERE BEHANDLING_ID = ? AND AKTIV") {
            setParams { setLong(1, behandlingId.toLong()) }
        }

        val vurderingId = connection.executeReturnKey(
            """
            INSERT INTO SOKNAD_UTENLANDSOPPHOLD_VURDERING (SOKNADSPERIODE, LAND, RESULTAT, GODKJENT_PERIODE,
                                                           ANTALL_GODKJENTE_DAGER, BEGRUNNELSE, VURDERT_AV,
                                                           VURDERT_TIDSPUNKT)
            VALUES (?::daterange, ?, ?, ?::daterange, ?, ?, ?, ?)
            """.trimIndent()
        ) {
            setParams {
                setPeriode(1, vurdering.søknadsperiode)
                setString(2, vurdering.land)
                setEnumName(3, vurdering.resultat)
                setPeriode(4, vurdering.godkjentPeriode)
                setInt(5, vurdering.antallGodkjenteDager)
                setString(6, vurdering.begrunnelse)
                setBruker(7, vurdering.vurdertAv)
                setLocalDateTime(8, vurdering.vurdertTidspunkt)
            }
        }

        connection.execute("INSERT INTO SOKNAD_UTENLANDSOPPHOLD_GRUNNLAG (BEHANDLING_ID, VURDERING_ID) VALUES (?, ?)") {
            setParams {
                setLong(1, behandlingId.toLong())
                setLong(2, vurderingId)
            }
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): SøknadUtenlandsoppholdGrunnlag? {
        return connection.queryFirstOrNull(
            """
            SELECT v.*
            FROM SOKNAD_UTENLANDSOPPHOLD_GRUNNLAG g
                     JOIN SOKNAD_UTENLANDSOPPHOLD_VURDERING v ON g.VURDERING_ID = v.ID
            WHERE g.BEHANDLING_ID = ? AND g.AKTIV
            """.trimIndent()
        ) {
            setParams { setLong(1, behandlingId.toLong()) }
            setRowMapper {
                SøknadUtenlandsoppholdGrunnlag(
                    SøknadUtenlandsoppholdVurdering(
                        søknadsperiode = it.getPeriode("SOKNADSPERIODE"),
                        land = it.getString("LAND"),
                        resultat = it.getEnum("RESULTAT"),
                        godkjentPeriode = it.getPeriodeOrNull("GODKJENT_PERIODE"),
                        antallGodkjenteDager = it.getIntOrNull("ANTALL_GODKJENTE_DAGER"),
                        begrunnelse = it.getString("BEGRUNNELSE"),
                        vurdertAv = it.getBruker("VURDERT_AV"),
                        vurdertTidspunkt = it.getLocalDateTime("VURDERT_TIDSPUNKT"),
                    )
                )
            }
        }
    }

    // Behandlingen er uavhengig av andre behandlinger, så grunnlaget kopieres ikke
    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
    }

    override fun slett(behandlingId: BehandlingId) {
        val vurderingIder = connection.queryList(
            "SELECT VURDERING_ID FROM SOKNAD_UTENLANDSOPPHOLD_GRUNNLAG WHERE BEHANDLING_ID = ?"
        ) {
            setParams { setLong(1, behandlingId.toLong()) }
            setRowMapper { it.getLong("VURDERING_ID") }
        }

        connection.execute("DELETE FROM SOKNAD_UTENLANDSOPPHOLD_GRUNNLAG WHERE BEHANDLING_ID = ?") {
            setParams { setLong(1, behandlingId.toLong()) }
        }

        connection.executeBatch("DELETE FROM SOKNAD_UTENLANDSOPPHOLD_VURDERING WHERE ID = ?", vurderingIder) {
            setParams { setLong(1, it) }
        }
    }
}
