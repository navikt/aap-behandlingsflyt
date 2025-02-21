package no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap

import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.EØSLand

data class ManuellVurderingForLovvalgMedlemskap (
    val lovvalgVedSøknadsTidspunkt: LovvalgVedSøknadsTidspunkt,
    val medlemskapVedSøknadsTidspunkt: MedlemskapVedSøknadsTidspunkt?,
    val overstyrt: Boolean
)

data class LovvalgVedSøknadsTidspunkt(
    val begrunnelse: String,
    val lovvalgsEØSLand: EØSLand?,
)

data class MedlemskapVedSøknadsTidspunkt(
    val begrunnelse: String?,
    val varMedlemIFolketrygd: Boolean?
)