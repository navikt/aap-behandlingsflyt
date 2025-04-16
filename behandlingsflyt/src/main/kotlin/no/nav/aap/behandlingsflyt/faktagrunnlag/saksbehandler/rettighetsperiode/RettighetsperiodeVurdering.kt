package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.rettighetsperiode

import java.time.LocalDate

data class RettighetsperiodeVurdering(
    val startDato: LocalDate,
    val begrunnelse: String,
    val årsak: RettighetsperiodeEndringsårsak
)

enum class RettighetsperiodeEndringsårsak {
    ANNEN,
    SØKT_PÅ_ANNEN_MÅTE
}
