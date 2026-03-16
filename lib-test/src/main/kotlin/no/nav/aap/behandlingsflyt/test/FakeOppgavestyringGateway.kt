package no.nav.aap.behandlingsflyt.test

import no.nav.aap.behandlingsflyt.hendelse.oppgavestyring.OppgavestyringGateway
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.BehandlingFlytStoppetHendelse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.TilbakekrevingsbehandlingOppdatertHendelse
import no.nav.aap.behandlingsflyt.kontrakt.oppgave.EnhetNrDto
import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.oppgave.enhet.OppgaveEnhetResponse

class FakeOppgavestyringGateway : OppgavestyringGateway {
    override fun varsleHendelse(hendelse: BehandlingFlytStoppetHendelse) {
        /**
         * Noop
         */
    }

    override fun varsleTilbakekrevingHendelse(hendelse: TilbakekrevingsbehandlingOppdatertHendelse) {
        /**
         * Noop
         */
    }

    override fun finnNayEnhetForPerson(
        personIdent: String,
        relevanteIdenter: List<String>
    ): EnhetNrDto {
        return EnhetNrDto("1234")
    }

    override fun hentOppgaveEnhet(behandlingReferanse: BehandlingReferanse): OppgaveEnhetResponse {
        return OppgaveEnhetResponse(emptyList())
    }

    companion object : Factory<OppgavestyringGateway> {
        override fun konstruer(): OppgavestyringGateway = FakeOppgavestyringGateway()
    }

}