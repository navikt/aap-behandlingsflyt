package no.nav.aap.behandlingsflyt.underveis.regler

import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkår
import java.time.LocalDate

data class UnderveisInput(val førsteFastsatteDag: LocalDate, val relevanteVilkår: List<Vilkår>)
