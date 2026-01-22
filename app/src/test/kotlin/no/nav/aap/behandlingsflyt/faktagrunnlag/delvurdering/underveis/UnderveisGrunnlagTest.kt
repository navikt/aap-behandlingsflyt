package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.RettighetsType
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.test.april
import no.nav.aap.behandlingsflyt.test.august
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.mai
import no.nav.aap.behandlingsflyt.test.september
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Dagsatser
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.komponenter.verdityper.TimerArbeid
import org.assertj.core.api.Assertions.assertThat
import java.math.BigDecimal
import kotlin.random.Random
import kotlin.test.Test

class UnderveisGrunnlagTest {

    val forventetSisteDagMedYtelse = 31 august 2026

    /**
     * Per i dag vil underveisperioder i underveisgrunnlag kun
     * ha 2 reelle utfall:
     *
     *  { OPPFYLT, IKKE_OPPFYLT }
     *
     *  Dette er "slutt-tilstander" for individuelle vilkårsresultater.
     *
     *  Dvs. utfallene { IKKE_VURDERT, IKKE_RELEVANT }
     *  er ikke relevant for underveisgrunnlag/tidslinje
     */
    @Test
    fun `sisteDagMedYtelse() henter korrekt tom dato i periodelisten`() {
        val underveisGrunnlag = underveisGrunnlag(
            underveisperiode(
                periode = Periode(1 januar 2025, 30 april 2025),
                rettighetsType = RettighetsType.BISTANDSBEHOV,
                utfall = Utfall.OPPFYLT,
            ),
            underveisperiode(
                periode = Periode(1 mai 2025, forventetSisteDagMedYtelse),
                rettighetsType = RettighetsType.BISTANDSBEHOV,
                utfall = Utfall.OPPFYLT,
            ),
            underveisperiode(
                periode = Periode(1 september 2025, 31 desember 2025),
                rettighetsType = RettighetsType.BISTANDSBEHOV,
                utfall = Utfall.IKKE_OPPFYLT,
            )
        )

        assertThat(underveisGrunnlag.sisteDagMedYtelse()).isEqualTo(forventetSisteDagMedYtelse)
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
        utfall: Utfall
    ): Underveisperiode {
        return Underveisperiode(
            periode = periode,
            meldePeriode = periode,
            utfall = utfall,
            rettighetsType = rettighetsType,
            avslagsårsak = if (utfall == Utfall.OPPFYLT) null else UnderveisÅrsak.VARIGHETSKVOTE_BRUKT_OPP,
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
            meldepliktGradering = Prosent.`0_PROSENT`,
        )
    }

}