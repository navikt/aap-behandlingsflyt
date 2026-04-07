package no.nav.aap.behandlingsflyt.behandling.vilkår.inntektsbortfall

import no.nav.aap.komponenter.verdityper.GUnit

data class InntektsbortfallKanBehandlesAutomatisk(
    val kanBehandlesAutomatisk: Boolean,
    val inntektSisteÅrOver1G: InntektSisteÅrOver1G,
    val inntektSiste3ÅrOver3G: InntektSiste3ÅrOver3G,
    val under62ÅrVedSøknadstidspunkt: Under62ÅrVedSøknadstidspunkt,
)

data class InntektSisteÅrOver1G(
    val gverdi: GUnit,
    val resultat: Boolean
)

data class InntektSiste3ÅrOver3G(
    val gverdi: GUnit,
    val resultat: Boolean
)

data class Under62ÅrVedSøknadstidspunkt(
    val alder: Int,
    val resultat: Boolean
)