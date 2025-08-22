package no.nav.aap.behandlingsflyt.behandling.foreslåvedtak

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.komponenter.type.Periode

data class ForeslåVedtakDto(
    val periode: Periode?,
    val utfall: Utfall,
    val rettighetsType: RettighetsType?
) {
    companion object {
        fun ForeslåVedtakDto.tilForeslåVedtakData(): ForeslåVedtakData {
            return ForeslåVedtakData(
                this.utfall,
                this.rettighetsType
            )
        }
    }
}

data class ForeslåVedtakData(
    val utfall: Utfall,
    val rettighetsType: RettighetsType?
)