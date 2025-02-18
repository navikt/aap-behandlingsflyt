package no.nav.aap.behandlingsflyt.behandling.lovvalgmedlemskap.grunnlag

import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForForutgåendeMedlemskap

data class ForutgåendeMedlemskapGrunnlagDto (
    val vurdering: ManuellVurderingForForutgåendeMedlemskap?
)