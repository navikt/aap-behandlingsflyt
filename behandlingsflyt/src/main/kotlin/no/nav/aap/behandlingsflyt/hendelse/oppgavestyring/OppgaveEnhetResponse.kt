package no.nav.aap.behandlingsflyt.hendelse.oppgavestyring

data class OppgaveEnhetResponse(val oppgaver: List<OppgaveEnhet>)

data class OppgaveEnhet(
    val avklaringsbehovKode: String,
    val enhet: String
)