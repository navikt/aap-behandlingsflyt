package no.nav.aap.behandlingsflyt.hendelse.mottak

import no.nav.aap.behandlingsflyt.avklaringsbehov.SattPåVentLøsning
import no.nav.aap.behandlingsflyt.dbstuff.transaction
import no.nav.aap.behandlingsflyt.domene.behandling.BehandlingTjeneste
import no.nav.aap.behandlingsflyt.domene.behandling.EndringType
import no.nav.aap.behandlingsflyt.domene.behandling.Status
import no.nav.aap.behandlingsflyt.domene.behandling.Årsak
import no.nav.aap.behandlingsflyt.sak.person.Ident
import no.nav.aap.behandlingsflyt.sak.person.Personlager
import no.nav.aap.behandlingsflyt.sak.SakRepository
import no.nav.aap.behandlingsflyt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.flyt.AvklaringsbehovOrkestrator
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.flyt.FlytOrkestrator
import no.nav.aap.behandlingsflyt.flyt.ValiderBehandlingTilstand
import no.nav.aap.behandlingsflyt.prosessering.Gruppe
import no.nav.aap.behandlingsflyt.prosessering.OppgaveInput
import no.nav.aap.behandlingsflyt.prosessering.OppgaveRepository
import no.nav.aap.behandlingsflyt.prosessering.ProsesserBehandlingOppgave
import javax.sql.DataSource

class HendelsesMottak(private val dataSource: DataSource) {

    fun håndtere(key: Ident, hendelse: PersonHendelse) {
        val person = Personlager.finnEllerOpprett(key)

        val sak = SakRepository.finnEllerOpprett(person, hendelse.periode())

        // Legg til kø for sak, men mocker ved å kalle videre bare

        håndtere(sak.saksnummer, hendelse.tilSakshendelse())
    }

    fun håndtere(key: Saksnummer, hendelse: SakHendelse) {
        val sak = SakRepository.hent(key)
        val sisteBehandlingOpt = BehandlingTjeneste.finnSisteBehandlingFor(sak.id)

        val sisteBehandling = if (sisteBehandlingOpt != null && !sisteBehandlingOpt.status().erAvsluttet()) {
            sisteBehandlingOpt
        } else {
            // Har ikke behandling så oppretter en
            BehandlingTjeneste.opprettBehandling(
                sak.id,
                listOf(Årsak(EndringType.MOTTATT_SØKNAD))
            ) // TODO: Reeltsett oppdatere denne
        }
        håndtere(key = sisteBehandling.id, hendelse.tilBehandlingHendelse())
    }

    fun håndtere(key: Long, hendelse: LøsAvklaringsbehovBehandlingHendelse) {
        dataSource.transaction { connection ->
            val behandling = BehandlingTjeneste.hent(key)
            ValiderBehandlingTilstand.validerTilstandBehandling(behandling = behandling)

            val sak = SakRepository.hent(behandling.sakId)

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
            val behandling = BehandlingTjeneste.hent(key)
            ValiderBehandlingTilstand.validerTilstandBehandling(behandling = behandling)

            val sak = SakRepository.hent(behandling.sakId)

            val kontekst = FlytKontekst(sakId = sak.id, behandlingId = behandling.id)
            val kontroller = FlytOrkestrator(connection)
            kontroller.settBehandlingPåVent(kontekst)
        }
    }

    fun håndtere(key: Long, hendelse: BehandlingHendelse) {
        dataSource.transaction { connection ->
            val behandling = BehandlingTjeneste.hent(key)
            ValiderBehandlingTilstand.validerTilstandBehandling(behandling = behandling)

            val sak = SakRepository.hent(behandling.sakId)

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
