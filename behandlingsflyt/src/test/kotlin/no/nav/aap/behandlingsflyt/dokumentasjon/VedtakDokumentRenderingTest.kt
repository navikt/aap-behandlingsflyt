package no.nav.aap.behandlingsflyt.dokumentasjon

import io.mockk.every
import io.mockk.mockk
import no.nav.aap.behandlingsflyt.behandling.beregning.beregnGrunnlagYrkesskade
import no.nav.aap.behandlingsflyt.behandling.vilkår.innsikt.DOM
import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakId
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Beregningsgrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Grunnlag11_19
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagInntekt
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagUføre
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Avslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårtype
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.ArbeidsevneNedsattValg
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.YrkesskadeSak
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Yrkesskadevurdering
import no.nav.aap.behandlingsflyt.kontrakt.behandling.BehandlingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingMedVedtak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.ÅrsakTilOpprettelse
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.type.Periode as DomenePeriode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year

class VedtakDokumentRenderingTest {
    private val behandlingId = BehandlingId(42)
    private val behandling = mockk<Behandling>(relaxed = true) {
        every { id } returns behandlingId
        every { referanse } returns BehandlingReferanse()
        every { opprettetTidspunkt } returns LocalDateTime.of(2024, 1, 1, 12, 0)
        every { vurderingsbehov() } returns emptyList()
        every { årsakTilOpprettelse } returns ÅrsakTilOpprettelse.SØKNAD
    }
    private val behandlingMedVedtak = BehandlingMedVedtak(
        saksnummer = Saksnummer("1234567890"),
        id = behandlingId,
        forrigeBehandlingId = null,
        referanse = BehandlingReferanse(),
        typeBehandling = TypeBehandling.Førstegangsbehandling,
        status = Status.AVSLUTTET,
        opprettetTidspunkt = LocalDateTime.of(2024, 1, 1, 12, 0),
        vedtakId = VedtakId(1),
        vedtakstidspunkt = LocalDateTime.of(2024, 1, 2, 12, 0),
        virkningstidspunkt = null,
        vurderingsbehov = emptySet(),
        årsakTilOpprettelse = ÅrsakTilOpprettelse.SØKNAD,
    )

    private fun grunnlag11_19() = Grunnlag11_19(
        grunnlaget = GUnit(BigDecimal("3.5")),
        erGjennomsnitt = true,
        gjennomsnittligInntektIG = GUnit(BigDecimal("3.5")),
        inntekter = listOf(
            GrunnlagInntekt(
                år = Year.of(2023),
                inntektIKroner = Beløp(450000),
                grunnbeløp = Beløp(118620),
                inntektIG = GUnit(BigDecimal("3.79")),
                inntekt6GBegrenset = GUnit(BigDecimal("3.79")),
                er6GBegrenset = false,
            )
        ),
    )

    private fun grunnlagUføre() = GrunnlagUføre(
        grunnlaget = GUnit(BigDecimal("4.2")),
        type = GrunnlagUføre.Type.YTTERLIGERE_NEDSATT,
        grunnlag = grunnlag11_19(),
        grunnlagYtterligereNedsatt = Grunnlag11_19(
            grunnlaget = GUnit(BigDecimal("4.2")),
            erGjennomsnitt = true,
            gjennomsnittligInntektIG = GUnit(BigDecimal("4.2")),
            inntekter = listOf(
                GrunnlagInntekt(
                    år = Year.of(2020),
                    inntektIKroner = Beløp(420000),
                    grunnbeløp = Beløp(101351),
                    inntektIG = GUnit(BigDecimal("4.14")),
                    inntekt6GBegrenset = GUnit(BigDecimal("4.14")),
                    er6GBegrenset = false,
                ),
                GrunnlagInntekt(
                    år = Year.of(2021),
                    inntektIKroner = Beløp(450000),
                    grunnbeløp = Beløp(106399),
                    inntektIG = GUnit(BigDecimal("4.23")),
                    inntekt6GBegrenset = GUnit(BigDecimal("4.23")),
                    er6GBegrenset = false,
                ),
                GrunnlagInntekt(
                    år = Year.of(2022),
                    inntektIKroner = Beløp(480000),
                    grunnbeløp = Beløp(111477),
                    inntektIG = GUnit(BigDecimal("4.31")),
                    inntekt6GBegrenset = GUnit(BigDecimal("4.31")),
                    er6GBegrenset = false,
                ),
            ),
        ),
        uføreInntekterFraForegåendeÅr = emptyList(),
        uføreYtterligereNedsattArbeidsevneÅr = Year.of(2023),
        uføregrader = emptySet(),
    )

    private fun grunnlag(
        beregningsgrunnlag: Beregningsgrunnlag? = grunnlag11_19(),
        sykdomGrunnlag: SykdomGrunnlag? = null,
        refusjonkrav: List<RefusjonkravVurdering>? = null,
        vilkårsresultat: Vilkårsresultat = Vilkårsresultat(),
    ) = VedtakDokumentGrunnlag(
        saksnummer = behandlingMedVedtak.saksnummer,
        behandling = behandling,
        behandlinger = listOf(behandlingMedVedtak),
        vilkårsresultat = vilkårsresultat,
        tilkjentYtelse = Tidslinje.empty(),
        underveis = Tidslinje.empty(),
        mottatteDokumenter = emptyList(),
        beregningsgrunnlag = beregningsgrunnlag,
        forrigeTilkjentYtelse = Tidslinje.empty(),
        forrigeUnderveis = Tidslinje.empty(),
        forrigeVilkårsresultat = Vilkårsresultat(),
        sykdomGrunnlag = sykdomGrunnlag,
        bistandGrunnlag = null,
        studentGrunnlag = null,
        overgangUføreGrunnlag = null,
        etableringEgenVirksomhetGrunnlag = null,
        arbeidsevneGrunnlag = null,
        arbeidsopptrappingGrunnlag = null,
        overgangArbeidGrunnlag = null,
        vedtakslengdeGrunnlag = null,
        meldepliktGrunnlag = null,
        stønadsperiodeGrunnlag = null,
        barnetilleggGrunnlag = null,
        samordningGrunnlag = null,
        rettighetstypeGrunnlag = null,
        institusjonsoppholdGrunnlag = null,
        sykepengerErstatningGrunnlag = null,
        refusjonkravVurderinger = refusjonkrav,
        overstyringMeldepliktGrunnlag = null,
        manuellInntektGrunnlag = null,
        beregningVurderingGrunnlag = null,
        forutgåendeMedlemskapGrunnlag = null,
        oppholdskravGrunnlag = null,
    )

    private fun VedtakDokumentGrunnlag.render() =
        VedtakDokumentRenderer.render(this).body

    @Test
    fun `genererer dokument uten valgfrie grunnlag`() {
        val dokument = VedtakDokumentRenderer.render(grunnlag(beregningsgrunnlag = null))
        assertThat(dokument.tittel)
            .isEqualTo("Oppsummering av vilkårsvurderinger for sak ${behandlingMedVedtak.saksnummer}")
        assertThat(dokument.body).isNotEmpty()
        assertThat(dokument.body.filterIsInstance<DOM.Avsnitt>().map { it.avsnitt })
            .anyMatch { "ikke tilgjengelig" in it }
    }

    @Test
    fun `viser beregningsgrunnlaget`() {
        val lister = grunnlag().render().filterIsInstance<DOM.List>().flatMap { it.liste }

        assertThat(lister.map { it.first() })
            .anyMatch { "2023" in it }
            .anyMatch { "Endelig grunnlag" in it }
    }

    @Test
    fun `viser beregningsalternativer for uføregrunnlag`() {
        val rader = grunnlag(beregningsgrunnlag = grunnlagUføre())
            .render()
            .filterIsInstance<DOM.List>()
            .flatMap { it.liste }

        assertThat(rader.map { it.first() })
            .contains(
                "Gjennomsnitt inntekt siste 3 år etter §§ 11-19 / 11-28 (2020 - 2022)",
                "Inntekt siste år etter §§ 11-19 / 11-28 (2022)",
            )
    }

    @Test
    fun `viser grunnlag med yrkesskadefordel`() {
        val yrkesskadeGrunnlag = beregnGrunnlagYrkesskade(
            grunnlag11_19 = grunnlag11_19(),
            antattÅrligInntekt = InntektPerÅr(Year.of(2023), Beløp(600000)),
            andelAvNedsettelsenSomSkyldesYrkesskaden = Prosent(70),
        )
        val rader = grunnlag(beregningsgrunnlag = yrkesskadeGrunnlag)
            .render()
            .filterIsInstance<DOM.List>()
            .flatMap { it.liste }

        assertThat(rader.map { it.first() })
            .contains("Grunnlag med yrkesskadefordel (§§ 11-19 / 11-22)")
    }

    @Test
    fun `viser uføreberegning og yrkesskadefordel sammen`() {
        val yrkesskadeUføreGrunnlag = beregnGrunnlagYrkesskade(
            grunnlag11_19 = grunnlagUføre(),
            antattÅrligInntekt = InntektPerÅr(Year.of(2023), Beløp(600000)),
            andelAvNedsettelsenSomSkyldesYrkesskaden = Prosent(70),
        )
        val rader = grunnlag(beregningsgrunnlag = yrkesskadeUføreGrunnlag)
            .render()
            .filterIsInstance<DOM.List>()
            .flatMap { it.liste }

        assertThat(rader.map { it.first() })
            .contains(
                "Gjennomsnitt inntekt siste 3 år etter §§ 11-19 / 11-28 (2020 - 2022)",
                "Inntekt siste år etter §§ 11-19 / 11-28 (2022)",
                "Grunnlag med yrkesskadefordel (§§ 11-19 / 11-22)",
            )
    }

    private fun sykdomsvurdering(vurdertIBehandling: BehandlingId) =
        Sykdomsvurdering(
            begrunnelse = "Klar sykdom",
            vurderingenGjelderFra = LocalDate.of(2024, 1, 1),
            vurderingenGjelderTil = null,
            harSkadeSykdomEllerLyte = true,
            erSkadeSykdomEllerLyteVesentligdel = true,
            erNedsettelseIArbeidsevneMerEnnHalvparten = true,
            erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense = null,
            yrkesskadeBegrunnelse = null,
            harNedsattArbeidsevne = ArbeidsevneNedsattValg.JA,
            diagnose = null,
            vurdertAv = Bruker("Z999999"),
            vurdertIBehandling = vurdertIBehandling,
            opprettet = Instant.now(),
        )

    @Test
    fun `viser sykdomsvurderingen`() {
        val dom = grunnlag(
            sykdomGrunnlag = SykdomGrunnlag(null, listOf(sykdomsvurdering(behandlingId)))
        ).render()

        assertThat(dom.filterIsInstance<DOM.Header>().map { it.overskrift })
            .contains("Vurderinger av § 11-5")
            .anyMatch { it.endsWith("(nåværende behandling)") }
        assertThat(dom.filterIsInstance<DOM.List>().flatMap { it.liste })
            .contains(listOf("Begrunnelse", "Klar sykdom"))
        assertThat(dom.filterIsInstance<DOM.List>().flatMap { it.liste }.flatten())
            .doesNotContain("Vurdert av", "Z999999")
    }

    @Test
    fun `viser yrkesskade og refusjonskrav`() {
        val yrkesskade = Yrkesskadevurdering(
            begrunnelse = "Klar yrkesskade",
            relevanteSaker = listOf(YrkesskadeSak("YS-123", null)),
            erÅrsakssammenheng = true,
            andelAvNedsettelsen = Prosent(80),
            vurdertAv = Bruker("Z111111"),
        )
        val dom = grunnlag(
            sykdomGrunnlag = SykdomGrunnlag(yrkesskade, emptyList()),
            refusjonkrav = listOf(
                RefusjonkravVurdering(
                    harKrav = true,
                    navKontor = "Oslo",
                    vurdertAv = Bruker("Z333333"),
                )
            ),
        ).render()
        val overskrifter = dom.filterIsInstance<DOM.Header>().map { it.overskrift }

        assertThat(overskrifter).contains("Yrkesskadevurdering", "Refusjonskrav")
    }

    @Test
    fun `viser alle vurderte vilkårstyper`() {
        val periode = DomenePeriode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31))
        val vilkårsresultat = Vilkårsresultat(
            vilkår = Vilkårtype.entries.map { type ->
                Vilkår(
                    type = type,
                    vilkårsperioder = setOf(
                        Vilkårsperiode(
                            periode = periode,
                            utfall = Utfall.OPPFYLT,
                            begrunnelse = "Vilkåret er oppfylt",
                        )
                    ),
                )
            }
        )
        val overskrifter = VedtakDokumentRenderer
            .render(grunnlag(vilkårsresultat = vilkårsresultat))
            .body
            .filterIsInstance<DOM.Header>()
            .map { it.overskrift }

        val kontekst = RenderKontekst(behandlingId, listOf(behandlingMedVedtak))
        val forventedeVilkårsoverskrifter = Vilkårtype.entries.map { type ->
            "${PrettyEnum(type).render(kontekst)} (${type.hjemmel})"
        }
        assertThat(overskrifter).containsAll(forventedeVilkårsoverskrifter)
    }

    @Test
    fun `viser alle perioder og vurderingsdetaljer for vilkår`() {
        val oppfyltPeriode = DomenePeriode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31))
        val avslåttPeriode = DomenePeriode(LocalDate.of(2024, 2, 1), LocalDate.of(2024, 2, 29))
        val vilkårsresultat = Vilkårsresultat(
            vilkår = listOf(
                Vilkår(
                    type = Vilkårtype.ALDERSVILKÅRET,
                    vilkårsperioder = setOf(
                        Vilkårsperiode(
                            periode = oppfyltPeriode,
                            utfall = Utfall.OPPFYLT,
                            manuellVurdering = true,
                            begrunnelse = "Aldersvilkåret er oppfylt",
                        ),
                        Vilkårsperiode(
                            periode = avslåttPeriode,
                            utfall = Utfall.IKKE_OPPFYLT,
                            begrunnelse = "Brukeren har fylt 67 år",
                            avslagsårsak = Avslagsårsak.BRUKER_OVER_67,
                        ),
                    ),
                )
            )
        )
        val vurderinger = VedtakDokumentRenderer
            .render(grunnlag(vilkårsresultat = vilkårsresultat))
            .body
            .filterIsInstance<DOM.List>()
            .flatMap { it.liste }
            .associate { it[0] to it[1] }

        val kontekst = RenderKontekst(behandlingId, listOf(behandlingMedVedtak))
        assertThat(vurderinger[Periode(oppfyltPeriode).render(kontekst)])
            .contains("Utfall: OPPFYLT")
            .contains("Vurderingsmåte: Manuell")
            .contains("Begrunnelse: Aldersvilkåret er oppfylt")
        assertThat(vurderinger[Periode(avslåttPeriode).render(kontekst)])
            .contains("Utfall: IKKE_OPPFYLT")
            .contains("Vurderingsmåte: Maskinell")
            .contains("Avslagsårsak: BRUKER_OVER_67, § 11-4 1. ledd")
            .contains("Begrunnelse: Brukeren har fylt 67 år")
    }
}
