package no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap

import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.EØSLand

data class VurderingerForLovvalgMedlemskap (
    val lovvalgVedSøknadsTidspunkt: LovvalgVedSøknadsTidspunkt,
    val medlemskapVedSøknadsTidspunkt: MedlemskapVedSøknadsTidspunkt?
)

data class LovvalgVedSøknadsTidspunkt(
    val tekstVurderinger: String,
    val lovvalgsEØSLand: EØSLand,
)

data class MedlemskapVedSøknadsTidspunkt(
    val tekstVurdering: String,
    val varMedlemIFolketrygd: Boolean
)