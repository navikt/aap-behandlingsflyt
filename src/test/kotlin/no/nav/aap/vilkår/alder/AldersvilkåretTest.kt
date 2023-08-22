package no.nav.aap.vilkår.alder

import no.nav.aap.domene.behandling.Utfall
import no.nav.aap.domene.behandling.grunnlag.person.Fødselsdato
import no.nav.aap.domene.vilkår.alder.Aldersgrunnlag
import no.nav.aap.domene.vilkår.alder.Aldersvilkåret
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class AldersvilkåretTest {

    @Test
    fun `vilkåret er ikke oppfylt hvis bruker søker når de er under 18 år`() {
        val aldersgrunnlaget = Aldersgrunnlag(
            søknadsdato = LocalDate.now(),
            fødselsdato = Fødselsdato(LocalDate.now().minusYears(17))
        )

        val resultat = Aldersvilkåret.vurder(aldersgrunnlaget)

        assertThat(resultat.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
    }

    @Test
    fun `vilkåret er ikke oppfylt hvis bruker søker dagen før 18-årsdagen`() {
        val fødselsdato = LocalDate.now().minusYears(18)
        val dagenFør18årsdagen = LocalDate.now().minusDays(1)
        val aldersgrunnlaget = Aldersgrunnlag(
            søknadsdato = dagenFør18årsdagen,
            fødselsdato = Fødselsdato(fødselsdato)
        )

        val resultat = Aldersvilkåret.vurder(aldersgrunnlaget)

        assertThat(resultat.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
    }

    @Test
    fun `vilkåret er ikke oppfylt hvis bruker søker etter de har fylt 67 år`() {
        val aldersgrunnlaget = Aldersgrunnlag(
            søknadsdato = LocalDate.now(),
            fødselsdato = Fødselsdato(LocalDate.now().minusYears(68))
        )

        val resultat = Aldersvilkåret.vurder(aldersgrunnlaget)

        assertThat(resultat.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
    }

    @Test
    fun `vilkåret er ikke oppfylt hvis bruker søker på 67-årsdagen`() {
        val fødselsdato = LocalDate.now().minusYears(67)
        val dagenManFyller67år = LocalDate.now()
        val aldersgrunnlaget = Aldersgrunnlag(
            søknadsdato = dagenManFyller67år,
            fødselsdato = Fødselsdato(fødselsdato)
        )

        val resultat = Aldersvilkåret.vurder(aldersgrunnlaget)

        assertThat(resultat.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
    }

    @Test
    fun `vilkåret er oppfylt hvis bruker er mellom 18 og 67`() {
        val aldersgrunnlaget = Aldersgrunnlag(
            søknadsdato = LocalDate.now(),
            fødselsdato = Fødselsdato(LocalDate.now().minusYears(45))
        )

        val resultat = Aldersvilkåret.vurder(aldersgrunnlaget)

        assertThat(resultat.utfall).isEqualTo(Utfall.OPPFYLT)
    }

}
