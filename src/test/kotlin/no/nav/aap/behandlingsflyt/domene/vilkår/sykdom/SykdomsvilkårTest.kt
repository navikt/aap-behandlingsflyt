package no.nav.aap.behandlingsflyt.domene.vilkår.sykdom

import no.nav.aap.behandlingsflyt.avklaringsbehov.sykdom.NedreGrense
import no.nav.aap.behandlingsflyt.avklaringsbehov.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.domene.behandling.Utfall
import no.nav.aap.behandlingsflyt.domene.behandling.Vilkårsresultat
import no.nav.aap.behandlingsflyt.domene.behandling.Vilkårstype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate


class SykdomsvilkårTest {

    @Test
    fun `testNye vurderinger skal overskrive`() {
        val vilkårsresultat = Vilkårsresultat()
        vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårstype.SYKDOMSVILKÅRET)

        Sykdomsvilkår(vilkårsresultat).vurder(
            SykdomsFaktagrunnlag(
                LocalDate.now(),
                LocalDate.now().plusYears(3),
                null,
                Sykdomsvurdering("", listOf(), true, true, NedreGrense.FEMTI, LocalDate.now().minusYears(1))
            )
        )
        val vilkår = vilkårsresultat.finnVilkår(Vilkårstype.SYKDOMSVILKÅRET)

        assertThat(vilkår.vilkårsperioder()).hasSize(1).allMatch { periode -> periode.utfall == Utfall.OPPFYLT }

        Sykdomsvilkår(vilkårsresultat).vurder(
            SykdomsFaktagrunnlag(
                LocalDate.now(),
                LocalDate.now().plusYears(3),
                null,
                Sykdomsvurdering("", listOf(), true, false, NedreGrense.FEMTI, LocalDate.now().minusYears(1))
            )
        )

        assertThat(vilkår.vilkårsperioder()).hasSize(1).allMatch { periode -> periode.utfall == Utfall.IKKE_OPPFYLT }
    }
}