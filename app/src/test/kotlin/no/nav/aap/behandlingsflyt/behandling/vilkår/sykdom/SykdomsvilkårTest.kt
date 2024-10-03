package no.nav.aap.behandlingsflyt.behandling.vilkår.sykdom

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.NedreGrense
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
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
                    harSkadeSykdomEllerLyte = true,
                    erSkadeSykdomEllerLyteVesentligdel = true,
                    erNedsettelseIArbeidsevneHøyereEnnNedreGrense = true,
                    nedreGrense = NedreGrense.FEMTI,
                    nedsattArbeidsevneDato = LocalDate.now(),
                    erArbeidsevnenNedsatt = true
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
                    harSkadeSykdomEllerLyte = true,
                    erSkadeSykdomEllerLyteVesentligdel = true,
                    erNedsettelseIArbeidsevneHøyereEnnNedreGrense = false,
                    nedreGrense = NedreGrense.FEMTI,
                    nedsattArbeidsevneDato = LocalDate.now(),
                    erArbeidsevnenNedsatt = true
                ),
                studentvurdering = null
            )
        )

        assertThat(vilkår.vilkårsperioder()).hasSize(1).allMatch { periode -> periode.utfall == Utfall.IKKE_OPPFYLT }
    }
}