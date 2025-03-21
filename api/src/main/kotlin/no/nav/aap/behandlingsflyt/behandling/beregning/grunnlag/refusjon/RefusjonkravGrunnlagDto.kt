package no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.refusjon

import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate

data class RefusjonkravGrunnlagDto (
    val vurderinger: List<RefusjonkravVurderingDto>
)

data class RefusjonkravVurderingDto (
    val harKrav: Boolean,
    val periode: Periode,
    val vurdertAvIdent: String,
    val vurdertDato: LocalDate
)