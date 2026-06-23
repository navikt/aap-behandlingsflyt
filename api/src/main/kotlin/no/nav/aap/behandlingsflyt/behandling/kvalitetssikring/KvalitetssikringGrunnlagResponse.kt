package no.nav.aap.behandlingsflyt.behandling.kvalitetssikring

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.flate.Historikk
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.ÅrsakTilRetur
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.oppgave.markering.MarkeringDto

data class KvalitetssikringGrunnlagResponse(
    val harTilgangTilÅSaksbehandle: Boolean,
    val harGjortVilkårsvurderingerPåBehandling: Boolean,
    val vurderinger: List<TotrinnsVurderingResponse>,
    val historikk: List<Historikk>
)

data class TotrinnsVurderingResponse(
    val definisjon: AvklaringsbehovKode,
    val godkjent: Boolean?,
    val begrunnelse: String?,
    val endretSidenSist: Boolean?,
    val grunner: List<ÅrsakTilRetur>?,
    val markeringer: List<MarkeringDto>?,
)