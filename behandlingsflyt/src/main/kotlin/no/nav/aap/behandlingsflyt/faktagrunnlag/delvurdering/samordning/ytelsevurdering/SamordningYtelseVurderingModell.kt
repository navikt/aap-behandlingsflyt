package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.ytelsevurdering

import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent
import java.time.LocalDate

data class SamordningYtelse(
    val ytelseType: Ytelse,
    val ytelsePerioder: List<SamordningYtelsePeriode>,
    val kilde: String,
    val saksRef: String? = null,
)

data class SamordningYtelsePeriode(
    val periode: Periode,
    val gradering: Prosent?,
    val kronesum: Number? = null
)

data class SamordningVurdering(
    val ytelseType: Ytelse,
    val begrunnelse: String,
    val maksDatoEndelig: Boolean,
    val maksDato: LocalDate?,
    val vurderingPerioder: List<SamordningVurderingPeriode>,
)

data class SamordningVurderingPeriode(
    val periode: Periode,
    val gradering: Prosent?,
    val kronesum: Number? = null
)

data class SamordningYtelseVurderingGrunnlag(
    @Deprecated("Denne er alltid null.")
    val ytelseGrunnlag: SamordningYtelseGrunnlag?,
    val vurderingGrunnlag: SamordningVurderingGrunnlag?
) : Faktagrunnlag

data class SamordningYtelseGrunnlag(
    val grunnlagId: Long,
    val ytelser: List<SamordningYtelse>,
)

data class SamordningVurderingGrunnlag(
    val vurderingerId: Long?,
    val vurderinger: List<SamordningVurdering>,
)