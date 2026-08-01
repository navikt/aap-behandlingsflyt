package no.nav.aap.behandlingsflyt.behandling.`foreslåvedtak`

import no.nav.aap.vilkårsresultat.Avslagsårsak
import java.time.LocalDate
import java.util.UUID

data class StansOpphørDto(
    val stansOpphørFraOgMed: LocalDate,

    /* Sortert, med nyeste element først. */
    val historikk: List<StansOpphørVurderingDto>,
)

data class StansOpphørVurderingDto(
    val type: StansOpphørVurderingTypeDto,
    val årsaker: List<Avslagsårsak>,
    val behandlingReferanse: UUID,
)

enum class StansOpphørVurderingTypeDto {
    STANS, OPPHØR, OPPHEVET
}




