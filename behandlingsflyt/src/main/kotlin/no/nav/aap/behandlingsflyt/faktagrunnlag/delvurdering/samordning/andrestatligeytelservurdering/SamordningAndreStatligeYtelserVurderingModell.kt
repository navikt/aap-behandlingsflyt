package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering

import no.nav.aap.komponenter.type.Periode
import java.time.LocalDateTime

data class SamordningAndreStatligeYtelserGrunnlag(
    val vurdering: SamordningAndreStatligeYtelserVurdering,
)

data class SamordningAndreStatligeYtelserVurdering(
    val begrunnelse: String,
    val vurdertAv: String,
    val vurdertTidspunkt: LocalDateTime? = null,
    val vurderingPerioder: List<SamordningAndreStatligeYtelserVurderingPeriode>,
)

data class SamordningAndreStatligeYtelserVurderingPeriode(
    val ytelse: AndreStatligeYtelser,
    val periode: Periode,
    val beløp: Int
)

data class SamordningAndreStatligeYtelserVurderingDto(
    val begrunnelse: String,
    val vurderingPerioder: List<SamordningAndreStatligeYtelserVurderingPeriodeDto>,
)

data class SamordningAndreStatligeYtelserVurderingPeriodeDto(
    val ytelse: AndreStatligeYtelser,
    val periode: Periode,
    val beløp: Int
)

enum class AndreStatligeYtelser {
    SYKEPENGER,
    TILTAKSPENGER,
    OMSTILLINGSSTØNAD,
    OVERGANGSSTØNAD,
    DAGPENGER,
    BARNEPENSJON,
}
