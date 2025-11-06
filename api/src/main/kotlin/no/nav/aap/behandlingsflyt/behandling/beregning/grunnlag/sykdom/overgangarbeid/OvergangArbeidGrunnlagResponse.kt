package no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.sykdom.overgangarbeid

import no.nav.aap.behandlingsflyt.PeriodiserteVurderingerDto
import no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.sykdom.sykdom.SykdomsvurderingResponse
import no.nav.aap.komponenter.type.Periode

data class OvergangArbeidGrunnlagResponse(
    override val harTilgangTilÅSaksbehandle: Boolean,
    override val sisteVedtatteVurderinger: List<OvergangArbeidVurderingResponse>,
    override val nyeVurderinger: List<OvergangArbeidVurderingResponse>,
    override val kanVurderes: List<Periode>,
    override val behøverVurderinger: List<Periode>,

    val gjeldendeSykdsomsvurderinger: List<SykdomsvurderingResponse>,
): PeriodiserteVurderingerDto<OvergangArbeidVurderingResponse>
