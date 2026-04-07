package no.nav.aap.behandlingsflyt.behandling.`vilkår`.inntektsbortfall

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.Grunnbeløp
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.mars
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.GUnit
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.Year

class InntektsbortfallVurderingServiceTest {
    val rettighetsperiode = Periode(1 januar 2026, 31 desember 2026)

    val service = InntektsbortfallVurderingService(
        setOf(Year.of(2023), Year.of(2024), Year.of(2025)), rettighetsperiode
    )

    @Test
    fun `om søker er under 62 på kravtidspunkt, kan vilkåret behandles automatisk`() {
        val resultat = service.vurderInntektsbortfall(
            // Under 62 år
            fødselsdato = Fødselsdato(13 mars 1990),
            inntektPerÅr = setOf()
        )

        assertThat(resultat.kanBehandlesAutomatisk).isTrue()
    }

    @ParameterizedTest
    @CsvSource(
        // Inntekt i G, Forventet resultat
        "    0.5,         false",
        "    1.5,         true"
    )
    fun `om søker har hatt inntekt på over 1G siste år, kan vilkåret behandles automatisk`(
        inntektIG: Double,
        kanBehandlesAutomatisk: Boolean
    ) {
        val resultat = service.vurderInntektsbortfall(
            fødselsdato = Fødselsdato(13 mars 1963),
            inntektPerÅr = setOf(
                InntektPerÅr(
                    Year.of(2023), Grunnbeløp.finnGrunnbeløp(1 januar 2025).multiplisert(
                        GUnit(0.5.toBigDecimal())
                    ), null
                ),
                InntektPerÅr(
                    Year.of(2024), Grunnbeløp.finnGrunnbeløp(1 januar 2025).multiplisert(
                        GUnit(0.6.toBigDecimal())
                    ), null
                ),
                InntektPerÅr(
                    Year.of(2025), Grunnbeløp.finnGrunnbeløp(1 januar 2025).multiplisert(
                        GUnit(inntektIG.toBigDecimal())
                    ), null
                )
            )
        )

        assertThat(resultat.kanBehandlesAutomatisk).isEqualTo(kanBehandlesAutomatisk)
    }

    @ParameterizedTest
    @CsvSource(
        // Inntekter i G for
        // 2023    2024    2025  - Forventet resultat
        "   0.4,   0.4,    1.0,      false",
        "   1.5,   1.0,    1.0,      true"
    )
    fun `om inntekt siste 3 år er over 3G, men ikke siste år, kan vilkåret likevel behandles automatisk`(
        år1: Double,
        år2: Double,
        år3: Double,
        kanBehandlesAutomatisk: Boolean
    ) {
        val resultat = service.vurderInntektsbortfall(
            fødselsdato = Fødselsdato(13 mars 1963),
            inntektPerÅr = listOf(år1, år2, år3).mapIndexed { index, inntekt ->
                val årForInntekt = 2023 + index
                InntektPerÅr(
                    Year.of(årForInntekt), Grunnbeløp.finnGrunnbeløp(1 januar årForInntekt).multiplisert(
                        GUnit(inntekt.toBigDecimal())
                    ), null
                )
            }.toSet()
        )

        assertThat(resultat.kanBehandlesAutomatisk).isEqualTo(kanBehandlesAutomatisk)
    }
}