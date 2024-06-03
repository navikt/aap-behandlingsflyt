package no.nav.aap.motor.help

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører

class TullTestJobbUtfører() : JobbUtfører {

    override fun utfør(input: JobbInput) {
    }

    companion object : Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            return TullTestJobbUtfører()
        }

        override fun type(): String {
            return "Tull"
        }
    }
}
