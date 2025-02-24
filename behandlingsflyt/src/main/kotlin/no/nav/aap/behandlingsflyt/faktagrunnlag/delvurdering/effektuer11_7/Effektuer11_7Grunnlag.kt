package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.effektuer11_7

import no.nav.aap.behandlingsflyt.behandling.brev.bestilling.BrevbestillingReferanse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import java.time.LocalDate

data class Effektuer11_7Grunnlag(
    val vurdering: Effektuer11_7Vurdering?,
    val varslinger: List<Effektuer11_7Forhåndsvarsel>
)

data class Effektuer11_7Vurdering(
    val begrunnelse: String,
)

data class Effektuer11_7Forhåndsvarsel(
    val referanse: BrevbestillingReferanse?,
    val datoVarslet: LocalDate,
    val frist: LocalDate?,
    val underveisperioder: List<Underveisperiode>
)