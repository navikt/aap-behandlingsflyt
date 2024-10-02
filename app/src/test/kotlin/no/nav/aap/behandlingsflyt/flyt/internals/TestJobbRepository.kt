package no.nav.aap.behandlingsflyt.flyt.internals

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.JobbStatus
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.verdityper.sakogbehandling.SakId

class TestJobbRepository(private val connection: DBConnection) {
    fun harOppgaver(sakId: SakId?, behandlingId: BehandlingId?): Boolean {

        var query = "SELECT count(1) as antall " +
                "FROM JOBB " +
                "WHERE status not in ('${JobbStatus.FERDIG.name}', '${JobbStatus.FEILET.name}')"

        val params = HashMap<String, Long>()

        if (sakId != null) {
            query += " AND sak_id = ?"
            params["sak_id"] = sakId.toLong()
        }
        if (behandlingId != null) {
            query += " AND behandling_id = ?"
            params["behandling_id"] = behandlingId.toLong()
        }


        val antall =
            connection.queryFirst(
                query
            ) {
                if (params.isNotEmpty()) {
                    setParams {
                        if (params["sak_id"] != null && params["behandling_id"] == null) {
                            setLong(1, params["sak_id"]!!)
                        }
                        if (params["sak_id"] == null && params["behandling_id"] != null) {
                            setLong(1, params["behandling_id"]!!)
                        }
                        if (params["sak_id"] != null && params["behandling_id"] != null) {
                            setLong(1, params["sak_id"]!!)
                            setLong(2, params["behandling_id"]!!)
                        }
                    }
                }
                setRowMapper {
                    it.getLong("antall") > 0
                }
            }
        return antall
    }
}
