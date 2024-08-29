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

        var params = HashMap<String, Long>()

        if (sakId != null) {
            query += " AND sak_id = :sak_id"
            params["sak_id"] = sakId.toLong()
        }
        if (behandlingId != null) {
            query += " AND behandling_id = :behandling_id"
            params["behandling_id"] = behandlingId.toLong()
        }


        val antall =
            connection.queryFirst(
                query
            ) {
                if (params.isNotEmpty()) {
                    setNamedParams {
                        for (param in params) {
                            setLong(param.key, param.value)
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
