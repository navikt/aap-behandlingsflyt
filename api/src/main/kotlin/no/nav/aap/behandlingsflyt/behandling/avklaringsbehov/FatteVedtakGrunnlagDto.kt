package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.flate.Historikk
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.vedtak.TotrinnsVurdering

data class FatteVedtakGrunnlagDto(
    val harTilgangTilÅSaksbehandle: Boolean,
    val vurderinger: List<TotrinnsVurdering>,
    val historikk: List<Historikk>
)
