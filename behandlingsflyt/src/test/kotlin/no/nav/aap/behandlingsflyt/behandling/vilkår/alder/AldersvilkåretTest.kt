package no.nav.aap.behandlingsflyt.behandling.vilkår.alder

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.test.april
import no.nav.aap.behandlingsflyt.test.mai
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class AldersvilkåretTest {

    @Test
    fun `vilkåret er ikke oppfylt hvis bruker søker når de er under 18 år`() {
        val nå = LocalDate.now()
        val aldersgrunnlaget = Aldersgrunnlag(
            periode = Periode(nå, nå.plusYears(3)),
            fødselsdato = Fødselsdato(LocalDate.now().minusYears(17))
        )

        val vilkårsresultat = Vilkårsresultat()
        Aldersvilkåret(vilkårsresultat).vurder(aldersgrunnlaget)

        val vilkåret = vilkårsresultat.finnVilkår(Vilkårtype.ALDERSVILKÅRET)
        assertThat(vilkåret.vilkårsperioder().first().utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
    }

    @Test
    fun `vilkåret er ikke oppfylt hvis bruker søker dagen før 18-årsdagen`() {
        val fødselsdato = LocalDate.now().minusYears(18)
        val dagenFør18årsdagen = LocalDate.now().minusDays(1)
        val rettighetsperiode = Periode(dagenFør18årsdagen, dagenFør18årsdagen.plusYears(3))
        val aldersgrunnlaget = Aldersgrunnlag(
            periode = rettighetsperiode,
            fødselsdato = Fødselsdato(fødselsdato)
        )

        val vilkårsresultat = Vilkårsresultat()
        Aldersvilkåret(vilkårsresultat).vurder(aldersgrunnlaget)
        val vilkåret = vilkårsresultat.finnVilkår(Vilkårtype.ALDERSVILKÅRET)

        assertThat(vilkåret.vilkårsperioder().first().utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
    }

    @Test
    fun `vilkåret er ikke oppfylt hvis bruker søker etter de har fylt 67 år`() {
        val søknadsdato = LocalDate.now()
        val rettighetsperiode = Periode(søknadsdato, søknadsdato.plusYears(3))
        val aldersgrunnlaget = Aldersgrunnlag(
            periode = rettighetsperiode,
            fødselsdato = Fødselsdato(LocalDate.now().minusYears(68))
        )

        val vilkårsresultat = Vilkårsresultat()
        Aldersvilkåret(vilkårsresultat).vurder(aldersgrunnlaget)
        val vilkåret = vilkårsresultat.finnVilkår(Vilkårtype.ALDERSVILKÅRET)

        assertThat(vilkåret.vilkårsperioder().first().utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
    }

    @Test
    fun `vilkåret er oppfylt siste dag i måneden etter 67-årsdagen`() {
        val fødselsdato = 16 april 1958 // 67-år-dagen er 16 april 2025
        val rettighetsperiode = Periode(30 april 2025, 30 april 2028)
        val aldersgrunnlaget = Aldersgrunnlag(
            periode = rettighetsperiode,
            fødselsdato = Fødselsdato(fødselsdato)
        )

        val vilkårsresultat = Vilkårsresultat()
        Aldersvilkåret(vilkårsresultat).vurder(aldersgrunnlaget)
        val vilkåret = vilkårsresultat.finnVilkår(Vilkårtype.ALDERSVILKÅRET)

        assertThat(vilkåret.vilkårsperioder().first().utfall).isEqualTo(Utfall.OPPFYLT)
    }

    @Test
    fun `vilkåret er ikke oppfylt første dag i måneden etter 67-årsdagen`() {
        val fødselsdato = 16 april 1958 // 67-år-dagen er 16 april 2025
        val rettighetsperiode = Periode(1 mai 2025, 1 mai 2028)
        val aldersgrunnlaget = Aldersgrunnlag(
            periode = rettighetsperiode,
            fødselsdato = Fødselsdato(fødselsdato)
        )

        val vilkårsresultat = Vilkårsresultat()
        Aldersvilkåret(vilkårsresultat).vurder(aldersgrunnlaget)
        val vilkåret = vilkårsresultat.finnVilkår(Vilkårtype.ALDERSVILKÅRET)

        assertThat(vilkåret.vilkårsperioder().first().utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
    }

    @Test
    fun `vilkåret er ikke oppfylt dagen etter 67-årsdagen hvis bursdag er siste dag i måneden`() {
        val fødselsdato = 30 april 1958 // 67-år-dagen er 16 april 2025
        val rettighetsperiode = Periode(1 mai 2025, 1 mai 2028)
        val aldersgrunnlaget = Aldersgrunnlag(
            periode = rettighetsperiode,
            fødselsdato = Fødselsdato(fødselsdato)
        )

        val vilkårsresultat = Vilkårsresultat()
        Aldersvilkåret(vilkårsresultat).vurder(aldersgrunnlaget)
        val vilkåret = vilkårsresultat.finnVilkår(Vilkårtype.ALDERSVILKÅRET)

        assertThat(vilkåret.vilkårsperioder().first().utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
    }

    @Test
    fun `vilkåret er ikke oppfylt hvis bruker søker måneden etter 67-årsdagen`() {
        val fødselsdato = LocalDate.now().minusYears(67)
        val månendenEtterFylte67år = LocalDate.now().plusMonths(1)
        val rettighetsperiode = Periode(månendenEtterFylte67år, månendenEtterFylte67år.plusYears(3))
        val aldersgrunnlaget = Aldersgrunnlag(
            periode = rettighetsperiode,
            fødselsdato = Fødselsdato(fødselsdato)
        )

        val vilkårsresultat = Vilkårsresultat()
        Aldersvilkåret(vilkårsresultat).vurder(aldersgrunnlaget)
        val vilkåret = vilkårsresultat.finnVilkår(Vilkårtype.ALDERSVILKÅRET)

        assertThat(vilkåret.vilkårsperioder().first().utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
    }

    @Test
    fun `vilkåret er oppfylt hvis bruker er mellom 18 og 67`() {
        val søknadsdato = LocalDate.now()
        val rettighetsperiode = Periode(søknadsdato, søknadsdato.plusYears(3))
        val aldersgrunnlaget = Aldersgrunnlag(
            periode = rettighetsperiode,
            fødselsdato = Fødselsdato(LocalDate.now().minusYears(45))
        )

        val vilkårsresultat = Vilkårsresultat()
        Aldersvilkåret(vilkårsresultat).vurder(aldersgrunnlaget)
        val vilkåret = vilkårsresultat.finnVilkår(Vilkårtype.ALDERSVILKÅRET)

        assertThat(vilkåret.vilkårsperioder().first().utfall).isEqualTo(Utfall.OPPFYLT)
    }

    @Test
    fun `vilkåret er oppfylt for perioden bruker er mellom 18 og 67`() {
        val søknadsdato = LocalDate.now()
        val rettighetsperiode = Periode(søknadsdato, søknadsdato.plusYears(3))
        val aldersgrunnlaget = Aldersgrunnlag(
            periode = rettighetsperiode,
            fødselsdato = Fødselsdato(LocalDate.now().minusYears(66).minusMonths(6))
        )

        val vilkårsresultat = Vilkårsresultat()
        Aldersvilkåret(vilkårsresultat).vurder(aldersgrunnlaget)
        val aldersvilkåret = vilkårsresultat.finnVilkår(Vilkårtype.ALDERSVILKÅRET)

        assertThat(aldersvilkåret.vilkårsperioder()).hasSize(2)
        assertThat(aldersvilkåret.vilkårsperioder()).anyMatch { it.erOppfylt() }
        assertThat(aldersvilkåret.vilkårsperioder()).anyMatch { !it.erOppfylt() }
    }

}
