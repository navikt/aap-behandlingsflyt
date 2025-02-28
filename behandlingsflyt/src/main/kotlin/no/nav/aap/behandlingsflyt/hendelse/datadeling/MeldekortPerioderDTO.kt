package no.nav.aap.behandlingsflyt.hendelse.datadeling

import no.nav.aap.komponenter.type.Periode

data class MeldekortPerioderDTO (
    val personIdent: String,
    val meldekortPerioder: List<Periode>
)

