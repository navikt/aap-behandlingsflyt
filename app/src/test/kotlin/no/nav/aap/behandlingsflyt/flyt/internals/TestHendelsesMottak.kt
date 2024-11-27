package no.nav.aap.behandlingsflyt.flyt.internals

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovOrkestrator
import no.nav.aap.behandlingsflyt.hendelse.avløp.BehandlingHendelseServiceImpl
import no.nav.aap.behandlingsflyt.hendelse.mottak.BehandlingSattPåVent
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters.PdlIdentGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.server.prosessering.HendelseMottattHåndteringJobbUtfører
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.verdityper.dokument.Kanal
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

    fun håndtere(key: BehandlingId, hendelse: BehandlingSattPåVent) {
        dataSource.transaction { connection ->
            AvklaringsbehovOrkestrator(
                connection,
                BehandlingHendelseServiceImpl(FlytJobbRepository(connection), SakService(SakRepositoryImpl(connection)))
            ).settBehandlingPåVent(key, hendelse)
        }
    }

    private fun håndtere(key: Saksnummer, hendelse: SakHendelse) {
        dataSource.transaction { connection ->
            if (hendelse is DokumentMottattSakHendelse) {
                val sakService = SakService(SakRepositoryImpl(connection))
                val sak = sakService.hent(key)

                val flytJobbRepository = FlytJobbRepository(connection)

                val referanse = InnsendingReferanse(hendelse.journalpost)

                flytJobbRepository.leggTil(
                    HendelseMottattHåndteringJobbUtfører.nyJobb(
                        sakId = sak.id,
                        dokumentReferanse = referanse,
                        brevkategori = hendelse.strukturertDokument.brevkategori,
                        kanal = Kanal.DIGITAL,
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
