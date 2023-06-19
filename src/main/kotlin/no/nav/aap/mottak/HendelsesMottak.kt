package no.nav.aap.mottak

import no.nav.aap.domene.behandling.BehandlingTjeneste
import no.nav.aap.domene.fagsak.FagsakTjeneste
import no.nav.aap.domene.person.PersonTjeneste
import no.nav.aap.domene.typer.Ident
import no.nav.aap.domene.typer.Saksnummer
import no.nav.aap.flyt.kontroll.FlytKontekst
import no.nav.aap.flyt.kontroll.FlytKontroller

object HendelsesMottak {

    private val kontroller = FlytKontroller()

    fun håndtere(key: Ident, hendelse: PersonHendelse) {
        val person = PersonTjeneste.finnEllerOpprett(key)

        val fagsak = FagsakTjeneste.finnEllerOpprett(person, hendelse.periode())

        // Legg til kø for fagsak, men mocker ved å kalle videre bare

        håndtere(fagsak.saksnummer, hendelse.tilSakshendelse())
    }

    fun håndtere(key: Saksnummer, hendelse: SakHendelse) {
        val fagsak = FagsakTjeneste.hent(key)
        val sisteBehandlingOpt = BehandlingTjeneste.finnSisteBehandlingFor(fagsak.id)

        val sisteBehandling = if (sisteBehandlingOpt.isPresent && !sisteBehandlingOpt.get().status().erAvsluttet()) {
            sisteBehandlingOpt.get()
        } else {
            // Har ikke behandling så oppretter en
            BehandlingTjeneste.opprettBehandling(fagsak.id)
        }
        håndtere(key = sisteBehandling.id, hendelse.tilBehandlingHendelse())
    }

    fun håndtere(key: Long, hendelse: LøsAvklaringsbehovBehandlingHendelse) {
        val behandling = BehandlingTjeneste.hent(key)
        kontroller.validerTilstandBehandling(behandling = behandling)

        val fagsak = FagsakTjeneste.hent(behandling.fagsakId)

        val kontekst = FlytKontekst(fagsakId = fagsak.id, behandlingId = behandling.id)
        kontroller.løsAvklaringsbehovOgFortsettProsessering(
            kontekst = kontekst,
            avklaringsbehov = listOf(hendelse.behov())
        )
    }

    fun håndtere(key: Long, hendelse: BehandlingHendelse) {
        val behandling = BehandlingTjeneste.hent(key)
        kontroller.validerTilstandBehandling(behandling = behandling)

        val fagsak = FagsakTjeneste.hent(behandling.fagsakId)

        val kontekst = FlytKontekst(fagsakId = fagsak.id, behandlingId = behandling.id)
        if (hendelse is LøsAvklaringsbehovBehandlingHendelse) {
            throw IllegalArgumentException("Skal håndteres mellom eksplisitt funksjon")
        } else {
            kontroller.prosesserBehandling(kontekst)
        }
    }
}
