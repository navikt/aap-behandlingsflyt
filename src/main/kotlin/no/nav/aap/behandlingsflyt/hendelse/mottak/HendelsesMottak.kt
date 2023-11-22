package no.nav.aap.behandlingsflyt.hendelse.mottak

import no.nav.aap.behandlingsflyt.avklaringsbehov.SattPåVentLøsning
import no.nav.aap.behandlingsflyt.behandling.Behandling
import no.nav.aap.behandlingsflyt.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.behandling.EndringType
import no.nav.aap.behandlingsflyt.behandling.Status
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.Årsak
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.flyt.AvklaringsbehovOrkestrator
import no.nav.aap.behandlingsflyt.flyt.FlytOrkestrator
import no.nav.aap.behandlingsflyt.flyt.ValiderBehandlingTilstand
import no.nav.aap.behandlingsflyt.flyt.tilKontekst
import no.nav.aap.behandlingsflyt.prosessering.OppgaveInput
import no.nav.aap.behandlingsflyt.prosessering.OppgaveRepository
import no.nav.aap.behandlingsflyt.prosessering.ProsesserBehandlingOppgave
import no.nav.aap.behandlingsflyt.sak.Ident
import no.nav.aap.behandlingsflyt.sak.PersonRepository
import no.nav.aap.behandlingsflyt.sak.Sak
import no.nav.aap.behandlingsflyt.sak.SakRepository
import no.nav.aap.behandlingsflyt.sak.Saksnummer
import javax.sql.DataSource

class HendelsesMottak(private val dataSource: DataSource) {

    fun håndtere(key: Ident, hendelse: PersonHendelse) {
        val sak: Sak = dataSource.transaction { connection ->
            val person = PersonRepository(connection).finnEllerOpprett(key)

            SakRepository(connection).finnEllerOpprett(person, hendelse.periode())

            // Legg til kø for sak, men mocker ved å kalle videre bare
        }
        håndtere(sak.saksnummer, hendelse.tilSakshendelse())
    }

    fun håndtere(key: Saksnummer, hendelse: SakHendelse) {
        val sisteBehandling: Behandling = dataSource.transaction { connection ->
            val sak = SakRepository(connection).hent(key)
            val behandlingRepository = BehandlingRepository(connection)
            val sisteBehandlingOpt = behandlingRepository.finnSisteBehandlingFor(sak.id)

            if (sisteBehandlingOpt != null && !sisteBehandlingOpt.status().erAvsluttet()) {
                sisteBehandlingOpt
            } else {
                // Har ikke behandling så oppretter en
                behandlingRepository.opprettBehandling(
                    sak.id,
                    listOf(Årsak(EndringType.MOTTATT_SØKNAD))
                ) // TODO: Reeltsett oppdatere denne
            }
        }
        håndtere(sisteBehandling.id, hendelse.tilBehandlingHendelse())
    }

    fun håndtere(connection: DBConnection, key: BehandlingId, hendelse: LøsAvklaringsbehovBehandlingHendelse) {
        val behandling = BehandlingRepository(connection).hent(key)
        val avklaringsbehovene = AvklaringsbehovRepository(connection).hent(behandling.id)
        ValiderBehandlingTilstand.validerTilstandBehandling(
            behandling = behandling,
            eksisterenedeAvklaringsbehov = avklaringsbehovene.alle()
        )

        val kontekst = tilKontekst(behandling)
        val avklaringsbehovKontroller = AvklaringsbehovOrkestrator(connection)
        avklaringsbehovKontroller.løsAvklaringsbehovOgFortsettProsessering(
            kontekst = kontekst,
            avklaringsbehov = listOf(hendelse.behov())
        )
    }

    fun håndtere(key: BehandlingId, hendelse: BehandlingSattPåVent) {
        dataSource.transaction { connection ->
            val behandling = BehandlingRepository(connection).hent(key)
            val avklaringsbehovene = AvklaringsbehovRepository(connection).hent(behandling.id)
            ValiderBehandlingTilstand.validerTilstandBehandling(
                behandling = behandling,
                eksisterenedeAvklaringsbehov = avklaringsbehovene.alle()
            )

            val kontekst = tilKontekst(behandling)
            val kontroller = FlytOrkestrator(connection)
            kontroller.settBehandlingPåVent(kontekst)
        }
    }

    fun håndtere(key: BehandlingId, hendelse: BehandlingHendelse) {
        dataSource.transaction { connection ->
            val behandling = BehandlingRepository(connection).hent(key)
            val avklaringsbehovene = AvklaringsbehovRepository(connection).hent(behandling.id)
            ValiderBehandlingTilstand.validerTilstandBehandling(
                behandling = behandling,
                eksisterenedeAvklaringsbehov = avklaringsbehovene.alle()
            )

            val kontekst = tilKontekst(behandling)
            if (behandling.status() == Status.PÅ_VENT) {
                val avklaringsbehovKontroller = AvklaringsbehovOrkestrator(connection)
                avklaringsbehovKontroller.løsAvklaringsbehov(
                    kontekst = kontekst,
                    avklaringsbehov = listOf(SattPåVentLøsning())
                )
            }
            OppgaveRepository(connection).leggTil(
                OppgaveInput(oppgave = ProsesserBehandlingOppgave).forBehandling(
                    kontekst.sakId,
                    kontekst.behandlingId
                )
            )
        }
    }
}
