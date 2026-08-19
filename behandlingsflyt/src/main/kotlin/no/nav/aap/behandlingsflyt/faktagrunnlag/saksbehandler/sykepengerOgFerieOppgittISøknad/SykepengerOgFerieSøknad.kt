package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykepengerOgFerieOppgittISøknad

import no.nav.aap.komponenter.type.Periode

data class SykepengerOgFerieSøknad(
    val mottarSykepenger: Boolean,
    val feriePerioder: List<Periode>,
    val ferieDager: Int?,
)
