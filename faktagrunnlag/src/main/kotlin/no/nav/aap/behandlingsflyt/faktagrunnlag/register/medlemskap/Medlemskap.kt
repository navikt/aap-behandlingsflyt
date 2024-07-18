package no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap

import java.time.LocalDate

data class Medlemskap(val unntak: List<Unntak>)
data class Unntak(
    val unntakId: Number,
    val ident: String,
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
    val status: String,
    val statusaarsak: String?,
    val medlem: Boolean,
    val grunnlag: String,
    val lovvalg: String,
)