package no.nav.aap.behandlingsflyt

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.ArbeidsGradering
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.hendelse.kafka.person.PdlHendelseKonsumentUtils
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Dagsatser
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.komponenter.verdityper.TimerArbeid
import java.math.BigDecimal
import java.time.Instant
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PdlHendelseKonsumentUtilsTest {

    val pdlHendelseKonsumentUtils = PdlHendelseKonsumentUtils()

    @Test
    fun `sjekker om alle periodene etter at bruker er død gir avslag`() {
        val opprettetTidspunkt = Instant.parse("2025-10-31T10:15:30.00Z")
        val underveisGrunnlag = underveisGrunnlag(
            underveisperiode(
                periode = Periode(1 januar 2026, 15 januar 2026),
                rettighetsType = RettighetsType.BISTANDSBEHOV,
                avslagsÅrsak = UnderveisÅrsak.IKKE_GRUNNLEGGENDE_RETT,
                utfall = no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall.OPPFYLT,
            ), underveisperiode(
                periode = Periode(16 januar 2026, 31 januar 2026),
                rettighetsType = RettighetsType.BISTANDSBEHOV,
                avslagsÅrsak = UnderveisÅrsak.IKKE_GRUNNLEGGENDE_RETT,
                utfall = no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall.OPPFYLT,
            )
        )

        val result =
            pdlHendelseKonsumentUtils.allePerioderEtterOpprettetTidspunktHarAvslagsårsak(opprettetTidspunkt, underveisGrunnlag)
        assertTrue(result)
    }

    @Test
    fun `sjekker om minst en periode etter at bruker er død og ikke har avslagårsak ikke gir avslag`() {
        val opprettetTidspunkt = Instant.parse("2025-10-31T10:15:30.00Z")

        val underveisGrunnlag = underveisGrunnlag(
            underveisperiode(
                periode = Periode(1 januar 2026, 15 januar 2026),
                rettighetsType = RettighetsType.BISTANDSBEHOV,
                avslagsÅrsak = UnderveisÅrsak.IKKE_GRUNNLEGGENDE_RETT,
                utfall = no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall.OPPFYLT,
            ),
            underveisperiode(
                periode = Periode(16 januar 2026, 31 januar 2026),
                rettighetsType = RettighetsType.BISTANDSBEHOV,
                avslagsÅrsak = null,
                utfall = no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall.OPPFYLT,
            )
        )

        val result =
            pdlHendelseKonsumentUtils.allePerioderEtterOpprettetTidspunktHarAvslagsårsak(opprettetTidspunkt, underveisGrunnlag)
        assertFalse(result)
    }

    @Test
    fun `sjekker på om bruker har perioder både før og etter med og uten avslagårsak`() {
        val opprettetTidspunkt = Instant.parse("2025-10-31T10:15:30.00Z")

        val underveisGrunnlag = underveisGrunnlag(
            underveisperiode(
                periode = Periode(1 januar 2024, 15 januar 2024),
                rettighetsType = RettighetsType.BISTANDSBEHOV,
                avslagsÅrsak = UnderveisÅrsak.IKKE_GRUNNLEGGENDE_RETT,
                utfall = no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall.OPPFYLT,
            ),
            underveisperiode(
                periode = Periode(15 januar 2024, 29 januar 2024),
                rettighetsType = RettighetsType.BISTANDSBEHOV,
                avslagsÅrsak = null,
                utfall = no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall.OPPFYLT,
            ),
            underveisperiode(
                periode = Periode(1 januar 2026, 15 januar 2026),
                rettighetsType = RettighetsType.BISTANDSBEHOV,
                avslagsÅrsak = UnderveisÅrsak.IKKE_GRUNNLEGGENDE_RETT,
                utfall = no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall.OPPFYLT,
            ),
            underveisperiode(
                periode = Periode(16 januar 2026, 31 januar 2026),
                rettighetsType = RettighetsType.BISTANDSBEHOV,
                avslagsÅrsak = null,
                utfall = no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall.OPPFYLT,
            )
        )

        val result =
            pdlHendelseKonsumentUtils.allePerioderEtterOpprettetTidspunktHarAvslagsårsak(opprettetTidspunkt, underveisGrunnlag)
        assertFalse(result)
    }

    @Test
    fun `sjekker på om bruker har perioder både før og etter, der en etter har avslagårsak`() {
        val opprettetTidspunkt = Instant.parse("2025-10-31T10:15:30.00Z")

        val underveisGrunnlag = underveisGrunnlag(
            underveisperiode(
                periode = Periode(1 januar 2024, 15 januar 2024),
                rettighetsType = RettighetsType.BISTANDSBEHOV,
                avslagsÅrsak = null,
                utfall = no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall.OPPFYLT,
            ),
            underveisperiode(
                periode = Periode(16 januar 2026, 31 januar 2026),
                rettighetsType = RettighetsType.BISTANDSBEHOV,
                avslagsÅrsak = UnderveisÅrsak.IKKE_GRUNNLEGGENDE_RETT,
                utfall = no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall.OPPFYLT,
            )
        )

        val result =
            pdlHendelseKonsumentUtils.allePerioderEtterOpprettetTidspunktHarAvslagsårsak(opprettetTidspunkt, underveisGrunnlag)
        assertTrue(result)
    }

}

private fun underveisGrunnlag(vararg underveisperioder: Underveisperiode): UnderveisGrunnlag {
    return UnderveisGrunnlag(
        Random.nextLong(),
        underveisperioder.toList()
    )
}

private fun underveisperiode(
    periode: Periode,
    rettighetsType: RettighetsType,
    avslagsÅrsak: UnderveisÅrsak? = null,
    utfall: no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
): Underveisperiode {
    return Underveisperiode(
        periode = periode,
        meldePeriode = periode,
        utfall = utfall,
        rettighetsType = rettighetsType,
        avslagsårsak = avslagsÅrsak,
        grenseverdi = Prosent.`100_PROSENT`,
        institusjonsoppholdReduksjon = Prosent.`0_PROSENT`,
        arbeidsgradering = ArbeidsGradering(
            totaltAntallTimer = TimerArbeid(BigDecimal(0)),
            andelArbeid = Prosent.`0_PROSENT`,
            fastsattArbeidsevne = Prosent.`100_PROSENT`,
            gradering = Prosent.`100_PROSENT`,
            opplysningerMottatt = null,
        ),
        trekk = Dagsatser(0),
        brukerAvKvoter = emptySet(),
        meldepliktStatus = null,
    )
}