package no.nav.aap.behandlingsflyt.behandling.vilkår.sykdom

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Innvilgelsesårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.Bistandsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.ArbeidsevneNedsattValg
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Yrkesskadevurdering
import no.nav.aap.behandlingsflyt.help.assertTidslinje
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.test.desember
import no.nav.aap.behandlingsflyt.test.februar
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.behandlingsflyt.test.mars
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.komponenter.verdityper.Prosent
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
                kravDato = kravdato,
                sisteDagMedMuligYtelse = kravdato.plusYears(3),
                yrkesskadevurdering = null,
                sykdomsvurderinger = listOf(
                    sykdomsvurdering(
                        vurderingenGjelderFra = kravdato,
                        harSkadeSykdomEllerLyte = true,
                        harNedsattArbeidsevne = ArbeidsevneNedsattValg.JA,
                        erSkadeSykdomEllerLyteVesentligdel = true,
                        erNedsettelseIArbeidsevneMerEnnHalvparten = true
                    )
                ),
                sykepengerErstatningFaktagrunnlag = null,
                bistandvurderingFaktagrunnlag = bistandGrunnlag(LocalDate.now()),
                sykepengeerstatningVilkår = Tidslinje()
            )
        )
        val vilkår = vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)

        assertThat(vilkår.vilkårsperioder()).hasSize(1).allMatch { periode -> periode.utfall == Utfall.OPPFYLT }

        val faktagrunnlag = SykdomsFaktagrunnlag(
            kravDato = kravdato,
            sisteDagMedMuligYtelse = kravdato.plusYears(3),
            yrkesskadevurdering = null,
            sykdomsvurderinger = listOf(
                sykdomsvurdering(
                    vurderingenGjelderFra = kravdato,
                    harSkadeSykdomEllerLyte = true,
                    harNedsattArbeidsevne = ArbeidsevneNedsattValg.JA,
                    erSkadeSykdomEllerLyteVesentligdel = true,
                    erNedsettelseIArbeidsevneMerEnnHalvparten = false
                )
            ),
            sykepengerErstatningFaktagrunnlag = null,
            bistandvurderingFaktagrunnlag = null,
            sykepengeerstatningVilkår = Tidslinje()
        )
        Sykdomsvilkår(vilkårsresultat).vurder(faktagrunnlag)

        assertThat(vilkår.vilkårsperioder()).hasSize(1).allMatch { periode -> periode.utfall == Utfall.IKKE_OPPFYLT }
    }

    private fun bistandGrunnlag(
        startDato: LocalDate,
        sluttDato: LocalDate? = null,
        erBehovForAktivBehandling: Boolean = true,
        erBehovForArbeidsrettetTiltak: Boolean = true,
        erBehovForAnnenOppfølging: Boolean = true,
    ): BistandGrunnlag = BistandGrunnlag(
        vurderinger = listOf(
            Bistandsvurdering(
                vurdertIBehandling = BehandlingId(1),
                begrunnelse = "bistand",
                erBehovForAktivBehandling = erBehovForAktivBehandling,
                erBehovForArbeidsrettetTiltak = erBehovForArbeidsrettetTiltak,
                erBehovForAnnenOppfølging = erBehovForAnnenOppfølging,
                overgangBegrunnelse = "...",
                skalVurdereAapIOvergangTilArbeid = null,
                vurdertAv = "Foffer",
                vurderingenGjelderFra = startDato,
                opprettet = Instant.now(),
                tom = sluttDato
            )
        )
    )

    @Test
    fun `Ordinær etterfulgt av forbigående problemer`() {
        val vilkårsresultat = Vilkårsresultat()
        vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.SYKDOMSVILKÅRET)
        val startDato = 1 januar 2024
        val opprettet = LocalDateTime.now()

        val faktagrunnlag =
            SykdomsFaktagrunnlag(
                kravDato = startDato,
                sisteDagMedMuligYtelse = startDato.plusYears(3),
                yrkesskadevurdering = null,
                sykdomsvurderinger = listOf(
                    sykdomsvurdering(
                        opprettet = opprettet,
                        harSkadeSykdomEllerLyte = true,
                        harNedsattArbeidsevne = ArbeidsevneNedsattValg.JA,
                        erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                        erSkadeSykdomEllerLyteVesentligdel = true,
                        vurderingenGjelderFra = startDato
                    ),
                    sykdomsvurdering(
                        harSkadeSykdomEllerLyte = true,
                        harNedsattArbeidsevne = ArbeidsevneNedsattValg.JA_FORBIGÅENDE_PROBLEMER,
                        vurderingenGjelderFra = startDato.plusWeeks(1),
                        erNedsettelseIArbeidsevneMerEnnHalvparten = true,
                        erSkadeSykdomEllerLyteVesentligdel = true,
                        opprettet = opprettet.plusSeconds(50)
                    )
                ),
                sykepengerErstatningFaktagrunnlag = null,
                bistandvurderingFaktagrunnlag = bistandGrunnlag(startDato),
                sykepengeerstatningVilkår = Tidslinje()
            )

        Sykdomsvilkår(vilkårsresultat).vurder(
            faktagrunnlag
        )

        val vilkår = vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)

        assertThat(vilkår.vilkårsperioder()).hasSize(2)

        vilkår.tidslinje().assertTidslinje(
            Segment(Periode(1 januar 2024, 7 januar 2024)) { vurdering ->
                assertThat(vurdering.utfall).isEqualTo(Utfall.OPPFYLT)
            },
            Segment(Periode(8 januar 2024, 1 januar 2027)) { vurdering ->
                assertThat(vurdering.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
                assertThat(vurdering.avslagsårsak).isEqualTo(Avslagsårsak.IKKE_SYKDOM_AV_VISS_VARIGHET)
            },
        )
    }

    @Test
    fun `Skal oppfylles for yrkesskade dersom nedsettelse over yrkesskadegrense og årsakssammenheng`() {
        val vilkårsresultat = Vilkårsresultat()
        vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.SYKDOMSVILKÅRET)
        val startDato = 1 januar 2024
        val opprettet = LocalDateTime.now()

        val faktagrunnlag =
            SykdomsFaktagrunnlag(
                kravDato = startDato,
                sisteDagMedMuligYtelse = startDato.plusYears(3),
                yrkesskadevurdering = Yrkesskadevurdering(
                    begrunnelse = "",
                    relevanteSaker = emptyList(),
                    erÅrsakssammenheng = true,
                    andelAvNedsettelsen = Prosent(50),
                    vurdertAv = "Z00000",
                    vurdertTidspunkt = opprettet
                ),
                sykdomsvurderinger = listOf(
                    sykdomsvurdering(
                        opprettet = opprettet, vurderingenGjelderFra = startDato,
                        harSkadeSykdomEllerLyte = true,
                        harNedsattArbeidsevne = ArbeidsevneNedsattValg.JA,
                        erSkadeSykdomEllerLyteVesentligdel = true,
                        erNedsettelseIArbeidsevneMerEnnHalvparten = false,
                        erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = true
                    ),
                    sykdomsvurdering(
                        // Nedsatt arbeidsevne nei
                        harSkadeSykdomEllerLyte = true,
                        harNedsattArbeidsevne = ArbeidsevneNedsattValg.JA,
                        erNedsettelseIArbeidsevneMerEnnHalvparten = false,
                        erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = false,
                        vurderingenGjelderFra = startDato.plusYears(1),
                        opprettet = opprettet.plusSeconds(50)
                    )
                ),
                sykepengerErstatningFaktagrunnlag = null,
                bistandvurderingFaktagrunnlag = bistandGrunnlag(startDato),
                sykepengeerstatningVilkår = Tidslinje()
            )

        Sykdomsvilkår(vilkårsresultat).vurder(
            faktagrunnlag
        )

        val vilkår = vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)

        assertThat(vilkår.vilkårsperioder()).hasSize(2)

        vilkår.tidslinje().assertTidslinje(
            Segment(Periode(1 januar 2024, 31 desember 2024)) { vurdering ->
                assertThat(vurdering.utfall).isEqualTo(Utfall.OPPFYLT)
                assertThat(vurdering.innvilgelsesårsak).isEqualTo(Innvilgelsesårsak.YRKESSKADE_ÅRSAKSSAMMENHENG)
            },
            Segment(Periode(1 januar 2025, 1 januar 2027)) { vurdering ->
                assertThat(vurdering.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
                assertThat(vurdering.avslagsårsak).isEqualTo(Avslagsårsak.IKKE_NOK_REDUSERT_ARBEIDSEVNE)
            }
        )
    }

    @Test
    fun `Ikke syk, ikke vesentlig del, ikke nedstatt arbeidsevne skal gi avslag`() {
        val vilkårsresultat = Vilkårsresultat()
        vilkårsresultat.leggTilHvisIkkeEksisterer(Vilkårtype.SYKDOMSVILKÅRET)
        val startDato = 1 januar 2024
        val opprettet = LocalDateTime.now()

        val faktagrunnlag =
            SykdomsFaktagrunnlag(
                kravDato = startDato,
                sisteDagMedMuligYtelse = startDato.plusYears(3),
                yrkesskadevurdering = Yrkesskadevurdering(
                    begrunnelse = "",
                    relevanteSaker = emptyList(),
                    erÅrsakssammenheng = true,
                    andelAvNedsettelsen = Prosent(50),
                    vurdertAv = "Z00000",
                    vurdertTidspunkt = opprettet
                ),
                sykdomsvurderinger = listOf(
                    sykdomsvurdering(
                        harSkadeSykdomEllerLyte = true,
                        harNedsattArbeidsevne = ArbeidsevneNedsattValg.JA,
                        erSkadeSykdomEllerLyteVesentligdel = false,
                        opprettet = opprettet, vurderingenGjelderFra = startDato,
                    ),
                    sykdomsvurdering(
                        harSkadeSykdomEllerLyte = true,
                        harNedsattArbeidsevne = ArbeidsevneNedsattValg.NEI,
                        vurderingenGjelderFra = startDato.plusMonths(1),
                        opprettet = opprettet.plusSeconds(50)
                    ),
                    sykdomsvurdering(
                        harSkadeSykdomEllerLyte = false,
                        opprettet = opprettet.plusSeconds(100),
                        vurderingenGjelderFra = startDato.plusMonths(2),
                    ),
                ),
                sykepengerErstatningFaktagrunnlag = null,
                bistandvurderingFaktagrunnlag = bistandGrunnlag(startDato),
                sykepengeerstatningVilkår = Tidslinje()
            )

        Sykdomsvilkår(vilkårsresultat).vurder(
            faktagrunnlag
        )

        val vilkår = vilkårsresultat.finnVilkår(Vilkårtype.SYKDOMSVILKÅRET)

        assertThat(vilkår.vilkårsperioder()).hasSize(3)

        vilkår.tidslinje().assertTidslinje(
            Segment(Periode(1 januar 2024, 31 januar 2024)) { vurdering ->
                assertThat(vurdering.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
                assertThat(vurdering.avslagsårsak).isEqualTo(Avslagsårsak.IKKE_SYKDOM_SKADE_LYTE_VESENTLIGDEL)
            },
            Segment(Periode(1 februar 2024, 29 februar 2024)) { vurdering ->
                assertThat(vurdering.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
                assertThat(vurdering.avslagsårsak).isEqualTo(Avslagsårsak.IKKE_NOK_REDUSERT_ARBEIDSEVNE)
            },
            Segment(Periode(1 mars 2024, 1 januar 2027)) { vurdering ->
                assertThat(vurdering.utfall).isEqualTo(Utfall.IKKE_OPPFYLT)
                assertThat(vurdering.avslagsårsak).isEqualTo(Avslagsårsak.IKKE_SYKDOM_SKADE_LYTE)
            }
        )
    }

    private fun sykdomsvurdering(
        harSkadeSykdomEllerLyte: Boolean,
        erSkadeSykdomEllerLyteVesentligdel: Boolean? = null,
        erNedsettelseIArbeidsevneMerEnnHalvparten: Boolean? = null,
        erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense: Boolean? = null,
        harNedsattArbeidsevne: ArbeidsevneNedsattValg? = null,
        vurderingenGjelderFra: LocalDate,
        vurderingenGjelderTil: LocalDate? = null,
        opprettet: LocalDateTime = LocalDateTime.now(),
        behandlingId: BehandlingId = BehandlingId(1L)
    ) = Sykdomsvurdering(
        begrunnelse = "",
        harSkadeSykdomEllerLyte = harSkadeSykdomEllerLyte,
        erSkadeSykdomEllerLyteVesentligdel = erSkadeSykdomEllerLyteVesentligdel,
        erNedsettelseIArbeidsevneMerEnnHalvparten = erNedsettelseIArbeidsevneMerEnnHalvparten,
        erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense,
        harNedsattArbeidsevne = harNedsattArbeidsevne,
        yrkesskadeBegrunnelse = null,
        vurderingenGjelderFra = vurderingenGjelderFra,
        vurderingenGjelderTil = vurderingenGjelderTil,
        vurdertAv = Bruker("Z00000"),
        opprettet = opprettet.toInstant(ZoneOffset.UTC),
        vurdertIBehandling = behandlingId,
        diagnose = null
    )
}
