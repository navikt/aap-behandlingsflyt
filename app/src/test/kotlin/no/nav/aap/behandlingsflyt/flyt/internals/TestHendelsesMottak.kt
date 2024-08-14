package no.nav.aap.behandlingsflyt.flyt.internals

import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.hendelse.mottak.BehandlingHendelse
import no.nav.aap.behandlingsflyt.hendelse.mottak.BehandlingHendelseHåndterer
import no.nav.aap.behandlingsflyt.hendelse.mottak.DokumentMottattSakHendelse
import no.nav.aap.behandlingsflyt.hendelse.mottak.SakHendelse
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlIdentGateway
import no.nav.aap.behandlingsflyt.server.prosessering.BREVKODE
import no.nav.aap.behandlingsflyt.server.prosessering.HendelseMottattHåndteringOppgaveUtfører
import no.nav.aap.behandlingsflyt.server.prosessering.JOURNALPOST_ID
import no.nav.aap.behandlingsflyt.server.prosessering.MOTTATT_TIDSPUNKT
import no.nav.aap.behandlingsflyt.server.prosessering.PERIODE
import no.nav.aap.json.DefaultJsonMapper
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.verdityper.sakogbehandling.Ident
import java.time.LocalDateTime
import javax.sql.DataSource

class TestHendelsesMottak(private val dataSource: DataSource) {

    fun håndtere(key: Ident, hendelse: PersonHendelse) {
        val saksnummer: Saksnummer? = dataSource.transaction { connection ->
            val sak = PersonOgSakService(connection, PdlIdentGateway).finnEllerOpprett(key, hendelse.periode())
            sak.saksnummer
        }
        // Legg til kø for sak, men mocker ved å kalle videre bare
        if (saksnummer != null) {
            håndtere(saksnummer, hendelse.tilSakshendelse())
        }
    }

    private fun håndtere(key: Saksnummer, hendelse: SakHendelse) {
        dataSource.transaction { connection ->
            if (hendelse is DokumentMottattSakHendelse) {
                val sakService = SakService(connection)
                val sak = sakService.hent(key)

                val flytJobbRepository = FlytJobbRepository(connection)

                flytJobbRepository.leggTil(
                    JobbInput(HendelseMottattHåndteringOppgaveUtfører)
                        .forSak(sak.id)
                        .medParameter(JOURNALPOST_ID, hendelse.journalpost.identifikator)
                        .medParameter(BREVKODE, hendelse.strukturertDokument.brevkode.name)
                        .medParameter(MOTTATT_TIDSPUNKT, DefaultJsonMapper.toJson(LocalDateTime.now()))
                        .medParameter(PERIODE, "")
                        .medPayload(DefaultJsonMapper.toJson(hendelse.strukturertDokument.data!!))
                )
            }
        }
    }

    fun håndtere(key: BehandlingId, hendelse: BehandlingHendelse) {
        dataSource.transaction { connection ->
            BehandlingHendelseHåndterer(connection).håndtere(key, hendelse)
        }
    }
}
