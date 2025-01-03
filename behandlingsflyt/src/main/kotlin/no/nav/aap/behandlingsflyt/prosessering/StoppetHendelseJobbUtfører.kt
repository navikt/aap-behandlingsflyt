package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.hendelse.oppgavestyring.OppgavestyringGateway
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.BehandlingFlytStoppetHendelse
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(StoppetHendelseJobbUtfører::class.java)

class StoppetHendelseJobbUtfører private constructor() : JobbUtfører {

    override fun utfør(input: JobbInput) {
        val hendelse = input.payload<BehandlingFlytStoppetHendelse>()

        log.info("Varsler hendelse til OppgaveStyring. ${hendelse.saksnummer} :: ${hendelse.referanse.referanse}")
        OppgavestyringGateway.varsleHendelse(hendelse)
    }

    companion object : Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            return StoppetHendelseJobbUtfører()
        }

        override fun type(): String {
            return "flyt.hendelse"
        }

        override fun navn(): String {
            return "Oppgavestyrings hendelse"
        }

        override fun beskrivelse(): String {
            return "Produsere hendelse til oppgavestyring"
        }
    }
}
