package no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.fritakmeldeplikt

import no.nav.aap.behandlingsflyt.PeriodiserteVurderingerDto
import no.nav.aap.behandlingsflyt.VurderingDto
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate
import java.time.LocalDateTime

data class FritakMeldepliktGrunnlagResponse(
    override val harTilgangTilÅSaksbehandle: Boolean,
    val historikk: Set<FritakMeldepliktVurderingResponse>,
    val gjeldendeVedtatteVurderinger: List<FritakMeldepliktVurderingResponse>,
    val vurderinger: List<FritakMeldepliktVurderingResponse>,

    override val sisteVedtatteVurderinger: List<PeriodisertFritakMeldepliktVurderingResponse>,
    override val nyeVurderinger: List<PeriodisertFritakMeldepliktVurderingResponse>,
    override val kanVurderes: List<Periode>,
    override val behøverVurderinger: List<Periode>,
): PeriodiserteVurderingerDto<PeriodisertFritakMeldepliktVurderingResponse>

data class FritakMeldepliktVurderingResponse(
    val begrunnelse: String,
    val vurderingsTidspunkt: LocalDateTime,
    val harFritak: Boolean,
    val fraDato: LocalDate,
    val vurdertAv: VurdertAvResponse
)

data class PeriodisertFritakMeldepliktVurderingResponse(
    override val fom: LocalDate,
    override val tom: LocalDate?,
    override val vurdertAv: VurdertAvResponse?,
    override val kvalitetssikretAv: VurdertAvResponse? = null,
    override val besluttetAv: VurdertAvResponse? = null,
    val begrunnelse: String,
    val harFritak: Boolean,
): VurderingDto