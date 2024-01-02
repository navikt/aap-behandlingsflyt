package no.nav.aap.behandlingsflyt.flyt.vilkår.sykdom

import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.sykdom.NedreGrense
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.flyt.vilkår.Utfall
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkårsresultat
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkårtype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate


class SykdomsvilkårTest {

    @Test
    fun `testNye vurderinger skal overskrive`() {
        val vilkårsresultat = Vilkårsresultat()
        vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.SYKDOMSVILKÅRET)

        Sykdomsvilkår(vilkårsresultat).vurder(
            SykdomsFaktagrunnlag(
                vurderingsdato = LocalDate.now(),
                sisteDagMedMuligYtelse = LocalDate.now().plusYears(3),
                yrkesskadevurdering = null,
                sykdomsvurdering = Sykdomsvurdering(
                    begrunnelse = "",
                    dokumenterBruktIVurdering = listOf(),
                    erSkadeSykdomEllerLyteVesentligdel = true,
                    erNedsettelseIArbeidsevneHøyereEnnNedreGrense = true,
                    nedreGrense = NedreGrense.FEMTI,
                    nedsattArbeidsevneDato = LocalDate.now().minusYears(1),
                    ytterligereNedsattArbeidsevneDato = null
                ),
                studentvurdering = null
            )
        )
        val vilkår = vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)

        assertThat(vilkår.vilkårsperioder()).hasSize(1).allMatch { periode -> periode.utfall == Utfall.OPPFYLT }

        Sykdomsvilkår(vilkårsresultat).vurder(
            SykdomsFaktagrunnlag(
                vurderingsdato = LocalDate.now(),
                sisteDagMedMuligYtelse = LocalDate.now().plusYears(3),
                yrkesskadevurdering = null,
                sykdomsvurdering = Sykdomsvurdering(
                    begrunnelse = "",
                    dokumenterBruktIVurdering = listOf(),
                    erSkadeSykdomEllerLyteVesentligdel = true,
                    erNedsettelseIArbeidsevneHøyereEnnNedreGrense = false,
                    nedreGrense = NedreGrense.FEMTI,
                    nedsattArbeidsevneDato = LocalDate.now().minusYears(1),
                    ytterligereNedsattArbeidsevneDato = null
                ),
                studentvurdering = null
            )
        )

        assertThat(vilkår.vilkårsperioder()).hasSize(1).allMatch { periode -> periode.utfall == Utfall.IKKE_OPPFYLT }
    }
}