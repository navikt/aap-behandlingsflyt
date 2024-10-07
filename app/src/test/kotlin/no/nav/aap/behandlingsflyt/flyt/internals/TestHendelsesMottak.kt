package no.nav.aap.behandlingsflyt.flyt.internals

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentReferanse
import no.nav.aap.behandlingsflyt.hendelse.mottak.BehandlingHendelse
import no.nav.aap.behandlingsflyt.hendelse.mottak.BehandlingHendelseHåndterer
import no.nav.aap.behandlingsflyt.hendelse.mottak.DokumentMottattSakHendelse
import no.nav.aap.behandlingsflyt.hendelse.mottak.SakHendelse
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlIdentGateway
import no.nav.aap.behandlingsflyt.server.prosessering.HendelseMottattHåndteringJobbUtfører
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.verdityper.sakogbehandling.Ident
import javax.sql.DataSource

class TestHendelsesMottak(private val dataSource: DataSource) {

    fun håndtere(key: Ident, hendelse: PersonHendelse) {
        val saksnummer: Saksnummer = dataSource.transaction { connection ->
            val sak = PersonOgSakService(connection, PdlIdentGateway).finnEllerOpprett(key, hendelse.periode())
            sak.saksnummer
        }
        // Legg til kø for sak, men mocker ved å kalle videre bare
        håndtere(saksnummer, hendelse.tilSakshendelse())
    }

    private fun håndtere(key: Saksnummer, hendelse: SakHendelse) {
        dataSource.transaction { connection ->
            if (hendelse is DokumentMottattSakHendelse) {
                val sakService = SakService(connection)
                val sak = sakService.hent(key)

                val flytJobbRepository = FlytJobbRepository(connection)

                val referanse = MottattDokumentReferanse(hendelse.journalpost)

                flytJobbRepository.leggTil(
                    HendelseMottattHåndteringJobbUtfører.nyJobb(
                        sakId = sak.id,
                        dokumentReferanse = referanse,
                        brevkode = hendelse.strukturertDokument.brevkode,
                        periode = null,
                        payload = hendelse.strukturertDokument.data!!
                    )
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
