package no.nav.aap.motor.help

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører

class TøysOgTullTestJobbUtfører() : JobbUtfører {

    override fun utfør(input: JobbInput) {
    }

    companion object : Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            return TøysOgTullTestJobbUtfører()
        }

        override fun type(): String {
            return "tøys.tull"
        }

        override fun navn(): String {
            return type()
        }

        override fun beskrivelse(): String {
            return type()
        }
    }
}
