package no.nav.aap.behandlingsflyt.behandling.stønadsperiode

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode.RelevantKravType
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode.StønadsperiodeVurdering
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.komponenter.verdityper.Bruker
import java.time.Instant

data class StønadsperiodeGrunnlagDto(
    val harTilgangTilÅSaksbehandle: Boolean,
    val nyeVurderinger: List<StønadsperiodeVurderingDto>,
    val vedtatteVurderinger: List<StønadsperiodeVurderingDto>
)

data class StønadsperiodeVurderingDto(
    val referanse: String,
    val begrunnelse: String,
    val vurdertAv: Bruker,
    val vurdertIBehandling: BehandlingId,
    val opprettet: Instant,

    val harHattOrdinærSiste52Uker: Boolean,
    val harGjenværendeKvote: Boolean,
    val relevantKravType: RelevantKravType,
)

fun StønadsperiodeVurdering.somDto() = StønadsperiodeVurderingDto(
    referanse = referanse.verdi.toString(),
    begrunnelse = begrunnelse,
    harHattOrdinærSiste52Uker = harHattOrdinærSiste52Uker,
    harGjenværendeKvote = harGjenværendeKvote,
    relevantKravType = relevantKravType,
    vurdertAv = vurdertAv,
    vurdertIBehandling = vurdertIBehandling,
    opprettet = opprettet,
    
)
    