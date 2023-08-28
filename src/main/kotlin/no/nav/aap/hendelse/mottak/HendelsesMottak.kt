package no.nav.aap.hendelse.mottak

import no.nav.aap.domene.behandling.BehandlingTjeneste
import no.nav.aap.domene.person.Personlager
import no.nav.aap.domene.sak.Sakslager
import no.nav.aap.domene.typer.Ident
import no.nav.aap.domene.typer.Saksnummer
import no.nav.aap.flyt.kontroll.FlytKontekst
import no.nav.aap.flyt.kontroll.FlytKontroller
import no.nav.aap.flyt.kontroll.ValiderBehandlingTilstand

object HendelsesMottak {

    private val kontroller = FlytKontroller()

    fun håndtere(key: Ident, hendelse: PersonHendelse) {
        val person = Personlager.finnEllerOpprett(key)

        val sak = Sakslager.finnEllerOpprett(person, hendelse.periode())

        // Legg til kø for sak, men mocker ved å kalle videre bare

        håndtere(sak.saksnummer, hendelse.tilSakshendelse())
    }

    fun håndtere(key: Saksnummer, hendelse: SakHendelse) {
        val sak = Sakslager.hent(key)
        val sisteBehandlingOpt = BehandlingTjeneste.finnSisteBehandlingFor(sak.id)

        val sisteBehandling = if (sisteBehandlingOpt != null && !sisteBehandlingOpt.status().erAvsluttet()) {
            sisteBehandlingOpt
        } else {
            // Har ikke behandling så oppretter en
            BehandlingTjeneste.opprettBehandling(sak.id)
        }
        håndtere(key = sisteBehandling.id, hendelse.tilBehandlingHendelse())
    }

    fun håndtere(key: Long, hendelse: LøsAvklaringsbehovBehandlingHendelse) {
        val behandling = BehandlingTjeneste.hent(key)
        ValiderBehandlingTilstand.validerTilstandBehandling(behandling = behandling)

        val sak = Sakslager.hent(behandling.sakId)

        val kontekst = FlytKontekst(sakId = sak.id, behandlingId = behandling.id)
        kontroller.løsAvklaringsbehovOgFortsettProsessering(
            kontekst = kontekst,
            avklaringsbehov = listOf(hendelse.behov())
        )
    }

    fun håndtere(key: Long, hendelse: BehandlingHendelse) {
        val behandling = BehandlingTjeneste.hent(key)
        ValiderBehandlingTilstand.validerTilstandBehandling(behandling = behandling)

        val sak = Sakslager.hent(behandling.sakId)

        val kontekst = FlytKontekst(sakId = sak.id, behandlingId = behandling.id)
        if (hendelse is LøsAvklaringsbehovBehandlingHendelse) {
            throw IllegalArgumentException("Skal håndteres mellom eksplisitt funksjon")
        } else {
            kontroller.prosesserBehandling(kontekst)
        }
    }
}
