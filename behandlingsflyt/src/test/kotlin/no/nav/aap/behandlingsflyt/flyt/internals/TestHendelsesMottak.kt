package no.nav.aap.behandlingsflyt.flyt.internals

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovOrkestrator
import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.hendelse.mottak.BehandlingSattPåVent
import no.nav.aap.behandlingsflyt.integrasjon.ident.PdlIdentGateway
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.innsendingType
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.prosessering.HendelseMottattHåndteringJobbUtfører
import no.nav.aap.behandlingsflyt.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryAvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.test.inmemoryrepo.InMemoryTrukketSøknadRepository
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.verdityper.dokument.Kanal
import java.time.LocalDateTime
import javax.sql.DataSource

class TestHendelsesMottak(private val dataSource: DataSource) {

    fun håndtere(key: Ident, hendelse: PersonHendelse) {
        val saksnummer: Saksnummer = dataSource.transaction { connection ->
            val sak = PersonOgSakService(
                PdlIdentGateway(),
                PersonRepositoryImpl(connection),
                SakRepositoryImpl(connection),
                BehandlingRepositoryImpl(connection),
                TrukketSøknadService(
                    InMemoryAvklaringsbehovRepository,
                    InMemoryTrukketSøknadRepository
                ),
            ).finnEllerOpprett(key, hendelse.periode())
            sak.saksnummer
        }
        // Legg til kø for sak, men mocker ved å kalle videre bare
        håndtere(saksnummer, hendelse.tilSakshendelse())
    }

    fun håndtere(key: BehandlingId, hendelse: BehandlingSattPåVent) {
        dataSource.transaction { connection ->
            AvklaringsbehovOrkestrator(postgresRepositoryRegistry.provider(connection))
                .settBehandlingPåVent(key, hendelse)
        }
    }

    fun bestillLegeerklæring(key: BehandlingId) {
        dataSource.transaction { connection ->
            AvklaringsbehovOrkestrator(postgresRepositoryRegistry.provider(connection))
                .settPåVentMensVentePåMedisinskeOpplysninger(key, SYSTEMBRUKER)
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
                        brevkategori = hendelse.innsendingType
                            ?: hendelse.strukturertDokument?.data?.innsendingType()!!,
                        kanal = Kanal.DIGITAL,
                        melding = hendelse.strukturertDokument?.data,
                        mottattTidspunkt = LocalDateTime.now()
                    )
                )
            }
        }
    }
}
