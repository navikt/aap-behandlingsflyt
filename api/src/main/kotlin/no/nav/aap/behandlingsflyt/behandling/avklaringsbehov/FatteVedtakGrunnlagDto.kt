package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.flate.Historikk
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.vedtak.TotrinnsVurdering
import java.time.LocalDateTime

data class FatteVedtakGrunnlagDto(
    val harTilgangTilÅSaksbehandle: Boolean,
    val vurderinger: List<TotrinnsVurdering>,
    val historikk: List<Historikk>,
    val besluttetAv: BeslutterDto?
)

data class BeslutterDto(
    val ident: String,
    val navn: String,
    val kontor: String,
    val tidspunkt: LocalDateTime,
)