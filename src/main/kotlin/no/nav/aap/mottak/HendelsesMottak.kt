package no.nav.aap.mottak

import no.nav.aap.domene.behandling.BehandlingTjeneste
import no.nav.aap.domene.fagsak.FagsakTjeneste
import no.nav.aap.domene.person.PersonTjeneste
import no.nav.aap.flyt.kontroll.FlytKontekst
import no.nav.aap.flyt.kontroll.FlytKontroller

object HendelsesMottak {

    private val kontroller = FlytKontroller()

    fun håndtere(hendelse: PersonHendelse) {
        val ident = hendelse.ident()
        val person = PersonTjeneste.finnEllerOpprett(ident)

        val fagsak = FagsakTjeneste.finnEllerOpprett(person, hendelse.periode())

        // Legg til kø for fagsak, men mocker ved å kalle videre bare

        håndtere(hendelse.tilSakshendelse(fagsak.saksnummer))
    }

    fun håndtere(hendelse: SakHendelse) {
        val fagsak = FagsakTjeneste.hent(hendelse.saksnummer())
        val sisteBehandlingOpt = BehandlingTjeneste.finnSisteBehandlingFor(fagsak.id)

        val sisteBehandling = if (sisteBehandlingOpt.isPresent && !sisteBehandlingOpt.get().status().erAvsluttet()) {
            sisteBehandlingOpt.get()
        } else {
            // Har ikke behandling så oppretter en
            BehandlingTjeneste.opprettBehandling(fagsak.id)
        }
        håndtere(hendelse.tilBehandlingHendelse(sisteBehandling.id))
    }

    fun håndtere(hendelse: BehandlingHendelse) {
        val behandling = BehandlingTjeneste.hent(hendelse.behandlingId())
        val fagsak = FagsakTjeneste.hent(behandling.fagsakId)

        val kontekst = FlytKontekst(fagsakId = fagsak.id, behandlingId = behandling.id)
        if (hendelse is LøsAvklaringsbehovBehandlingHendelse) {
            kontroller.løsAvklaringsbehovOgFortsettProsessering(
                kontekst = kontekst,
                avklaringsbehov = listOf(hendelse.behov())
            )
        } else {
            kontroller.prosesserBehandling(kontekst)
        }
    }
}
