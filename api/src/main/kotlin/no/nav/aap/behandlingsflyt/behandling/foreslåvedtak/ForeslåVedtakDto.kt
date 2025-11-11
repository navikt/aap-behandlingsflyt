package no.nav.aap.behandlingsflyt.behandling.foreslåvedtak

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.komponenter.type.Periode

data class ForeslåVedtakDto(
    val periode: Periode,
    val utfall: Utfall,
    val rettighetsType: RettighetsType?,
    val avslagsårsak: AvslagsårsakDto
)

data class AvslagsårsakDto(
    val vilkårsavslag: List<String> = emptyList(),
    val underveisavslag: UnderveisÅrsak? = null
)

data class UnderveisPeriodeInfo(
    val periode: Periode,
    val utfall: Utfall,
    val rettighetsType: RettighetsType?,
    val underveisÅrsak: UnderveisÅrsak?
) {
    companion object {
        fun UnderveisPeriodeInfo.tilForeslåVedtakData(): ForeslåVedtakData {
            return ForeslåVedtakData(
                this.utfall,
                this.rettighetsType,
                this.underveisÅrsak
            )
        }
    }
}

data class ForeslåVedtakData(
    val utfall: Utfall,
    val rettighetsType: RettighetsType?,
    val underveisÅrsak: UnderveisÅrsak?
)