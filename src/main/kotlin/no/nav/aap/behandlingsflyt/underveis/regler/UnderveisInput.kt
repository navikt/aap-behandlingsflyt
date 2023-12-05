package no.nav.aap.behandlingsflyt.underveis.regler

import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkår

data class UnderveisInput(val relevanteVilkår: List<Vilkår>)
