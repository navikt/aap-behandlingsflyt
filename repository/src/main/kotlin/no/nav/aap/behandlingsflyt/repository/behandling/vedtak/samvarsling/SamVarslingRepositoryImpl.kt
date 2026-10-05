package no.nav.aap.behandlingsflyt.repository.behandling.vedtak.samvarsling

import no.nav.aap.behandlingsflyt.datadeling.sam.LagretSamVarsling
import no.nav.aap.behandlingsflyt.datadeling.sam.SamVarsling
import no.nav.aap.behandlingsflyt.datadeling.sam.SamVarslingRepository
import no.nav.aap.behandlingsflyt.datadeling.sam.SamordneVedtakRequest
import no.nav.aap.behandlingsflyt.datadeling.sam.SamordneVedtakRespons
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.lookup.repository.Factory

class SamVarslingRepositoryImpl(private val connection: DBConnection) : SamVarslingRepository {

    companion object : Factory<SamVarslingRepository> {
        override fun konstruer(connection: DBConnection): SamVarslingRepository {
            return SamVarslingRepositoryImpl(connection)
        }
    }

    override fun lagre(behandlingId: BehandlingId, varsling: SamVarsling) {
        connection.execute(
            """
            INSERT INTO SAM_VARSLING (BEHANDLING_ID, VEDTAK_ID, VARSLET, FORSTEGANGSBEHANDLING,
                                      ENDRING_I_RETTIGHETSTYPE, ANTALL_TP_YTELSER, REQUEST, VENT_PAA_SVAR)
            VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, ?)
            """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.id)
                setLong(2, varsling.vedtakId)
                setBoolean(3, varsling.varslet)
                setBoolean(4, varsling.førstegangsbehandling)
                setBoolean(5, varsling.endringIRettighetstype)
                setInt(6, varsling.antallTpYtelser)
                setString(7, DefaultJsonMapper.toJson(varsling.request))
                setBoolean(8, varsling.respons?.ventPaaSvar)
            }
        }
    }

    override fun hent(behandlingId: BehandlingId): List<LagretSamVarsling> {
        return connection.queryList(
            """
            SELECT * FROM SAM_VARSLING WHERE BEHANDLING_ID = ? ORDER BY OPPRETTET_TID, ID
            """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.id)
            }
            setRowMapper { row ->
                LagretSamVarsling(
                    varsling = SamVarsling(
                        vedtakId = row.getLong("vedtak_id"),
                        varslet = row.getBoolean("varslet"),
                        førstegangsbehandling = row.getBoolean("forstegangsbehandling"),
                        endringIRettighetstype = row.getBoolean("endring_i_rettighetstype"),
                        antallTpYtelser = row.getInt("antall_tp_ytelser"),
                        request = DefaultJsonMapper.fromJson<SamordneVedtakRequest>(row.getString("request")),
                        respons = row.getBooleanOrNull("vent_paa_svar")?.let { SamordneVedtakRespons(it) },
                    ),
                    opprettetTid = row.getLocalDateTime("opprettet_tid"),
                )
            }
        }
    }
}
