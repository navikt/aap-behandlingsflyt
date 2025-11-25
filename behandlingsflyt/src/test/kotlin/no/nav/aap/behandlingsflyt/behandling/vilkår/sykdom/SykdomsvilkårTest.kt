package no.nav.aap.behandlingsflyt.behandling.vilkår.sykdom

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Innvilgelsesårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.Bistandsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerErstatningGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerGrunn
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerVurdering
import no.nav.aap.behandlingsflyt.help.assertTidslinje
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset


class SykdomsvilkårTest {
    // TODO: 
    //  Sykdomsvurderinger som gjelder fra dato før rettighetsperiodens start gir avslag på sykdomsvilkåret
    //  Vilkåret bør vel kun bry seg om vurderinger innenfor rettighetsperioden?
    
    @Test
    fun `Nye vurderinger skal overskrive`() {
        val vilkårsresultat = Vilkårsresultat()
        vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.SYKDOMSVILKÅRET)
        
        val kravdato = LocalDate.now()
        Sykdomsvilkår(vilkårsresultat).vurder(
            SykdomsFaktagrunnlag(
                typeBehandling = TypeBehandling.Førstegangsbehandling,
                kravDato = kravdato,
                sisteDagMedMuligYtelse = kravdato.plusYears(3),
                yrkesskadevurdering = null,
                sykdomsvurderinger = listOf(
                    sykdomsvurdering(vurderingenGjelderFra = kravdato)
                ),
                studentvurdering = null,
                sykepengerErstatningFaktagrunnlag = null,
                bistandvurderingFaktagrunnlag = bistandGrunnlag(LocalDate.now()),
            )
        )
        val vilkår = vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)

        assertThat(vilkår.vilkårsperioder()).hasSize(1).allMatch { periode -> periode.utfall == Utfall.OPPFYLT }

        Sykdomsvilkår(vilkårsresultat).vurder(
            SykdomsFaktagrunnlag(
                typeBehandling = TypeBehandling.Førstegangsbehandling,
                kravDato = kravdato,
                sisteDagMedMuligYtelse = kravdato.plusYears(3),
                yrkesskadevurdering = null,
                sykdomsvurderinger = listOf(
                    sykdomsvurdering(vurderingenGjelderFra = kravdato, erNedsettelseIArbeidsevneMerEnnHalvparten = false)
                ),
                studentvurdering = null,
                sykepengerErstatningFaktagrunnlag = null,
                bistandvurderingFaktagrunnlag = null,
            )
        )

        assertThat(vilkår.vilkårsperioder()).hasSize(1).allMatch { periode -> periode.utfall == Utfall.IKKE_OPPFYLT }
    }

    @Test
    fun `vilkår med ulike utfall`() {
        val vilkårsresultat = Vilkårsresultat()
        vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.SYKDOMSVILKÅRET)
        val startDato = 1 januar 2024
        val opprettet = LocalDateTime.now()
        Sykdomsvilkår(vilkårsresultat).vurder(
            SykdomsFaktagrunnlag(
                typeBehandling = TypeBehandling.Førstegangsbehandling,
                kravDato = startDato,
                sisteDagMedMuligYtelse = startDato.plusYears(3),
                yrkesskadevurdering = null,
                sykdomsvurderinger = listOf(
                    sykdomsvurdering(opprettet = opprettet, vurderingenGjelderFra = startDato),
                    sykdomsvurdering(
                        erNedsettelseIArbeidsevneMerEnnHalvparten = false,
                        vurderingenGjelderFra = startDato.plusWeeks(1),
                        opprettet = opprettet.plusSeconds(50)
                    )
                ),
                studentvurdering = null,
                sykepengerErstatningFaktagrunnlag = null,
                bistandvurderingFaktagrunnlag = bistandGrunnlag(startDato),
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

    private fun bistandGrunnlag(startDato: LocalDate): BistandGrunnlag = BistandGrunnlag(
        vurderinger = listOf(
            Bistandsvurdering(
                vurdertIBehandling = BehandlingId(1),
                begrunnelse = "bistand",
                erBehovForAktivBehandling = true,
                erBehovForArbeidsrettetTiltak = true,
                erBehovForAnnenOppfølging = true,
                overgangBegrunnelse = "...",
                skalVurdereAapIOvergangTilArbeid = null,
                vurdertAv = "Foffer",
                vurderingenGjelderFra = startDato,
                opprettet = Instant.now()
            )
        )
    )

    @Test
    fun `nei på viss varighet ved førstegangsbehandling`() {
        val vilkårsresultat = Vilkårsresultat()
        vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.SYKDOMSVILKÅRET)
        val startDato = 1 januar 2024
        val opprettet = LocalDateTime.now()
        Sykdomsvilkår(vilkårsresultat).vurder(
            SykdomsFaktagrunnlag(
                typeBehandling = TypeBehandling.Førstegangsbehandling,
                kravDato = startDato,
                sisteDagMedMuligYtelse = startDato.plusYears(3),
                yrkesskadevurdering = null,
                sykdomsvurderinger = listOf(
                    sykdomsvurdering(vurderingenGjelderFra = startDato, opprettet = opprettet),
                    sykdomsvurdering(
                        erNedsettelseIArbeidsevneAvEnVissVarighet = false,
                        vurderingenGjelderFra = startDato,
                        opprettet = opprettet.plusSeconds(50)
                    )
                ),
                studentvurdering = null,
                sykepengerErstatningFaktagrunnlag = null,
                bistandvurderingFaktagrunnlag = bistandGrunnlag(startDato),
            )
        )

        val vilkår = vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)

        assertThat(vilkår.vilkårsperioder()).hasSize(1)

        vilkår.tidslinje().assertTidslinje(
            Segment(Periode(1 januar 2024, 1 januar 2027)) { vurdering ->
                assertThat(vurdering.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
            },
        )
    }

    @Test
    fun `Krever ikke svar på viss varighet ved revurdering`() {
        val vilkårsresultat = Vilkårsresultat()
        vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.SYKDOMSVILKÅRET)
        val startDato = 1 januar 2024
        val opprettet = LocalDateTime.now()
        Sykdomsvilkår(vilkårsresultat).vurder(
            SykdomsFaktagrunnlag(
                typeBehandling = TypeBehandling.Revurdering,
                kravDato = startDato,
                sisteDagMedMuligYtelse = startDato.plusYears(3),
                yrkesskadevurdering = null,
                sykdomsvurderinger = listOf(
                    sykdomsvurdering(opprettet = opprettet, vurderingenGjelderFra = startDato),
                    sykdomsvurdering(
                        erNedsettelseIArbeidsevneAvEnVissVarighet = null,
                        vurderingenGjelderFra = startDato.plusWeeks(1),
                        opprettet = opprettet.plusSeconds(50)
                    )
                ),
                studentvurdering = null,
                sykepengerErstatningFaktagrunnlag = null,
                bistandvurderingFaktagrunnlag = bistandGrunnlag(startDato)
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

    @Test
    fun `hvis revurdering av førstegangsvurdering, så skal viss varigheten vurderes`() {
        val vilkårsresultat = Vilkårsresultat()
        vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.SYKDOMSVILKÅRET)
        val startDato = 1 januar 2024
        val opprettet = LocalDateTime.now()

        Sykdomsvilkår(vilkårsresultat).vurder(
            SykdomsFaktagrunnlag(
                typeBehandling = TypeBehandling.Revurdering,
                kravDato = startDato,
                sisteDagMedMuligYtelse = startDato.plusYears(3),
                yrkesskadevurdering = null,
                sykdomsvurderinger = listOf(
                    sykdomsvurdering(opprettet = opprettet, vurderingenGjelderFra = startDato),
                    sykdomsvurdering(
                        erNedsettelseIArbeidsevneAvEnVissVarighet = false,
                        vurderingenGjelderFra = startDato,
                        opprettet = opprettet.plusSeconds(50)
                    )
                ),
                studentvurdering = null,
                bistandvurderingFaktagrunnlag = null,
                sykepengerErstatningFaktagrunnlag =
                    SykepengerErstatningGrunnlag(
                        vurderinger = listOf(
                            SykepengerVurdering(
                                begrunnelse = "",
                                dokumenterBruktIVurdering = emptyList(),
                                harRettPå = true,
                                grunn = SykepengerGrunn.SYKEPENGER_IGJEN_ARBEIDSUFOR,
                                vurdertAv = "abc123",
                                vurdertTidspunkt = LocalDateTime.now(),
                                vurdertIBehandling = BehandlingId(1L),
                                gjelderFra = LocalDate.now()
                            )
                        )
                    ),
            )
        )

        val vilkår = vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)

        assertThat(vilkår.vilkårsperioder()).hasSize(1)

        vilkår.tidslinje().assertTidslinje(
            Segment(Periode(1 januar 2024, 1 januar 2027)) { vurdering ->
                assertThat(vurdering.utfall).isEqualTo(Utfall.OPPFYLT)
                assertThat(vurdering.innvilgelsesårsak).isEqualTo(Innvilgelsesårsak.SYKEPENGEERSTATNING)
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
        vurderingenGjelderFra: LocalDate,
        vurderingenGjelderTil: LocalDate? = null,
        opprettet: LocalDateTime = LocalDateTime.now(),
        behandlingId: BehandlingId = BehandlingId(1L)
    ) = Sykdomsvurdering(
        begrunnelse = "",
        dokumenterBruktIVurdering = emptyList(),
        harSkadeSykdomEllerLyte = harSkadeSykdomEllerLyte,
        erSkadeSykdomEllerLyteVesentligdel = erSkadeSykdomEllerLyteVesentligdel,
        erNedsettelseIArbeidsevneMerEnnHalvparten = erNedsettelseIArbeidsevneMerEnnHalvparten,
        erNedsettelseIArbeidsevneAvEnVissVarighet = erNedsettelseIArbeidsevneAvEnVissVarighet,
        erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense,
        erArbeidsevnenNedsatt = erArbeidsevnenNedsatt,
        yrkesskadeBegrunnelse = null,
        vurderingenGjelderFra = vurderingenGjelderFra,
        vurderingenGjelderTil = vurderingenGjelderTil,
        vurdertAv = Bruker("Z00000"),
        opprettet = opprettet.toInstant(ZoneOffset.UTC),
        vurdertIBehandling = behandlingId
    )
}
