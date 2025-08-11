package no.nav.aap.behandlingsflyt.flyt.internals

import no.nav.aap.behandlingsflyt.SYSTEMBRUKER
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovOrkestrator
import no.nav.aap.behandlingsflyt.hendelse.mottak.BehandlingSattPåVent
import no.nav.aap.behandlingsflyt.integrasjon.ident.PdlIdentGateway
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.prosessering.HendelseMottattHåndteringJobbUtfører
import no.nav.aap.behandlingsflyt.repository.postgresRepositoryRegistry
import no.nav.aap.behandlingsflyt.repository.sak.PersonRepositoryImpl
import no.nav.aap.behandlingsflyt.repository.sak.SakRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.Ident
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonOgSakService
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
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
                SakRepositoryImpl(connection)
            ).finnEllerOpprett(key, hendelse.periode())
            sak.saksnummer
        }
        // Legg til kø for sak, men mocker ved å kalle videre bare
        håndtere(saksnummer, hendelse.tilSakshendelse())
    }

    fun håndtere(key: BehandlingId, hendelse: BehandlingSattPåVent) {
        dataSource.transaction { connection ->
            AvklaringsbehovOrkestrator(postgresRepositoryRegistry.provider(connection), GatewayProvider)
                .settBehandlingPåVent(key, hendelse)
        }
    }

    fun bestillLegeerklæring(key: BehandlingId) {
        dataSource.transaction { connection ->
            AvklaringsbehovOrkestrator(postgresRepositoryRegistry.provider(connection), GatewayProvider)
                .settPåVentMensVentePåMedisinskeOpplysninger(key, SYSTEMBRUKER)
        }
    }

    private fun håndtere(key: Saksnummer, hendelse: SakHendelse) {
        dataSource.transaction { connection ->
            if (hendelse is DokumentMottattSakHendelse || hendelse is NyÅrsakTilBehandlingSakHendelse) {
                val sakService = SakService(SakRepositoryImpl(connection))
                val sak = sakService.hent(key)

                val flytJobbRepository = FlytJobbRepository(connection)

                flytJobbRepository.leggTil(
                    HendelseMottattHåndteringJobbUtfører.nyJobb(
                        sakId = sak.id,
                        dokumentReferanse = hendelse.getInnsendingReferanse(),
                        brevkategori = hendelse.getInnsendingType(),
                        kanal = Kanal.DIGITAL,
                        melding = hendelse.getMelding()?.data,
                        mottattTidspunkt = LocalDateTime.now()
                    )
                )
            }
        }
    }
}
