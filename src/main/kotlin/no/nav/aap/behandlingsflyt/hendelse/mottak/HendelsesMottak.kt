package no.nav.aap.behandlingsflyt.hendelse.mottak

import no.nav.aap.behandlingsflyt.avklaringsbehov.SattPåVentLøsning
import no.nav.aap.behandlingsflyt.behandling.Behandling
import no.nav.aap.behandlingsflyt.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.behandling.EndringType
import no.nav.aap.behandlingsflyt.behandling.Status
import no.nav.aap.behandlingsflyt.behandling.Årsak
import no.nav.aap.behandlingsflyt.dbstuff.transaction
import no.nav.aap.behandlingsflyt.flyt.AvklaringsbehovOrkestrator
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.flyt.FlytOrkestrator
import no.nav.aap.behandlingsflyt.flyt.ValiderBehandlingTilstand
import no.nav.aap.behandlingsflyt.prosessering.Gruppe
import no.nav.aap.behandlingsflyt.prosessering.OppgaveInput
import no.nav.aap.behandlingsflyt.prosessering.OppgaveRepository
import no.nav.aap.behandlingsflyt.prosessering.ProsesserBehandlingOppgave
import no.nav.aap.behandlingsflyt.sak.Sak
import no.nav.aap.behandlingsflyt.sak.SakRepository
import no.nav.aap.behandlingsflyt.sak.SakService
import no.nav.aap.behandlingsflyt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sak.person.Ident
import no.nav.aap.behandlingsflyt.sak.person.PersonRepository
import javax.sql.DataSource

class HendelsesMottak(private val dataSource: DataSource) {

    fun håndtere(key: Ident, hendelse: PersonHendelse) {
        var sak: Sak? = null
        dataSource.transaction { connection ->
            val person = PersonRepository(connection).finnEllerOpprett(key)

            sak = SakRepository(connection).finnEllerOpprett(person, hendelse.periode())

            // Legg til kø for sak, men mocker ved å kalle videre bare
        }
        håndtere(sak!!.saksnummer, hendelse.tilSakshendelse())
    }

    fun håndtere(key: Saksnummer, hendelse: SakHendelse) {
        var sisteBehandling: Behandling? = null
        dataSource.transaction { connection ->
            val sak = SakRepository(connection).hent(key)
            val sisteBehandlingOpt = BehandlingRepository.finnSisteBehandlingFor(sak.id)

            sisteBehandling = if (sisteBehandlingOpt != null && !sisteBehandlingOpt.status().erAvsluttet()) {
                sisteBehandlingOpt
            } else {
                // Har ikke behandling så oppretter en
                BehandlingRepository.opprettBehandling(
                    sak.id,
                    listOf(Årsak(EndringType.MOTTATT_SØKNAD))
                ) // TODO: Reeltsett oppdatere denne
            }
        }
        håndtere(key = sisteBehandling!!.id, hendelse.tilBehandlingHendelse())
    }

    fun håndtere(key: Long, hendelse: LøsAvklaringsbehovBehandlingHendelse) {
        dataSource.transaction { connection ->
            val behandling = BehandlingRepository.hent(key)
            ValiderBehandlingTilstand.validerTilstandBehandling(behandling = behandling)

            val sakService = SakService(connection)
            val sak = sakService.hent(behandling.sakId)

            val kontekst = FlytKontekst(sakId = sak.id, behandlingId = behandling.id)
            val avklaringsbehovKontroller = AvklaringsbehovOrkestrator(connection)
            avklaringsbehovKontroller.løsAvklaringsbehovOgFortsettProsessering(
                kontekst = kontekst,
                avklaringsbehov = listOf(hendelse.behov())
            )
        }
    }

    fun håndtere(key: Long, hendelse: BehandlingSattPåVent) {
        dataSource.transaction { connection ->
            val behandling = BehandlingRepository.hent(key)
            ValiderBehandlingTilstand.validerTilstandBehandling(behandling = behandling)

            val sakService = SakService(connection)
            val sak = sakService.hent(behandling.sakId)

            val kontekst = FlytKontekst(sakId = sak.id, behandlingId = behandling.id)
            val kontroller = FlytOrkestrator(connection)
            kontroller.settBehandlingPåVent(kontekst)
        }
    }

    fun håndtere(key: Long, hendelse: BehandlingHendelse) {
        dataSource.transaction { connection ->
            val behandling = BehandlingRepository.hent(key)
            ValiderBehandlingTilstand.validerTilstandBehandling(behandling = behandling)

            val sakService = SakService(connection)
            val sak = sakService.hent(behandling.sakId)

            val kontekst = FlytKontekst(sakId = sak.id, behandlingId = behandling.id)
            if (hendelse is LøsAvklaringsbehovBehandlingHendelse) {
                throw IllegalArgumentException("Skal håndteres mellom eksplisitt funksjon")
            } else {
                if (behandling.status() == Status.PÅ_VENT) {
                    val avklaringsbehovKontroller = AvklaringsbehovOrkestrator(connection)
                    avklaringsbehovKontroller.løsAvklaringsbehov(
                        kontekst = kontekst,
                        avklaringsbehov = listOf(SattPåVentLøsning())
                    )
                }
                OppgaveRepository.leggTil(
                    Gruppe().leggTil(
                        OppgaveInput(oppgave = ProsesserBehandlingOppgave).forBehandling(
                            kontekst.sakId,
                            kontekst.behandlingId
                        )
                    )
                )
            }
        }
    }
}
