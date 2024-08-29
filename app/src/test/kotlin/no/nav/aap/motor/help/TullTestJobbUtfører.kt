package no.nav.aap.motor.help

import no.nav.aap.komponenter.dbconnect.DBConnection
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

        override fun navn(): String {
            return "tull"
        }

        override fun beskrivelse(): String {
            return "tull"
        }
    }
}
