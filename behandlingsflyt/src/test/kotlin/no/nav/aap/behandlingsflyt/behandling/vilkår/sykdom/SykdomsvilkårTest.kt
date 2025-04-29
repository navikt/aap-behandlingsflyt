package no.nav.aap.behandlingsflyt.behandling.vilkår.sykdom

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.help.assertTidslinje
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.httpklient.auth.Bruker
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.type.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate


class SykdomsvilkårTest {
    @Test
    fun `Nye vurderinger skal overskrive`() {
        val vilkårsresultat = Vilkårsresultat()
        vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.SYKDOMSVILKÅRET)

        Sykdomsvilkår(vilkårsresultat).vurder(
            SykdomsFaktagrunnlag(
                vurderingsdato = LocalDate.now(),
                sisteDagMedMuligYtelse = LocalDate.now().plusYears(3),
                yrkesskadevurdering = null,
                sykdomsvurderinger = listOf(
                    sykdomsvurdering()
                ),
                studentvurdering = null,
                sykepengerErstatningFaktagrunnlag = null
            )
        )
        val vilkår = vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)

        assertThat(vilkår.vilkårsperioder()).hasSize(1).allMatch { periode -> periode.utfall == Utfall.OPPFYLT }

        Sykdomsvilkår(vilkårsresultat).vurder(
            SykdomsFaktagrunnlag(
                vurderingsdato = LocalDate.now(),
                sisteDagMedMuligYtelse = LocalDate.now().plusYears(3),
                yrkesskadevurdering = null,
                sykdomsvurderinger = listOf(
                    sykdomsvurdering(erNedsettelseIArbeidsevneMerEnnHalvparten = false)
                ),
                studentvurdering = null,
                sykepengerErstatningFaktagrunnlag = null
            )
        )

        assertThat(vilkår.vilkårsperioder()).hasSize(1).allMatch { periode -> periode.utfall == Utfall.IKKE_OPPFYLT }
    }

    @Test
    fun `vilkår med ulike utfall`() {
        val vilkårsresultat = Vilkårsresultat()
        vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.SYKDOMSVILKÅRET)
        val startDato = 1 januar 2024
        val opprettet = Instant.now()
        Sykdomsvilkår(vilkårsresultat).vurder(
            SykdomsFaktagrunnlag(
                vurderingsdato = startDato,
                sisteDagMedMuligYtelse = startDato.plusYears(3),
                yrkesskadevurdering = null,
                sykdomsvurderinger = listOf(
                    sykdomsvurdering(opprettet = opprettet),
                    sykdomsvurdering(
                        erNedsettelseIArbeidsevneMerEnnHalvparten = false,
                        vurderingenGjelderFra = startDato.plusWeeks(1),
                        opprettet = opprettet.plusSeconds(50)
                    )
                ),
                studentvurdering = null,
                sykepengerErstatningFaktagrunnlag = null
            )
        )

        val vilkår = vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)

        assertThat(vilkår.vilkårsperioder()).hasSize(2)

        vilkår.tidslinje().assertTidslinje(
            Segment(Periode(1 januar 2024, 7 januar 2024)) { vurdering ->
                assertThat(vurdering.utfall).isEqualTo(Utfall.OPPFYLT)
            },
            Segment(Periode(8 januar 2024, 1 januar 2027)) { vurdering ->
                assertThat(vurdering.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
            },
        )
    }
    
    @Test
    fun `Krever ikke svar på viss varighet ved revurdering`() {
        val vilkårsresultat = Vilkårsresultat()
        vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.SYKDOMSVILKÅRET)
        val startDato = 1 januar 2024
        val opprettet = Instant.now()
        Sykdomsvilkår(vilkårsresultat).vurder(
            SykdomsFaktagrunnlag(
                vurderingsdato = startDato,
                sisteDagMedMuligYtelse = startDato.plusYears(3),
                yrkesskadevurdering = null,
                sykdomsvurderinger = listOf(
                    sykdomsvurdering(opprettet = opprettet),
                    sykdomsvurdering(
                        erNedsettelseIArbeidsevneAvEnVissVarighet = null,
                        vurderingenGjelderFra = startDato.plusWeeks(1),
                        opprettet = opprettet.plusSeconds(50)
                    )
                ),
                studentvurdering = null,
                sykepengerErstatningFaktagrunnlag = null
            )
        )

        val vilkår = vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)

        assertThat(vilkår.vilkårsperioder()).hasSize(2)

        vilkår.tidslinje().assertTidslinje(
            Segment(Periode(1 januar 2024, 7 januar 2024)) { vurdering ->
                assertThat(vurdering.utfall).isEqualTo(Utfall.OPPFYLT)
            },
            Segment(Periode(8 januar 2024, 1 januar 2027)) { vurdering ->
                assertThat(vurdering.utfall).isEqualTo(Utfall.OPPFYLT)
            },
        )
    }

    private fun sykdomsvurdering(
        harSkadeSykdomEllerLyte: Boolean = true,
        erSkadeSykdomEllerLyteVesentligdel: Boolean = true,
        erNedsettelseIArbeidsevneMerEnnHalvparten: Boolean = true,
        erNedsettelseIArbeidsevneAvEnVissVarighet: Boolean? = true,
        erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense: Boolean = true,
        erArbeidsevnenNedsatt: Boolean = true,
        vurderingenGjelderFra: LocalDate? = null,
        opprettet: Instant = Instant.now(),
    ) = Sykdomsvurdering(
        begrunnelse = "",
        dokumenterBruktIVurdering = listOf(),
        harSkadeSykdomEllerLyte = harSkadeSykdomEllerLyte,
        erSkadeSykdomEllerLyteVesentligdel = erSkadeSykdomEllerLyteVesentligdel,
        erNedsettelseIArbeidsevneMerEnnHalvparten = erNedsettelseIArbeidsevneMerEnnHalvparten,
        erNedsettelseIArbeidsevneAvEnVissVarighet = erNedsettelseIArbeidsevneAvEnVissVarighet,
        erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense,
        erArbeidsevnenNedsatt = erArbeidsevnenNedsatt,
        yrkesskadeBegrunnelse = null,
        vurderingenGjelderFra = vurderingenGjelderFra,
        vurdertAv = Bruker("Z00000"),
        opprettet = opprettet,
    )
}
