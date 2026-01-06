package no.nav.aap.behandlingsflyt.behandling.`vilkår`.inntektsbortfall

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.Grunnbeløp
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
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
    val kontekst = FlytKontekstMedPerioder(
        sakId = SakId(0L),
        behandlingId = BehandlingId(0L),
        forrigeBehandlingId = null,
        behandlingType = TypeBehandling.Førstegangsbehandling,
        vurderingType = VurderingType.FØRSTEGANGSBEHANDLING,
        rettighetsperiode = Periode(1 januar 2026, 31 desember 2026),
        vurderingsbehovRelevanteForSteg = emptySet()
    )

    val service = InntektsbortfallVurderingService(
        kontekst, setOf(Year.of(2023), Year.of(2024), Year.of(2025))
    )

    @Test
    fun `om søker er under 62 på kravtidspunkt, kan vilkåret behandles automatisk`() {
        val resultat = service.vurderInntektsbortfall(
            // Under 62 år
            fødselsdato = Fødselsdato(13 mars 1990),
            manuelleInntekter = setOf(),
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
            manuelleInntekter = setOf(),
            inntektPerÅr = setOf(
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
        // 2023 2024 2025  - Forventet resultat
        "   3,   2,    1,      false",
        "   4,   3,    2,      true"
    )
    fun `om gjennomsnittlig inntekt er over 3G, men ikke siste år, kan vilkåret likevel behandles automatisk`(
        år1: Int,
        år2: Int,
        år3: Int,
        kanBehandlesAutomatisk: Boolean
    ) {
        val resultat = service.vurderInntektsbortfall(
            fødselsdato = Fødselsdato(13 mars 1963),
            manuelleInntekter = setOf(),
            inntektPerÅr = setOf(år1, år2, år3).mapIndexed { index, inntekt ->
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