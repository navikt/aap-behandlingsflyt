package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.mdc.JobbLogInfoProvider
import no.nav.aap.motor.mdc.LogInformasjon

object BehandlingsflytLogInfoProvider : JobbLogInfoProvider {

    override fun hentInformasjon(connection: DBConnection, jobbInput: JobbInput): LogInformasjon? {
        val behandlingId = jobbInput.behandlingIdOrNull()
        val sakId = jobbInput.sakIdOrNull()

        if (behandlingId != null) {
            val query = """
            SELECT s.saksnummer, b.referanse
            FROM SAK s
                INNER JOIN BEHANDLING b on s.id = b.sak_id
            WHERE b.id = ?
        """.trimIndent()

            return connection.queryFirst(query) {
                setParams {
                    setLong(1, behandlingId)
                }
                setRowMapper { row ->
                    LogInformasjon(mapOf("saksnummer" to row.getString("saksnummer"), "behandlingReferanse" to row.getString("referanse")))
                }
            }
        } else if (sakId != null) {
            val query = """
            SELECT s.saksnummer
            FROM SAK s
            WHERE s.id = ?
        """.trimIndent()

            return connection.queryFirst(query) {
                setParams {
                    setLong(1, sakId)
                }
                setRowMapper {
                    LogInformasjon(mapOf("saksnummer" to it.getString("saksnummer")))
                }
            }
        }
        return null
    }
}