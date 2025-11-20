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
    val historiskePerioder: List<SamordningAndreStatligeYtelserVurderingPeriode>? = null,
)

data class SamordningAndreStatligeYtelserVurderingPeriode(
    val ytelse: AndreStatligeYtelser,
    val periode: Periode,
)

data class SamordningAndreStatligeYtelserVurderingDto(
    val begrunnelse: String,
    val vurderingPerioder: List<SamordningAndreStatligeYtelserVurderingPeriodeDto>,
)

data class SamordningAndreStatligeYtelserVurderingPeriodeDto(
    val ytelse: AndreStatligeYtelser,
    val periode: Periode,
)

enum class AndreStatligeYtelser {
    SYKEPENGER,
    TILTAKSPENGER,
    OMSTILLINGSSTØNAD,
    OVERGANGSSTØNAD,
    DAGPENGER,
    BARNEPENSJON,
    /**
     * Da etterlattereformen trådde i kraft 1. januar 2024 ble det satt en tidsbegrensning for en del av de som mottok
     * gjenlevendepensjon på 3 år (eventuelt +2 år). Fra 1. januar 2029 skal alle de gjenværende på gjenlevendepensjon
     * over på omstillingsstønad, som har en litt annen beregning. 
     **/
    GJENLEVENDEPENSJON,
}
