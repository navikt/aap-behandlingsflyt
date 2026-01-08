package no.nav.aap.behandlingsflyt.behandling.vilkår.inntektsbortfall

import java.math.BigDecimal

data class InntektsbortfallKanBehandlesAutomatisk(
    val kanBehandlesAutomatisk: Boolean,
    val inntektSisteÅrOver1G: InntektSisteÅrOver1G,
    val gjennomsnittInntektSiste3ÅrOver3G: GjennomsnittInntektSiste3ÅrOver3G,
    val under62ÅrVedSøknadstidspunkt: Under62ÅrVedSøknadstidspunkt,
)

data class InntektSisteÅrOver1G(
    val gverdi: BigDecimal,
    val resultat: Boolean
)

data class GjennomsnittInntektSiste3ÅrOver3G(
    val gverdi: BigDecimal,
    val resultat: Boolean
)

data class Under62ÅrVedSøknadstidspunkt(
    val alder: Int,
    val resultat: Boolean
)