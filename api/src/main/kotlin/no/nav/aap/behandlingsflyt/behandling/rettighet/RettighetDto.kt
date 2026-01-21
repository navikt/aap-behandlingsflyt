package no.nav.aap.behandlingsflyt.behandling.rettighet

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import java.time.LocalDate

data class RettighetDto(
    val type: RettighetsType,
    val kvote: Int? = null,
    val bruktKvote: Int? = null,
    var gjenværendeKvote: Int? = null,
    val startdato: LocalDate? = null,
    val maksDato: LocalDate? = null,
    val stansdato: LocalDate? = null,
    val opphørsdato: LocalDate? = null,
)
