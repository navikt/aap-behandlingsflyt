package no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap

import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.EØSLand
import java.time.LocalDate

data class ManuellVurderingForLovvalgMedlemskap (
    val lovvalgVedSøknadsTidspunkt: LovvalgVedSøknadsTidspunkt,
    val medlemskapVedSøknadsTidspunkt: MedlemskapVedSøknadsTidspunkt?,
    val overstyrt: Boolean = false
)

data class ManuellVurderingForLovvalgMedlemskapDto (
    val lovvalgVedSøknadsTidspunkt: LovvalgVedSøknadsTidspunkt,
    val medlemskapVedSøknadsTidspunkt: MedlemskapVedSøknadsTidspunkt?
)

data class HistoriskManuellVurderingForLovvalgMedlemskap (
    val manuellVurdering: ManuellVurderingForLovvalgMedlemskap,
    val opprettet: LocalDate
)

data class LovvalgVedSøknadsTidspunkt(
    val begrunnelse: String,
    val lovvalgsEØSLand: EØSLand?,
)

data class MedlemskapVedSøknadsTidspunkt(
    val begrunnelse: String?,
    val varMedlemIFolketrygd: Boolean?
)