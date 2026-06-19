package no.nav.aap.behandlingsflyt.hendelse.oppgavestyring

import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.BehandlingFlytStoppetHendelse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.TilbakekrevingsbehandlingOppdatertHendelse
import no.nav.aap.behandlingsflyt.kontrakt.oppgave.EnhetNrDto
import no.nav.aap.komponenter.gateway.Gateway
import no.nav.aap.oppgave.enhet.OppgaveEnhetResponse
import no.nav.aap.oppgave.verdityper.MarkeringForBehandling
import java.time.LocalDateTime

// TODO: Hent fra oppgave-lib
data class MarkeringNyDto(
    val markeringType: MarkeringForBehandling,
    val begrunnelse: String? = null,
    val opprettetAv: String? = null,
    val opprettetTidspunkt: LocalDateTime,
    val opprettetAvNavn: String? = null,
    val hendelseType: MarkeringHendelseType? = null,
)

// TODO: Hent fra oppgave-lib
enum class MarkeringHendelseType {
    OPPRETTET,
    FJERNET
}

interface OppgavestyringGateway : Gateway {
    fun varsleHendelse(hendelse: BehandlingFlytStoppetHendelse)
    fun varsleTilbakekrevingHendelse(hendelse: TilbakekrevingsbehandlingOppdatertHendelse)
    fun finnNayEnhetForPerson(personIdent: String, relevanteIdenter: List<String>): EnhetNrDto
    fun hentOppgaveEnhet(behandlingReferanse: BehandlingReferanse): OppgaveEnhetResponse
    fun hentMarkeringerOgHistorikk(behandlingReferanse: BehandlingReferanse): List<MarkeringNyDto>
}