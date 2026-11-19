package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning

import no.nav.aap.behandlingsflyt.behandling.vilkår.TidligereVurderinger
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreVurderingPeriode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.uførevurdering.SamordningUføreVurderingValidator
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.uføre.Uføre
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.juli
import no.nav.aap.behandlingsflyt.test.juni
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.tidslinjeOf
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Prosent
import no.nav.aap.komponenter.verdityper.Tid
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SamordningUføreVurderingValidatorTest {
    private val rettighetsperiode = Periode(1 juni 2026, 31 desember 2026)
    private val potensieltOppfylt = TidligereVurderinger.PotensieltOppfylt(null)
    private val avslag = TidligereVurderinger.UunngåeligAvslag(Vilkårtype.SYKDOMSVILKÅRET)

    private val uføre = Uføre(1 januar 2026, Prosent(100), 1 januar 2026, null)

    private fun utfall(vararg segmenter: Pair<Periode, TidligereVurderinger.Behandlingsutfall>): Tidslinje<TidligereVurderinger.Behandlingsutfall> =
        tidslinjeOf(*segmenter)

    private fun manglende(
        utfall: Tidslinje<TidligereVurderinger.Behandlingsutfall>,
        vedtak: Set<Uføre>,
        vurderinger: List<SamordningUføreVurderingPeriode> = emptyList(),
    ) = SamordningUføreVurderingValidator.manglendeVurderinger(utfall, vedtak, vurderinger)

    @Test
    fun `krever vurdering når uføre og rett til AAP overlapper`() {
        val resultat = manglende(utfall(rettighetsperiode to potensieltOppfylt), setOf(uføre))

        assertThat(resultat).containsExactly(1 januar 2026)
    }

    @Test
    fun `krever ikke vurdering når uførevedtaket kun gjelder perioder uten rett`() {
        val resultat = manglende(utfall(rettighetsperiode to avslag), setOf(uføre))

        assertThat(resultat).isEmpty()
    }

    @Test
    fun `krever vurdering når rett kun gjelder deler av uførevedtaket`() {
        val resultat = manglende(
            utfall(
                Periode(1 juni 2026, 30 juni 2026) to potensieltOppfylt,
                Periode(1 juli 2026, 31 desember 2026) to avslag,
            ),
            setOf(uføre),
        )

        assertThat(resultat).containsExactly(1 januar 2026)
    }

    @Test
    fun `krever ikke vurdering når uførevedtaket er avsluttet før perioden med rett`() {
        val avsluttet = uføre.copy(uføregradTom = 31 desember 2025)

        val resultat = manglende(utfall(rettighetsperiode to potensieltOppfylt), setOf(avsluttet))

        assertThat(resultat).isEmpty()
    }

    @Test
    fun `krever ikke vurdering for vedtak som allerede er vurdert`() {
        val resultat = manglende(
            utfall(rettighetsperiode to potensieltOppfylt), setOf(uføre), vurderinger = listOf(SamordningUføreVurderingPeriode(1 januar 2026, Prosent(100)))
        )

        assertThat(resultat).isEmpty()
    }

    @Test
    fun `ser kun på det vedtaket som gjelder i perioden med rett`() {
        val første = uføre.copy(uføregradTom = null)
        val andre = Uføre(1 juli 2026, Prosent(50), 1 juli 2026, null)

        val resultat = manglende(
            utfall(
                Periode(1 januar 2026, 30 juni 2026) to avslag,
                Periode(1 juli 2026, Tid.MAKS) to potensieltOppfylt,
            ),
            setOf(første, andre),
        )

        assertThat(resultat).containsExactly(1 juli 2026)
    }
}
