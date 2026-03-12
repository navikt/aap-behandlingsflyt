package no.nav.aap.behandlingsflyt.behandling.grunnlag.samordning

import no.nav.aap.behandlingsflyt.behandling.samordning.EndringStatus
import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.andrestatligeytelservurdering.AndreStatligeYtelser
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.TjenestePensjonForhold
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.TjenestePensjonOrdning
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.tjenestepensjon.YtelseTypeCode
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.refusjonskrav.TjenestepensjonRefusjonskravVurdering
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate

/**
 * @param ytelser Hvilke ytelser det er funnet på denne personen.
 * @param vurdering Manuelle vurderinger gjort av saksbehandler for gitte ytelser.
 */
data class SamordningYtelseVurderingGrunnlagDTO(
    val harTilgangTilÅSaksbehandle: Boolean,
    val ytelser: List<SamordningYtelseDTO>,
    val vurdering: SamordningYtelseVurderingDTO?,
    val historiskeVurderinger: List<SamordningYtelseVurderingDTO>,
    val tpYtelser: List<TjenestePensjonForhold>?,
)

data class SamordningYtelseVurderingDTO(
    val begrunnelse: String?,
    val vurderinger: List<SamordningVurderingDTO>,
    val vurdertAv: VurdertAvResponse?
)

data class SamordningYtelseDTO(
    val ytelseType: Ytelse,
    val periode: Periode,
    val gradering: Int?,
    val kronesum: Int?,
    val kilde: String,
    val saksRef: String?,
    val endringStatus: EndringStatus
)

data class SamordningVurderingDTO(
    val ytelseType: Ytelse,
    val periode: Periode,
    val gradering: Int?,
    val kronesum: Int?,
    val manuell: Boolean?
)

data class SamordningUføreVurderingGrunnlagDTO(
    val harTilgangTilÅSaksbehandle: Boolean,
    val vurdering: SamordningUføreVurderingDTO?,
    val grunnlag: List<SamordningUføreGrunnlagDTO>
)

/**
 * @param kilde Alltid lik PESYS.
 */
data class SamordningUføreGrunnlagDTO(
    val virkningstidspunkt: LocalDate,
    val uføregrad: Int,
    val endringStatus: EndringStatus
) {
    val kilde = "PESYS"
}

data class SamordningUføreVurderingDTO(
    val begrunnelse: String,
    val vurderingPerioder: List<SamordningUføreVurderingPeriodeDTO>,
    val vurdertAv: VurdertAvResponse
)

data class SamordningUføreVurderingPeriodeDTO(
    val virkningstidspunkt: LocalDate,
    val uføregradTilSamordning: Int
)

data class SamordningAndreStatligeYtelserGrunnlagDTO(
    val harTilgangTilÅSaksbehandle: Boolean,
    val vurdering: SamordningAndreStatligeYtelserVurderingDTO?,
    val historiskeVurderinger: List<SamordningAndreStatligeYtelserVurderingDTO>? = emptyList(),
    val perioder: List<AndreStatligeYtelserPeriodeDto>? = emptyList(),
)

data class SamordningAndreStatligeYtelserVurderingDTO(
    val begrunnelse: String,
    val vurderingPerioder: List<SamordningAndreStatligeYtelserVurderingPeriodeDTO>,
    val vurdertAv: VurdertAvResponse?
)

data class SamordningAndreStatligeYtelserVurderingPeriodeDTO(
    val periode: Periode,
    val ytelse: AndreStatligeYtelser,
)

data class SamordningArbeidsgiverGrunnlagDTO(
    val harTilgangTilÅSaksbehandle: Boolean,
    val vurdering: SamordningArbeidsgiverVurderingDTO?,
    val historiskeVurderinger: List<SamordningArbeidsgiverVurderingDTO>? = emptyList(),
    val harFåttEkstrautbetalingFraArbeidsgiver: Boolean? = null
)

data class SamordningArbeidsgiverVurderingDTO(
    val begrunnelse: String,
    val perioder: List<Periode>,
    val vurdertAv: VurdertAvResponse?
)

data class TjenestepensjonGrunnlagDTO(
    val harTilgangTilÅSaksbehandle: Boolean,
    val tjenestepensjonYtelser: List<TjenestepensjonYtelseDTO>,
    val tjenestepensjonRefusjonskravVurdering: TjenestepensjonRefusjonskravVurdering? = null
)

data class TjenestepensjonYtelseDTO(
    val ytelseIverksattFom: LocalDate,
    val ytelseIverksattTom: LocalDate?,
    val ytelse: YtelseTypeCode,
    val ordning: TjenestePensjonOrdning
)

data class AndreStatligeYtelserGrunnlagDto (
    val perioder: List<AndreStatligeYtelserPeriodeDto> = emptyList()
)

data class AndreStatligeYtelserPeriodeDto (
    val fom: LocalDate,
    val tom: LocalDate,
    val kilde: AndreStatligeYtelserKilde,
    val ytelseType: AndreStatligeYtelserType
)

enum class AndreStatligeYtelserKilde {
    ARENA, DP_SAK
}

enum class AndreStatligeYtelserType {
    DAGPENGER_ARBEIDSSOKER_ORDINAER, DAGPENGER_PERMITTERING_ORDINAER, DAGPENGER_PERMITTERING_FISKEINDUSTRI
}