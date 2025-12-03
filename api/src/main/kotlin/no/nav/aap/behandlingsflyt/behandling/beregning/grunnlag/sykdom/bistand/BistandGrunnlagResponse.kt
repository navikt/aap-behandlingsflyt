package no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.sykdom.bistand

import no.nav.aap.behandlingsflyt.PeriodiserteVurderingerDto
import no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.sykdom.sykdom.SykdomsvurderingResponse
import no.nav.aap.behandlingsflyt.behandling.vurdering.VurdertAvResponse
import no.nav.aap.komponenter.type.Periode

data class BistandGrunnlagResponse(
    override val harTilgangTilÅSaksbehandle: Boolean,
    @Deprecated("Erstattes av nyeVurderinger")
    val vurderinger: List<BistandVurderingResponse>,
    override val nyeVurderinger: List<BistandVurderingResponse>,
    @Deprecated("Erstattes av sisteVedtatteVurderinger")
    val gjeldendeVedtatteVurderinger: List<BistandVurderingResponse>,
    override val sisteVedtatteVurderinger: List<BistandVurderingResponse>,
    val historiskeVurderinger: List<BistandVurderingResponse>,
    val gjeldendeSykdsomsvurderinger: List<SykdomsvurderingResponse>,
    val harOppfylt11_5: Boolean?, // Slettes når 11-17 er prodsatt
    @Deprecated("Ligger på vurderingsnivå")
    val kvalitetssikretAv: VurdertAvResponse?,
    override val kanVurderes: List<Periode>,
    override val behøverVurderinger: List<Periode>
) : PeriodiserteVurderingerDto<BistandVurderingResponse> 