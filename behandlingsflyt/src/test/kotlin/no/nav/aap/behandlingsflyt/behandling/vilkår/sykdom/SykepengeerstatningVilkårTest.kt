package no.nav.aap.behandlingsflyt.behandling.vilkår.sykdom

import no.nav.aap.vilkårsresultat.Utfall
import no.nav.aap.sykdom.ArbeidsevneNedsattValg
import no.nav.aap.sykdom.SykdomGrunnlag
import no.nav.aap.sykdom.Sykdomsvurdering
import no.nav.aap.sykepengererstatning.SykepengerErstatningGrunnlag
import no.nav.aap.sykepengererstatning.SykepengerGrunn
import no.nav.aap.sykepengererstatning.SykepengerVurdering
import no.nav.aap.behandlingsflyt.help.assertTidslinje
import no.nav.aap.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.test.januar
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.sykepengererstatning.SykepengeerstatningVilkår
import no.nav.aap.sykepengererstatning.SykepengerErstatningFaktagrunnlag
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

class SykepengeerstatningVilkårTest {
    
    @Test
    fun `hvis revurdering av førstegangsvurdering, så skal viss varigheten vurderes`() {
        val startDato = 1 januar 2024
        val opprettet = LocalDateTime.now()

        val resultat = SykepengeerstatningVilkår.vurder(
            SykepengerErstatningFaktagrunnlag(
                rettighetsperiode = Periode(startDato, startDato.plusYears(3)),
                sykdomGrunnlag = SykdomGrunnlag(
                    yrkesskadevurdering = null, sykdomsvurderinger = listOf(
                        sykdomsvurdering(opprettet = opprettet, vurderingenGjelderFra = startDato),
                        sykdomsvurdering(
                            harNedsattArbeidsevne = ArbeidsevneNedsattValg.JA_FORBIGÅENDE_PROBLEMER,
                            vurderingenGjelderFra = startDato,
                            opprettet = opprettet.plusSeconds(50)
                        )
                    )
                ),
                sykepengeerstatningGrunnlag =
                    SykepengerErstatningGrunnlag(
                        vurderinger = listOf(
                            SykepengerVurdering(
                                begrunnelse = "",
                                harRettPå = true,
                                grunn = SykepengerGrunn.SYKEPENGER_IGJEN_ARBEIDSUFOR,
                                vurdertAv = Bruker("abc123"),
                                vurdertTidspunkt = LocalDateTime.now(),
                                vurdertIBehandling = BehandlingId(1L),
                                fom = startDato
                            )
                        )
                    ),
            )
        )

        resultat.assertTidslinje(
            Segment(Periode(1 januar 2024, 1 januar 2027)) { vurdering ->
                assertThat(vurdering.utfall).isEqualTo(Utfall.OPPFYLT)
                assertThat(vurdering.innvilgelsesårsak).isEqualTo(null)
            },
        )
    }

    @Test
    fun `overlapp feil`() {
        val startDato = 1 januar 2024
        val opprettet = LocalDateTime.now()

        val rettighetsperiode = Periode(startDato, startDato.plusYears(3))
        val grunnlag = SykepengerErstatningFaktagrunnlag(
            rettighetsperiode = rettighetsperiode,
            sykdomGrunnlag = SykdomGrunnlag(
                yrkesskadevurdering = null, sykdomsvurderinger = listOf(
                    sykdomsvurdering(opprettet = opprettet, vurderingenGjelderFra = startDato),
                    sykdomsvurdering(
                        harNedsattArbeidsevne = ArbeidsevneNedsattValg.JA_FORBIGÅENDE_PROBLEMER,
                        vurderingenGjelderFra = startDato,
                        opprettet = opprettet.plusSeconds(50)
                    )
                )
            ),
            sykepengeerstatningGrunnlag =
                SykepengerErstatningGrunnlag(
                    vurderinger = listOf(
                        SykepengerVurdering(
                            begrunnelse = "",
                            harRettPå = true,
                            grunn = SykepengerGrunn.SYKEPENGER_IGJEN_ARBEIDSUFOR,
                            vurdertAv = Bruker("abc123"),
                            vurdertTidspunkt = LocalDateTime.now(),
                            vurdertIBehandling = BehandlingId(1L),
                            fom = startDato,
                        ),
                        SykepengerVurdering(
                            begrunnelse = "",
                            harRettPå = true,
                            grunn = SykepengerGrunn.SYKEPENGER_IGJEN_ARBEIDSUFOR,
                            vurdertAv = Bruker("abc123"),
                            vurdertTidspunkt = LocalDateTime.now(),
                            vurdertIBehandling = BehandlingId(1L),
                            fom = startDato.plusDays(10),
                        )
                    )
                ),
        )
        grunnlag.sykepengeerstatningGrunnlag?.somTidslinje(startDato, rettighetsperiode.tom)
        val resultat = SykepengeerstatningVilkår.vurder(grunnlag)

        resultat.assertTidslinje(
            Segment(Periode(1 januar 2024, 10 januar 2024)) { vurdering ->
                assertThat(vurdering.utfall).isEqualTo(Utfall.OPPFYLT)
                assertThat(vurdering.innvilgelsesårsak).isEqualTo(null)
            },
            Segment(Periode(11 januar 2024, 1 januar 2027)) { vurdering ->
                assertThat(vurdering.utfall).isEqualTo(Utfall.OPPFYLT)
                assertThat(vurdering.innvilgelsesårsak).isEqualTo(null)
            },
        )
    }

    private fun sykdomsvurdering(
        harSkadeSykdomEllerLyte: Boolean = true,
        erSkadeSykdomEllerLyteVesentligdel: Boolean = true,
        erNedsettelseIArbeidsevneMerEnnHalvparten: Boolean = true,
        erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense: Boolean = true,
        harNedsattArbeidsevne: ArbeidsevneNedsattValg = ArbeidsevneNedsattValg.JA,
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