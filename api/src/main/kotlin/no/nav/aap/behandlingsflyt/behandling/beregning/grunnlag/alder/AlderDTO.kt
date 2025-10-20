package no.nav.aap.behandlingsflyt.behandling.beregning.grunnlag.alder

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import java.time.LocalDate

data class AlderDTO (
    val fødselsdato: LocalDate? = null,
    val vilkårsperioder: List<Vilkårsperiode>,
    val vurdertDato: LocalDate? = null
)