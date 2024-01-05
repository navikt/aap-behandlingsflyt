package no.nav.aap.behandlingsflyt.underveis.regler

import no.nav.aap.behandlingsflyt.verdityper.Periode
import no.nav.aap.behandlingsflyt.faktagrunnlag.arbeid.Pliktkort
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkår
import java.time.LocalDate

data class UnderveisInput(
    val førsteFastsatteDag: LocalDate,
    val relevanteVilkår: List<Vilkår>,
    val opptrappingPerioder: List<Periode>,
    val pliktkort: List<Pliktkort>
)
