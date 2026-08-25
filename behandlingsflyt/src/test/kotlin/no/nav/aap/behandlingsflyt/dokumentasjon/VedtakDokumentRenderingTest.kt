package no.nav.aap.behandlingsflyt.dokumentasjon

import no.nav.aap.behandlingsflyt.behandling.avslag11_27.Avslag11_27Grunnlag
import no.nav.aap.behandlingsflyt.behandling.avslag11_27.Avslag11_27Vurdering
import no.nav.aap.behandlingsflyt.behandling.beregning.beregnGrunnlagYrkesskade
import no.nav.aap.behandlingsflyt.behandling.lovvalg.MedlemskapArbeidInntektGrunnlag
import no.nav.aap.behandlingsflyt.behandling.samordning.Ytelse
import no.nav.aap.behandlingsflyt.behandling.vilkår.innsikt.DOM
import no.nav.aap.behandlingsflyt.behandling.vilkår.medlemskap.EØSLandEllerLandMedAvtale
import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakId
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7Grunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Aktivitetsplikt11_7Vurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.aktivitetsplikt.Utfall as AktivitetspliktUtfall
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
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.LovvalgDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.ManuellVurderingForLovvalgMedlemskap
import no.nav.aap.behandlingsflyt.faktagrunnlag.lovvalgmedlemskap.MedlemskapDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.InntektsbortfallVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Kravreferanse
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.sykestipend.SykestipendGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.sykestipend.SykestipendVurdering
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
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
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
import java.util.UUID

class VedtakDokumentRenderingTest {
    @Test
    fun `genererer dokument uten valgfrie grunnlag`() {
        val dokument = VedtakDokumentRenderer.render(grunnlag(beregningsgrunnlag = null))
        assertThat(dokument.tittel)
            .isEqualTo(
                "Oppsummering av vilkårsvurderinger for sak ${behandlingMedVedtak.saksnummer} – 02. januar 2024"
            )
        assertThat(dokument.body).isNotEmpty()
        assertThat(dokument.body.filterIsInstance<DOM.Avsnitt>().map { it.avsnitt })
            .anyMatch { "ikke tilgjengelig" in it }
    }

    @Test
    fun `viser beregningsgrunnlaget`() {
        val lister = grunnlag().render().filterIsInstance<DOM.List>().flatMap { it.liste }

        assertThat(lister.map { it.first() })
            .anyMatch { "2023" in it }
            .contains("Grunnlag § 11-19")
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
                "Grunnlag § 11-19",
                "Grunnlag § 11-28",
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
    fun `viser manuell vurdering av lovvalg og medlemskap`() {
        val lovvalgMedlemskapGrunnlag = MedlemskapArbeidInntektGrunnlag(
            medlemskapGrunnlag = null,
            inntekterINorgeGrunnlag = emptyList(),
            arbeiderINorgeGrunnlag = emptyList(),
            vurderinger = listOf(
                ManuellVurderingForLovvalgMedlemskap(
                    lovvalg = LovvalgDto(
                        begrunnelse = "Sverige er lovvalgsland",
                        lovvalgsEØSLandEllerLandMedAvtale = EØSLandEllerLandMedAvtale.SWE,
                    ),
                    medlemskap = MedlemskapDto(
                        begrunnelse = "Ikke medlem i norsk folketrygd",
                        varMedlemIFolketrygd = false,
                    ),
                    vurdertAv = Bruker("Z111111"),
                    vurdertDato = LocalDateTime.of(2024, 1, 1, 12, 0),
                    overstyrt = true,
                    fom = LocalDate.of(2024, 1, 1),
                    tom = LocalDate.of(2024, 1, 31),
                    vurdertIBehandling = behandlingId,
                )
            ),
        )

        val dom = grunnlag(lovvalgMedlemskapGrunnlag = lovvalgMedlemskapGrunnlag).render()
        val overskrifter = dom.filterIsInstance<DOM.Header>().map { it.overskrift }
        val felter = dom.filterIsInstance<DOM.List>().flatMap { it.liste }

        assertThat(overskrifter)
            .contains("Lovvalg og medlemskap (§ 2)")
            .anyMatch { it.contains("01. januar 2024 – 31. januar 2024") }
        assertThat(felter)
            .contains(
                listOf("Begrunnelse for lovvalg", "Sverige er lovvalgsland"),
                listOf("Lovvalgsland", "SWE"),
                listOf("Medlem i folketrygden", "nei"),
                listOf("Overstyrt", "ja"),
                listOf("Begrunnelse for medlemskap", "Ikke medlem i norsk folketrygd"),
            )
        assertThat(felter.flatten()).doesNotContain("Z111111")
    }

    @Test
    fun `viser manuell vurdering av annen full ytelse`() {
        val kravreferanse = Kravreferanse(UUID.randomUUID())
        val avslag11_27Grunnlag = Avslag11_27Grunnlag(
            setOf(
                Avslag11_27Vurdering(
                    referanse = kravreferanse,
                    vurdertIBehandling = behandlingId,
                    opprettet = Instant.parse("2024-01-01T12:00:00Z"),
                    vurdertAv = Bruker("Z111111"),
                    begrunnelse = "Brukeren mottar fulle sykepenger",
                    harAnnenFullYtelse = true,
                    brukersYtelse = Ytelse.SYKEPENGER,
                    brukersYtelseTom = LocalDate.of(2024, 2, 29),
                    harSykepengegrunnlagOver2G = true,
                    harArbeidsgiverSykepengerUtbetaling = false,
                    skalAvslås1127 = true,
                )
            )
        )

        val dom = grunnlag(avslag11_27Grunnlag = avslag11_27Grunnlag).render()
        val overskrifter = dom.filterIsInstance<DOM.Header>().map { it.overskrift }
        val felter = dom.filterIsInstance<DOM.List>().flatMap { it.liste }

        assertThat(overskrifter).contains(
            "Vurdering av annen full ytelse (§ 11-27)",
            "Vurdering",
        )
        assertThat(felter)
            .contains(
                listOf("Begrunnelse", "Brukeren mottar fulle sykepenger"),
                listOf("Har annen full ytelse", "ja"),
                listOf("Ytelse", "Sykepenger"),
                listOf("Ytelse til og med", "29. februar 2024"),
                listOf("Sykepengegrunnlag over 2 G", "ja"),
                listOf("Arbeidsgiver utbetaler sykepenger", "nei"),
                listOf("Skal avslås etter § 11-27", "ja"),
            )
        assertThat(felter.flatten()).doesNotContain("Z111111")
    }

    @Test
    fun `viser manuell vurdering av sykestipend`() {
        val sykestipendGrunnlag = SykestipendGrunnlag(
            SykestipendVurdering(
                begrunnelse = "Brukeren mottar sykestipend i januar",
                perioder = setOf(
                    DomenePeriode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31))
                ),
                vurdertIBehandling = behandlingId,
                vurdertAv = Bruker("Z111111"),
                opprettet = Instant.parse("2024-01-01T12:00:00Z"),
            )
        )

        val dom = grunnlag(sykestipendGrunnlag = sykestipendGrunnlag).render()
        val overskrifter = dom.filterIsInstance<DOM.Header>().map { it.overskrift }
        val felter = dom.filterIsInstance<DOM.List>().flatMap { it.liste }
        val perioder = dom.filterIsInstance<DOM.Tabell>()
            .single {
                it.kolonner == listOf("Perioder med sykestipend")
            }

        assertThat(overskrifter).contains("Sykestipend (§ 11-29)", "Vurdering")
        assertThat(felter).contains(
            listOf("Begrunnelse", "Brukeren mottar sykestipend i januar"),
            listOf("Mottar sykestipend", "ja"),
        )
        assertThat(perioder.rader).containsExactly(
            listOf("01.01.2024 – 31.01.2024")
        )
        assertThat(felter.flatten()).doesNotContain("Z111111")
    }

    @Test
    fun `viser manuell vurdering av inntektsbortfall`() {
        val vurdering = InntektsbortfallVurdering(
            begrunnelse = "Brukeren har rett til fullt uttak av alderspensjon",
            rettTilUttak = true,
            vurdertAv = Bruker("Z111111"),
            vurdertIBehandling = behandlingId,
            opprettetTid = LocalDateTime.of(2024, 1, 1, 12, 0),
        )

        val dom = grunnlag(inntektsbortfallVurdering = vurdering).render()
        val overskrifter = dom.filterIsInstance<DOM.Header>().map { it.overskrift }
        val felter = dom.filterIsInstance<DOM.List>().flatMap { it.liste }

        assertThat(overskrifter).contains("Inntektsbortfall (§ 11-4 andre ledd)", "Vurdering")
        assertThat(felter).contains(
            listOf("Begrunnelse", "Brukeren har rett til fullt uttak av alderspensjon"),
            listOf("Rett til fullt uttak av alderspensjon", "ja"),
        )
        assertThat(felter.flatten()).doesNotContain("Z111111")
    }

    @Test
    fun `viser manuell vurdering av aktivitetsplikt`() {
        val aktivitetspliktGrunnlag = Aktivitetsplikt11_7Grunnlag(
            vurderinger = listOf(
                Aktivitetsplikt11_7Vurdering(
                    begrunnelse = "Aktivitetsplikten er brutt",
                    erOppfylt = false,
                    utfall = AktivitetspliktUtfall.STANS,
                    vurdertAv = Bruker("Z111111"),
                    fom = LocalDate.of(2024, 1, 1),
                    opprettet = Instant.parse("2024-01-01T12:00:00Z"),
                    vurdertIBehandling = behandlingId,
                    skalIgnorereVarselFrist = true,
                )
            )
        )

        val dom = grunnlag(aktivitetsplikt11_7Grunnlag = aktivitetspliktGrunnlag).render()
        val overskrifter = dom.filterIsInstance<DOM.Header>().map { it.overskrift }
        val vurderinger = dom.filterIsInstance<DOM.Tabell>()
            .single {
                it.kolonner == listOf(
                    "Periode (fom – tom)",
                    "Oppfylt",
                    "Utfall",
                    "Varselfrist skal ignoreres",
                    "Begrunnelse",
                )
            }

        assertThat(overskrifter).contains("Aktivitetsplikt (§ 11-7)")
        assertThat(vurderinger.rader).containsExactly(
            listOf(
                "01.01.2024 – ∞",
                "nei",
                "Stans",
                "ja",
                "Aktivitetsplikten er brutt",
            )
        )
        assertThat(vurderinger.rader.flatten()).doesNotContain("Z111111")
    }

    @Test
    fun `viser studentvurdering`() {
        val studentGrunnlag = StudentGrunnlag(
            vurderinger = setOf(
                StudentVurdering(
                    fom = LocalDate.of(2024, 1, 1),
                    tom = LocalDate.of(2024, 6, 30),
                    begrunnelse = "Studiet ble avbrutt på grunn av sykdom",
                    harAvbruttStudie = true,
                    godkjentStudieAvLånekassen = true,
                    avbruttPgaSykdomEllerSkade = true,
                    harBehovForBehandling = true,
                    avbruttStudieDato = LocalDate.of(2024, 1, 15),
                    avbruddMerEnn6Måneder = true,
                    vurdertAv = Bruker("Z111111"),
                    vurdertTidspunkt = LocalDateTime.of(2024, 1, 16, 12, 0),
                    vurdertIBehandling = behandlingId,
                )
            ),
            oppgittStudent = null,
        )

        val dom = grunnlag(studentGrunnlag = studentGrunnlag).render()
        val overskrifter = dom.filterIsInstance<DOM.Header>().map { it.overskrift }
        val felter = dom.filterIsInstance<DOM.List>().flatMap { it.liste }

        assertThat(overskrifter)
            .contains("Student (§ 11-14)")
            .anyMatch {
                it.contains("01. januar 2024 – 30. juni 2024") &&
                    it.endsWith("(nåværende behandling)")
            }
        assertThat(felter).contains(
            listOf("Dato for avbrutt studie", "15. januar 2024"),
            listOf("Begrunnelse", "Studiet ble avbrutt på grunn av sykdom"),
        )
        assertThat(felter.flatten()).doesNotContain("Z111111")
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

    private val behandlingId = BehandlingId(42)
    private val behandlingReferanse = BehandlingReferanse()
    private val behandling = Behandling(
        id = behandlingId,
        forrigeBehandlingId = null,
        referanse = behandlingReferanse,
        sakId = SakId(1),
        typeBehandling = TypeBehandling.Førstegangsbehandling,
        status = Status.AVSLUTTET,
        vurderingsbehov = emptyList(),
        årsakTilOpprettelse = ÅrsakTilOpprettelse.SØKNAD,
        opprettetTidspunkt = LocalDateTime.of(2024, 1, 1, 12, 0),
        versjon = 1,
    )

    private val behandlingMedVedtak = BehandlingMedVedtak(
        saksnummer = Saksnummer("1234567890"),
        id = behandlingId,
        forrigeBehandlingId = null,
        referanse = behandlingReferanse,
        typeBehandling = TypeBehandling.Førstegangsbehandling,
        status = Status.AVSLUTTET,
        opprettetTidspunkt = LocalDateTime.of(2024, 1, 1, 12, 0),
        vedtakId = VedtakId(1),
        vedtakstidspunkt = LocalDateTime.of(2024, 1, 2, 12, 0),
        virkningstidspunkt = null,
        vurderingsbehov = emptySet(),
        årsakTilOpprettelse = ÅrsakTilOpprettelse.SØKNAD,
    )

    private fun grunnlag(
        beregningsgrunnlag: Beregningsgrunnlag? = grunnlag11_19(),
        sykdomGrunnlag: SykdomGrunnlag? = null,
        refusjonkrav: List<RefusjonkravVurdering>? = null,
        vilkårsresultat: Vilkårsresultat = Vilkårsresultat(),
        behandlinger: List<BehandlingMedVedtak> = listOf(behandlingMedVedtak),
        lovvalgMedlemskapGrunnlag: MedlemskapArbeidInntektGrunnlag? = null,
        avslag11_27Grunnlag: Avslag11_27Grunnlag? = null,
        sykestipendGrunnlag: SykestipendGrunnlag? = null,
        inntektsbortfallVurdering: InntektsbortfallVurdering? = null,
        aktivitetsplikt11_7Grunnlag: Aktivitetsplikt11_7Grunnlag? = null,
        studentGrunnlag: StudentGrunnlag? = null,
    ) = VedtakDokumentGrunnlag(
        saksnummer = behandlingMedVedtak.saksnummer,
        behandling = behandling,
        behandlinger = behandlinger,
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
        studentGrunnlag = studentGrunnlag,
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
        avslag11_27Grunnlag = avslag11_27Grunnlag,
        sykestipendGrunnlag = sykestipendGrunnlag,
        inntektsbortfallVurdering = inntektsbortfallVurdering,
        aktivitetsplikt11_7Grunnlag = aktivitetsplikt11_7Grunnlag,
        overstyringMeldepliktGrunnlag = null,
        manuellInntektGrunnlag = null,
        beregningVurderingGrunnlag = null,
        lovvalgMedlemskapGrunnlag = lovvalgMedlemskapGrunnlag,
        forutgåendeMedlemskapGrunnlag = null,
        oppholdskravGrunnlag = null,
    )

    private fun VedtakDokumentGrunnlag.render() =
        VedtakDokumentRenderer.render(this).body

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
}
