package no.nav.aap.behandlingsflyt.hendelse.oppgavestyring

import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.BehandlingFlytStoppetHendelse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.TilbakekrevingsbehandlingOppdatertHendelse
import no.nav.aap.behandlingsflyt.kontrakt.oppgave.EnhetNrDto
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.gateway.Gateway
import java.time.LocalDateTime

// TODO: Hvor skal denne leve og hva skal den hete?
data class MarkeringNyDto(
    val behandlingRef: BehandlingReferanse,
    val markeringType: MarkeringForBehandling,
    val begrunnelse: String? = null,
    val opprettetAv: String? = null,
    val opprettetTidspunkt: LocalDateTime,
    val opprettetAvNavn: String? = null,
    val hendelseType: MarkeringHendelseType? = null,
)

// TODO: Hvor skal denne leve og hva skal den hete?
enum class MarkeringHendelseType {
    OPPRETTET,
    FJERNET
}

enum class MarkeringForBehandling {
    HASTER,
    AVSLAG_11_5
}

interface OppgavestyringGateway : Gateway {
    fun varsleHendelse(hendelse: BehandlingFlytStoppetHendelse)
    fun varsleTilbakekrevingHendelse(hendelse: TilbakekrevingsbehandlingOppdatertHendelse)
    fun finnNayEnhetForPerson(personIdent: String, relevanteIdenter: List<String>): EnhetNrDto
    fun hentOppgaveEnhet(behandlingReferanse: BehandlingReferanse): OppgaveEnhetResponse
    fun hentMarkeringerOgHistorikk(saksnummer: Saksnummer): List<MarkeringNyDto>
}