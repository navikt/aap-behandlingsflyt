package no.nav.aap.behandlingsflyt.behandling.lovvalgmedlemskap.grunnlag

import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.HistoriskManuellVurderingForLovvalgMedlemskap
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForLovvalgMedlemskap

data class LovvalgMedlemskapGrunnlagDto (
    val vurdering: ManuellVurderingForLovvalgMedlemskap?,
    val historiskeManuelleVurderinger: List<HistoriskManuellVurderingForLovvalgMedlemskap>
)