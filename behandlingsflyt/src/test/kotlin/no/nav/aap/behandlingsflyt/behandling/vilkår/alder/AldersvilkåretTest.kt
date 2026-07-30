package no.nav.aap.behandlingsflyt.behandling.vilkår.alder

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.help.assertTidslinje
import no.nav.aap.behandlingsflyt.test.april
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.mai
import no.nav.aap.behandlingsflyt.test.mars
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class AldersvilkåretTest {

    @Test
    fun `vilkåret er ikke oppfylt hvis bruker søker dagen før 18-årsdagen (gammel løsning)`() {
        val fødselsdato = LocalDate.now().minusYears(18)
        val dagenFør18årsdagen = LocalDate.now().minusDays(1)
        val rettighetsperiode = Periode(dagenFør18årsdagen, dagenFør18årsdagen.plusYears(3))
        val aldersgrunnlaget = Aldersgrunnlag(
            periode = rettighetsperiode,
            fødselsdato = Fødselsdato(fødselsdato),
            grenseForAntallMånederFørFylte18 = 0,
        )

        val resultat = Aldersvilkåret().vurder(aldersgrunnlaget)

//        assertThat(resultat.segmenter()).hasSize(1)
        assertThat(resultat.segmenter().first().verdi.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
    }

    @Test
    fun `vilkåret er ikke oppfylt hvis bruker søker når de er under 18 år`() {
        val rettighetsperiode = Periode(1 januar 2020, 1 januar 2023)
        val fødselsdato = Fødselsdato(rettighetsperiode.fom.minusYears(17))

        val aldersgrunnlaget = Aldersgrunnlag(
            periode = rettighetsperiode,
            fødselsdato = fødselsdato,
            grenseForAntallMånederFørFylte18 = 3,
            vurderingsdato = 1 januar 2020
        )

        val resultat = Aldersvilkåret().vurder(aldersgrunnlaget)

        assertTidslinje(resultat, rettighetsperiode to {
            assertThat(it.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
        })
    }

    @Test
    fun `vilkåret er ikke oppfylt hvis bruker søker dagen før 18-årsdagen`() {
        val rettighetsperiode = Periode(1 januar 2020, 1 januar 2023)
        val fødselsdato = Fødselsdato(rettighetsperiode.fom.minusYears(17).minusMonths(9))

        val aldersgrunnlaget = Aldersgrunnlag(
            periode = rettighetsperiode,
            fødselsdato = fødselsdato,
            grenseForAntallMånederFørFylte18 = 3,
            vurderingsdato = 1 januar 2020
        )

        val vilkåret = Aldersvilkåret().vurder(aldersgrunnlaget)

        assertTidslinje(
            vilkåret, Periode(rettighetsperiode.fom, 31 mars 2020) to {
                assertThat(it.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
            },
            Periode(1 april 2020, rettighetsperiode.tom) to {
                assertThat(it.utfall).isEqualTo(Utfall.OPPFYLT)
            })
    }

    @Test
    fun `vilkåret er ikke oppfylt hvis bruker søker etter de har fylt 67 år`() {
        val søknadsdato = LocalDate.now()
        val rettighetsperiode = Periode(søknadsdato, søknadsdato.plusYears(3))
        val aldersgrunnlaget = Aldersgrunnlag(
            periode = rettighetsperiode,
            fødselsdato = Fødselsdato(LocalDate.now().minusYears(68)),
            grenseForAntallMånederFørFylte18 = 3
        )

        val vilkåret = Aldersvilkåret().vurder(aldersgrunnlaget)

        assertThat(vilkåret.segmenter()).allSatisfy { segment ->
            assertThat(segment.verdi.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
        }
    }

    @Test
    fun `vilkåret er oppfylt siste dag i måneden etter 67-årsdagen`() {
        val fødselsdato = 16 april 1958 // 67-år-dagen er 16 april 2025
        val rettighetsperiode = Periode(30 april 2025, 30 april 2028)
        val aldersgrunnlaget = Aldersgrunnlag(
            periode = rettighetsperiode,
            fødselsdato = Fødselsdato(fødselsdato),
            grenseForAntallMånederFørFylte18 = 3,
            vurderingsdato = rettighetsperiode.fom
        )

        val resultat = Aldersvilkåret().vurder(aldersgrunnlaget)

        assertThat(resultat.segmenter().first().verdi.utfall).isEqualTo(Utfall.OPPFYLT)
    }

    @Test
    fun `vilkåret er ikke oppfylt første dag i måneden etter 67-årsdagen`() {
        val fødselsdato = 16 april 1958 // 67-år-dagen er 16 april 2025
        val rettighetsperiode = Periode(1 mai 2025, 1 mai 2028)
        val aldersgrunnlaget = Aldersgrunnlag(
            periode = rettighetsperiode,
            fødselsdato = Fødselsdato(fødselsdato),
            grenseForAntallMånederFørFylte18 = 3,
            vurderingsdato = rettighetsperiode.fom
        )
        val resultat = Aldersvilkåret().vurder(aldersgrunnlaget)

        assertThat(resultat.segmenter()).allSatisfy {
            assertThat(it.verdi.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
        }
    }

    @Test
    fun `vilkåret er ikke oppfylt dagen etter 67-årsdagen hvis bursdag er siste dag i måneden`() {
        val fødselsdato = 30 april 1958 // 67-år-dagen er 16 april 2025
        val rettighetsperiode = Periode(1 mai 2025, 1 mai 2028)
        val aldersgrunnlaget = Aldersgrunnlag(
            periode = rettighetsperiode,
            fødselsdato = Fødselsdato(fødselsdato),
            grenseForAntallMånederFørFylte18 = 3,
            vurderingsdato = rettighetsperiode.fom
        )

        val resultat = Aldersvilkåret().vurder(aldersgrunnlaget)

        assertThat(resultat.segmenter()).allSatisfy {
            assertThat(it.verdi.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
        }
    }

    @Test
    fun `vilkåret er ikke oppfylt hvis bruker søker måneden etter 67-årsdagen`() {
        val fødselsdato = LocalDate.now().minusYears(67)
        val månendenEtterFylte67år = LocalDate.now().plusMonths(1)
        val rettighetsperiode = Periode(månendenEtterFylte67år, månendenEtterFylte67år.plusYears(3))
        val aldersgrunnlaget = Aldersgrunnlag(
            periode = rettighetsperiode,
            fødselsdato = Fødselsdato(fødselsdato),
            grenseForAntallMånederFørFylte18 = 3,
        )

        val resultat = Aldersvilkåret().vurder(aldersgrunnlaget)

        assertThat(resultat.segmenter()).allSatisfy {
            assertThat(it.verdi.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
        }
    }

    @Test
    fun `vilkåret er oppfylt hvis bruker er mellom 18 og 67`() {
        val søknadsdato = LocalDate.now()
        val rettighetsperiode = Periode(søknadsdato, søknadsdato.plusYears(3))
        val aldersgrunnlaget = Aldersgrunnlag(
            periode = rettighetsperiode,
            fødselsdato = Fødselsdato(LocalDate.now().minusYears(45)),
            grenseForAntallMånederFørFylte18 = 3
        )

        val resultat = Aldersvilkåret().vurder(aldersgrunnlaget)

        assertThat(resultat.segmenter()).allSatisfy {
            assertThat(it.verdi.utfall).isEqualTo(Utfall.OPPFYLT)
        }
    }

    @Test
    fun `vilkåret er oppfylt for perioden bruker er mellom 18 og 67`() {
        val søknadsdato = LocalDate.now()
        val rettighetsperiode = Periode(søknadsdato, søknadsdato.plusYears(3))
        val aldersgrunnlaget = Aldersgrunnlag(
            periode = rettighetsperiode,
            fødselsdato = Fødselsdato(LocalDate.now().minusYears(66).minusMonths(6)),
            grenseForAntallMånederFørFylte18 = 3
        )

        val resultat = Aldersvilkåret().vurder(aldersgrunnlaget)

        val måned67 = YearMonth.from(aldersgrunnlaget.fødselsdato.dato.plusYears(67).minusDays(1)).atEndOfMonth()

        assertTidslinje(resultat, Periode(søknadsdato, måned67) to {
            assertThat(it.utfall).isEqualTo(Utfall.OPPFYLT)
        }, Periode(måned67.plusDays(1), søknadsdato.plusYears(3)) to {
            assertThat(it.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
        })
    }

}
