package no.nav.aap.behandlingsflyt.hendelse.oppgavestyring

import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.BehandlingFlytStoppetHendelse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.TilbakekrevingsbehandlingOppdatertHendelse
import no.nav.aap.behandlingsflyt.kontrakt.oppgave.EnhetNrDto
import no.nav.aap.komponenter.gateway.Gateway
import no.nav.aap.oppgave.enhet.OppgaveEnhetResponse

interface OppgavestyringGateway : Gateway {
    fun varsleHendelse(hendelse: BehandlingFlytStoppetHendelse)
    fun varsleTilbakekrevingHendelse(hendelse: TilbakekrevingsbehandlingOppdatertHendelse)
    fun finnNayEnhetForPerson(personIdent: String, relevanteIdenter: List<String>): EnhetNrDto
    fun hentOppgaveEnhet(behandlingReferanse: BehandlingReferanse): OppgaveEnhetResponse
}