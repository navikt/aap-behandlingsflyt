package no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.sykdom.bistand

import no.nav.aap.behandlingsflyt.PeriodiserteVurderingerDto
import no.nav.aap.komponenter.type.Periode

data class BistandGrunnlagResponse(
    override val harTilgangTilÅSaksbehandle: Boolean,
    override val nyeVurderinger: List<BistandVurderingResponse>,
    override val sisteVedtatteVurderinger: List<BistandVurderingResponse>,
    override val behøverVurderinger: List<Periode>,
    override val ikkeRelevantePerioder: List<Periode>,
    override val kanVurderes: List<Periode>,
    ) : PeriodiserteVurderingerDto<BistandVurderingResponse>