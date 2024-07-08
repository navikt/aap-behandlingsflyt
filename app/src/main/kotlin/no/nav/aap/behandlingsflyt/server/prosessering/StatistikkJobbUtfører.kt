package no.nav.aap.behandlingsflyt.server.prosessering

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.hendelse.avløp.BehandlingFlytStoppetHendelse
import no.nav.aap.behandlingsflyt.hendelse.statistikk.StatistikkGateway
import no.nav.aap.behandlingsflyt.hendelse.statistikk.StatistikkHendelseDTO
import no.nav.aap.json.DefaultJsonMapper
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import org.slf4j.LoggerFactory

val logger = LoggerFactory.getLogger(StatistikkJobbUtfører::class.java)

class StatistikkJobbUtfører(private val statistikkGateway: StatistikkGateway) : JobbUtfører {
    override fun utfør(input: JobbInput) {
        // TODO: undersøk om denne er relevant utenfor domeneet
        // for nå i poc, send til statistikk

        logger.info("Utfører jobbinput statistikk: $input")
        val payload = input.payload()

        val hendelse = DefaultJsonMapper.fromJson<BehandlingFlytStoppetHendelse>(payload)

        statistikkGateway.avgiStatistikk(
            StatistikkHendelseDTO(
                saksnummer = hendelse.saksnummer.toString(),
                behandlingType = hendelse.behandlingType,
                status = hendelse.status
            )
        )
    }


    companion object : Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            return StatistikkJobbUtfører(StatistikkGateway())
        }

        override fun type(): String {
            return "flyt.statistikk"
        }

        override fun navn(): String {
            return "Lagrer statistikk"
        }

        override fun beskrivelse(): String {
            return "Skal ta i mot data fra steg i en behandling og sender til statistikk-appen."
        }
    }
}