package no.nav.aap.motor.mdc

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.motor.JobbInput

object NoExtraLogInfoProvider : JobbLogInfoProvider {
    override fun hentInformasjon(connection: DBConnection, jobbInput: JobbInput): LogInformasjon? {
        return null
    }
}
