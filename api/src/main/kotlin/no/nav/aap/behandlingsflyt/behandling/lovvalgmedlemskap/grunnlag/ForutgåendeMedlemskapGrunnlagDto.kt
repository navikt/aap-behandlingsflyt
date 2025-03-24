package no.nav.aap.behandlingsflyt.behandling.lovvalgmedlemskap.grunnlag

import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.HistoriskManuellVurderingForForutgåendeMedlemskap
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForForutgåendeMedlemskap
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.tilgang.TilgangGatewayImpl
import no.nav.aap.komponenter.httpklient.auth.token

data class ForutgåendeMedlemskapGrunnlagDto (
    val harTilgangTilÅSaksbehandle: Boolean,
    val vurdering: ManuellVurderingForForutgåendeMedlemskap?,
    val historiskeManuelleVurderinger: List<HistoriskManuellVurderingForForutgåendeMedlemskap>
)