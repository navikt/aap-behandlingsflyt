package no.nav.aap.behandlingsflyt.dokumentasjon

import no.nav.aap.behandlingsflyt.behandling.lovvalg.ForutgåendeMedlemskapArbeidInntektGrunnlag
import no.nav.aap.behandlingsflyt.behandling.vilkår.innsikt.Dokument
import no.nav.aap.behandlingsflyt.behandling.oppholdskrav.OppholdskravGrunnlag
import no.nav.aap.behandlingsflyt.behandling.oppholdskrav.OppholdskravGrunnlagRepository
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.Tilkjent
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.tilTidslinje
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.tilTidslinje
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Beregningsgrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.BeregningsgrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Grunnlag11_19
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagInntekt
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagUføre
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagYrkesskade
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.UføreInntekt
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.rettighetstype.RettighetstypeGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.rettighetstype.RettighetstypeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.SamordningGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.SamordningRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.institusjonsopphold.InstitusjonsoppholdRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.ManuellInntektGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.ManuellInntektGrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.medlemskap.MedlemskapArbeidInntektForutgåendeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsopptrapping.ArbeidsopptrappingGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsopptrapping.ArbeidsopptrappingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurderingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringEgenVirksomhetGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringEgenVirksomhetRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.gjeldendeVurderinger
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.OverstyringMeldepliktGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.OverstyringMeldepliktRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangarbeid.OvergangArbeidGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangarbeid.OvergangArbeidRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.overgangufore.OvergangUføreRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.refusjonkrav.RefusjonkravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode.StønadsperiodeGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode.StønadsperiodeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode.RelevantKravType
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.student.StudentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerErstatningGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerErstatningRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde.VedtakslengdeGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde.VedtakslengdeRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingMedVedtak
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakId
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.utils.Diff
import no.nav.aap.behandlingsflyt.utils.Endret
import no.nav.aap.behandlingsflyt.utils.Fjernet
import no.nav.aap.behandlingsflyt.utils.LagtTil
import no.nav.aap.behandlingsflyt.utils.Uendret
import no.nav.aap.behandlingsflyt.utils.diffTidslinjer
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDateTime
import no.nav.aap.komponenter.type.Periode as DomenePeriode

data class BehandlingFaktagrunnlag(
    val behandling: Behandling,
    val behandlinger: List<BehandlingMedVedtak>,
    val vilkårsresultat: Vilkårsresultat,
    val tilkjentYtelse: Tidslinje<Tilkjent>,
    val underveis: Tidslinje<Underveisperiode>,
    val mottatteDokumenter: List<MottattDokument>,
    val beregningsgrunnlag: Beregningsgrunnlag?,

    val forrigeTilkjentYtelse: Tidslinje<Tilkjent>,
    val forrigeUnderveis: Tidslinje<Underveisperiode>,
    val forrigeVilkårsresultat: Vilkårsresultat,

    val sykdomGrunnlag: SykdomGrunnlag?,
    val bistandGrunnlag: BistandGrunnlag?,
    val studentGrunnlag: StudentGrunnlag?,
    val overgangUføreGrunnlag: OvergangUføreGrunnlag?,
    val etableringEgenVirksomhetGrunnlag: EtableringEgenVirksomhetGrunnlag?,
    val arbeidsevneGrunnlag: ArbeidsevneGrunnlag?,
    val arbeidsopptrappingGrunnlag: ArbeidsopptrappingGrunnlag?,
    val overgangArbeidGrunnlag: OvergangArbeidGrunnlag?,
    val vedtakslengdeGrunnlag: VedtakslengdeGrunnlag?,
    val meldepliktGrunnlag: MeldepliktGrunnlag?,
    val stønadsperiodeGrunnlag: StønadsperiodeGrunnlag?,
    val barnetilleggGrunnlag: BarnetilleggGrunnlag?,
    val samordningGrunnlag: SamordningGrunnlag?,
    val rettighetstypeGrunnlag: RettighetstypeGrunnlag?,
    val institusjonsoppholdGrunnlag: InstitusjonsoppholdGrunnlag?,
    val sykepengerErstatningGrunnlag: SykepengerErstatningGrunnlag?,
    val refusjonkravVurderinger: List<RefusjonkravVurdering>?,
    val overstyringMeldepliktGrunnlag: OverstyringMeldepliktGrunnlag?,
    val manuellInntektGrunnlag: ManuellInntektGrunnlag?,
    val beregningVurderingGrunnlag: BeregningGrunnlag?,
    val forutgåendeMedlemskapGrunnlag: ForutgåendeMedlemskapArbeidInntektGrunnlag?,
    val oppholdskravGrunnlag: OppholdskravGrunnlag?,
) {
    fun genererDokument(): Dokument {
        val kontekst = RenderKontekst(behandlinger)
        return Dokument(
            tittel = "Et vedtak ",
            header = "En header",
            body = tilSeksjon().render(kontekst),
        )
    }

    fun tilSeksjon(): Seksjon {
        return Seksjon(
            tittel = Tekst("Vedtak"),
            subseksjoner = listOfNotNull(
                opplysningerOmBehandlingen(),
                grunnlaget(),
                rettighetstype(),
                sykdomsvurderinger(),
                yrkesskadevurdering(),
                sykepengererstatning(),
                bistandsvurderinger(),
                studentvurderinger(),
                overgangUføre(),
                etableringEgenVirksomhet(),
                arbeidsevnevurderinger(),
                arbeidsopptrapping(),
                overgangArbeid(),
                vedtakslengde(),
                fritak(),
                overstyringMeldeplikt(),
                stønadsperiode(),
                barnetillegg(),
                refusjonkrav(),
                samordning(),
                institusjonsopphold(),
                forutgåendeMedlemskap(),
                oppholdskrav(),
                manuellInntekt(),
                beregningVurdering(),
                vilkår(),
                tilkjentYtelse(),
                vedleggTidligereBehandlinger(),
                vedleggDokumentoversikt(),
            )
        )
    }

    private fun grunnlaget(): Seksjon? {
        if (beregningsgrunnlag == null) return Seksjon(
            tittel = Tekst("Grunnlaget for størrelsen på AAP"),
            Avsnitt(Tekst("Beregningsgrunnlag er ikke tilgjengelig for denne behandlingen."))
        )
        return Seksjon(
            "Grunnlaget for størrelsen på AAP",
            Dict(
                when (beregningsgrunnlag) {
                    is Grunnlag11_19 -> grunnlag11_19Rader(beregningsgrunnlag)
                    is GrunnlagUføre -> grunnlag11_19Rader(beregningsgrunnlag.underliggende()) + listOf(
                        Tekst("Grunnlag §11-19 (standard)") to G(beregningsgrunnlag.underliggende().grunnlaget()),
                        Tekst("Grunnlag §11-19 (ytterligere nedsatt)") to G(beregningsgrunnlag.underliggendeYtterligereNedsatt().grunnlaget()),
                        Tekst("Type beregning") to PrettyEnum(beregningsgrunnlag.type()),
                        Tekst("Endelig grunnlag (etter §11-28)") to G(beregningsgrunnlag.grunnlaget()),
                    ) + beregningsgrunnlag.uføreInntekterFraForegåendeÅr().map { uføreInntektRad(it) }

                    is GrunnlagYrkesskade -> grunnlag11_19RaderForYrkesskade(beregningsgrunnlag) + listOf(
                        Tekst("Yrkesskadeprosent") to Prosent(beregningsgrunnlag.andelYrkesskade()),
                        Tekst("Benyttet yrkesskadeandel") to Prosent(beregningsgrunnlag.benyttetAndelForYrkesskade()),
                        Tekst("Terskelverdi yrkesskade") to Prosent(beregningsgrunnlag.terskelverdiForYrkesskade()),
                        Tekst("Inntekt på yrkesskadetidspunktet (kr)") to Kroner(beregningsgrunnlag.antattÅrligInntektYrkesskadeTidspunktet()),
                        Tekst("Yrkesskadeinntekt (G)") to G(beregningsgrunnlag.yrkesskadeinntektIG()),
                        Tekst("Grunnbeløp på yrkesskadetidspunktet") to Kroner(beregningsgrunnlag.grunnbeløp()),
                        Tekst("Andel som skyldes yrkesskade (G)") to G(beregningsgrunnlag.andelSomSkyldesYrkesskade()),
                        Tekst("Andel som ikke skyldes yrkesskade (G)") to G(beregningsgrunnlag.andelSomIkkeSkyldesYrkesskade()),
                        Tekst("Endelig grunnlag (G)") to G(beregningsgrunnlag.grunnlaget()),
                    )
                }
            )
        )
    }

    private fun grunnlag11_19Rader(g: Grunnlag11_19): List<Pair<LøpendeTekst, LøpendeTekst>> =
        g.inntekter().map<GrunnlagInntekt, Pair<LøpendeTekst, LøpendeTekst>> {
            Tekst("År ${it.år}") to Span(
                Tekst("Inntekt: "), Kroner(it.inntektIKroner), Tekst(" ("), G(it.inntektIG), Tekst(")."),
                Tekst(" Grunnbeløp: "), Kroner(it.grunnbeløp), Tekst("."),
                Tekst(" 6G-begrenset: "), G(it.inntekt6GBegrenset), Tekst(" ("),
                JaNeiValg(it.er6GBegrenset), Tekst(")."),
            )
        } + listOf(
            Tekst("Gjennomsnitt 3 år") to G(g.gjennomsnittligInntektIG()),
            Tekst("Gjennomsnitt valgt") to JaNeiValg(g.erGjennomsnitt()),
            Tekst("Endelig grunnlag") to G(g.grunnlaget()),
        )

    private fun grunnlag11_19RaderForYrkesskade(g: GrunnlagYrkesskade): List<Pair<LøpendeTekst, LøpendeTekst>> =
        when (val under = g.underliggende()) {
            is Grunnlag11_19 -> grunnlag11_19Rader(under)
            is GrunnlagUføre -> grunnlag11_19Rader(under.underliggende()) + listOf(
                Tekst("Grunnlag §11-28 (uføre)") to G(under.grunnlaget()),
            )
            is GrunnlagYrkesskade -> emptyList() // Ikke rekursjon i praksis
        }

    private fun uføreInntektRad(i: UføreInntekt): Pair<LøpendeTekst, LøpendeTekst> =
        Tekst("Uføreinntekt ${i.år}") to Span(
            Kroner(i.inntektIKroner), Tekst(" / justert: "), Kroner(i.inntektJustertForUføregrad),
        )

    private fun sykdomsvurderinger(): Seksjon? {
        val tidslinje = sykdomGrunnlag?.somSykdomsvurderingstidslinje() ?: return null
        if (tidslinje.isEmpty()) return null
        return tidslinje.tilSeksjon()
    }

    private fun bistandsvurderinger(): Seksjon? {
        val grunnlag = bistandGrunnlag ?: return null
        val tidslinje = grunnlag.somBistandsvurderingstidslinje()
        if (tidslinje.isEmpty()) return Seksjon(
            tittel = Tekst("Bistandsbehov (§ 11-6)"),
            Avsnitt(Tekst("Ingen bistandsvurderinger registrert."))
        )
        return Seksjon(
            tittel = Tekst("Bistandsbehov (§ 11-6)"),
            subseksjoner = tidslinje.segmenter().map { (periode, v) ->
                Seksjon(
                    tittel = vurderingsoverskrift(v.vurdertIBehandling, periode),
                    Fritekstfelt("Begrunnelse", v.begrunnelse),
                    Dict(
                        "Behov for aktiv behandling" to JaNeiValg(v.erBehovForAktivBehandling),
                        "Behov for arbeidsrettet tiltak" to JaNeiValg(v.erBehovForArbeidsrettetTiltak),
                        "Behov for annen oppfølging" to JaNeiValg(v.erBehovForAnnenOppfølging),
                        "Har bistandsbehov" to JaNeiValg(v.erBehovForBistand()),
                    )
                )
            }
        )
    }

    private fun studentvurderinger(): Seksjon? {
        val grunnlag = studentGrunnlag ?: return null
        val vurderinger = grunnlag.gjeldendeStudentvurderinger()
        if (vurderinger.isEmpty()) return null
        return Seksjon(
            tittel = Tekst("Student (§ 11-14)"),
            subseksjoner = vurderinger.map { v ->
                Seksjon(
                    tittel = Tekst("Vurdering"),
                    Dict(
                        "Avbrutt studie" to JaNeiValg(v.harAvbruttStudie),
                        "Godkjent av Lånekassen" to JaNeiValg(v.godkjentStudieAvLånekassen),
                        "Avbrutt pga sykdom/skade" to JaNeiValg(v.avbruttPgaSykdomEllerSkade),
                        "Avbrudd mer enn 6 måneder" to JaNeiValg(v.avbruddMerEnn6Måneder),
                        "Behov for behandling" to JaNeiValg(v.harBehovForBehandling),
                    ),
                    Fritekstfelt("Begrunnelse", v.begrunnelse),
                )
            }
        )
    }

    private fun overgangUføre(): Seksjon? {
        val grunnlag = overgangUføreGrunnlag ?: return null
        val tidslinje = grunnlag.somOvergangUforevurderingstidslinje()
        if (tidslinje.isEmpty()) return null
        return Seksjon(
            tittel = Tekst("Overgang til uføretrygd (§ 11-18)"),
            subseksjoner = tidslinje.segmenter().map { (periode, v) ->
                Seksjon(
                    tittel = vurderingsoverskrift(v.vurdertIBehandling, periode),
                    Fritekstfelt("Begrunnelse", v.begrunnelse),
                    Dict(
                        "Har søkt om uføretrygd" to JaNeiValg(v.brukerHarSøktOmUføretrygd),
                        "Fått vedtak om uføretrygd" to PrettyEnum(v.brukerHarFåttVedtakOmUføretrygd),
                        "Har rett på AAP" to JaNeiValg(v.brukerRettPåAAP),
                    )
                )
            }
        )
    }

    private fun etableringEgenVirksomhet(): Seksjon? {
        val grunnlag = etableringEgenVirksomhetGrunnlag ?: return null
        val tidslinje = grunnlag.gjeldendeVurderingerSomTidslinje()
        if (tidslinje.isEmpty()) return null
        return Seksjon(
            tittel = Tekst("Etablering av egen virksomhet"),
            subseksjoner = tidslinje.segmenter().map { (periode, v) ->
                Seksjon(
                    tittel = vurderingsoverskrift(v.vurdertIBehandling, periode),
                    Fritekstfelt("Begrunnelse", v.begrunnelse),
                    Dict(
                        "Virksomhetsnavn" to Tekst(v.virksomhetNavn),
                        "Org.nr." to Tekst(v.orgNr ?: "—"),
                        "Foreligger faglig vurdering" to JaNeiValg(v.foreliggerFagligVurdering),
                        "Virksomhet er ny" to JaNeiValg(v.virksomhetErNy),
                        "Kan føre til selvforsørging" to JaNeiValg(v.kanFøreTilSelvforsørget),
                    )
                )
            }
        )
    }

    private fun arbeidsevnevurderinger(): Seksjon? {
        val grunnlag = arbeidsevneGrunnlag ?: return null
        val tidslinje = grunnlag.gjeldendeVurderinger()
        if (tidslinje.isEmpty()) return null
        return Seksjon(
            tittel = Tekst("Arbeidsevne"),
            subseksjoner = tidslinje.segmenter().map { (periode, v) ->
                Seksjon(
                    tittel = vurderingsoverskrift(v.vurdertIBehandling, periode),
                    Fritekstfelt("Begrunnelse", v.begrunnelse),
                    Dict(
                        "Restarbeidsevne" to Prosent(v.arbeidsevne),
                    )
                )
            }
        )
    }

    private fun arbeidsopptrapping(): Seksjon? {
        val grunnlag = arbeidsopptrappingGrunnlag ?: return null
        val tidslinje = grunnlag.gjeldendeVurderinger()
        if (tidslinje.isEmpty()) return null
        return Seksjon(
            tittel = Tekst("Arbeidsopptrapping"),
            subseksjoner = tidslinje.segmenter().map { (periode, v) ->
                Seksjon(
                    tittel = vurderingsoverskrift(v.vurdertIBehandling, periode),
                    Fritekstfelt("Begrunnelse", v.begrunnelse),
                    Dict(
                        "Rett på AAP i opptrapping" to JaNeiValg(v.rettPaaAAPIOpptrapping),
                        "Reell mulighet til opptrapping" to JaNeiValg(v.reellMulighetTilOpptrapping),
                    )
                )
            }
        )
    }

    private fun overgangArbeid(): Seksjon? {
        val grunnlag = overgangArbeidGrunnlag ?: return null
        val tidslinje = grunnlag.gjeldendeVurderinger()
        if (tidslinje.isEmpty()) return null
        return Seksjon(
            tittel = Tekst("Overgang til arbeid (§ 11-17)"),
            subseksjoner = tidslinje.segmenter().map { (periode, v) ->
                Seksjon(
                    tittel = vurderingsoverskrift(v.vurdertIBehandling, periode),
                    Fritekstfelt("Begrunnelse", v.begrunnelse),
                    Dict(
                        "Rett på AAP" to JaNeiValg(v.brukerRettPåAAP),
                    )
                )
            }
        )
    }

    private fun vedtakslengde(): Seksjon? {
        val vurdering = vedtakslengdeGrunnlag?.gjeldendeVurdering() ?: return null
        return Seksjon(
            tittel = Tekst("Vedtakslengde"),
            Dict(
                "Sluttdato" to Dato(vurdering.sluttdato),
                "Utvidet med" to PrettyEnum(vurdering.utvidetMed),
                "Vurdert manuelt" to JaNeiValg(vurdering.vurdertManuelt),
            ),
            Fritekstfelt("Begrunnelse", vurdering.begrunnelse),
        )
    }

    private fun fritak(): Seksjon? {
        val grunnlag = meldepliktGrunnlag ?: return null
        val tidslinje = grunnlag.tilTidslinje()
        if (tidslinje.isEmpty()) return null
        return Seksjon(
            tittel = Tekst("Fritak fra meldeplikt"),
            Tabell.ofTidslinje(
                kolonner = listOf(Tekst("Har fritak"), Tekst("Begrunnelse")),
                tidslinje = tidslinje.map {
                    listOf(JaNeiValg(it.harFritak), Tekst(it.begrunnelse))
                }
            )
        )
    }

    private fun stønadsperiode(): Seksjon? {
        val grunnlag = stønadsperiodeGrunnlag ?: return null
        val vurderinger = grunnlag.gjeldendeVurderinger()
        if (vurderinger.isEmpty()) return null
        return Seksjon(
            tittel = Tekst("Stønadsperiode / rettighetsperiode"),
            subseksjoner = vurderinger.map { v ->
                Seksjon(
                    tittel = Tekst("Vurdering"),
                    Fritekstfelt("Begrunnelse", v.begrunnelse),
                    Dict(
                        "Relevant kravtype" to when (val kravtype = v.relevantKravType) {
                            RelevantKravType.NY_STØNADSPERIODE -> Tekst("Ny stønadsperiode")
                            RelevantKravType.GJENINNTREDEN_ETTER_OPPHØR -> Tekst("Gjeninntreden etter opphør")
                            RelevantKravType.AVSLAG -> Tekst("Avslag")
                            is RelevantKravType.GJENOPPTAK_ETTER_STANS -> Span(
                                Tekst("Gjenopptak etter stans"),
                                kravtype.gjennopptakEtter
                                    .takeIf { it.isNotEmpty() }
                                    ?.let { årsaker ->
                                        Span(
                                            Tekst(": "),
                                            årsaker.join { PrettyEnum(it) },
                                        )
                                    },
                            )
                        },
                        "Startdato" to Dato(v.startDato),
                        "Hatt ordinær siste 52 uker" to JaNeiValg(v.harHattOrdinærSiste52Uker),
                        "Gjenværende kvote" to JaNeiValg(v.harGjenværendeKvote),
                    )
                )
            }
        )
    }

    private fun barnetillegg(): Seksjon? {
        val perioder = barnetilleggGrunnlag?.perioder?.tilTidslinje() ?: return null
        if (perioder.isEmpty()) return null
        return Seksjon(
            tittel = Tekst("Barnetillegg"),
            Tabell.ofTidslinje(
                kolonner = listOf(Tekst("Antall barn med rett til barnetillegg")),
                tidslinje = perioder.map { listOf(Tekst(it.barnMedRettTil().size.toString())) }
            )
        )
    }

    private fun rettighetstype(): Seksjon? {
        val grunnlag = rettighetstypeGrunnlag ?: return null
        if (grunnlag.rettighetstypeTidslinje.isEmpty()) return null
        return Seksjon(
            tittel = Tekst("Rettighetstype"),
            Tabell.ofTidslinje(
                kolonner = listOf(Tekst("Rettighetstype"), Tekst("Hjemmel")),
                tidslinje = grunnlag.rettighetstypeTidslinje.map { rt ->
                    listOf(PrettyEnum(rt), Tekst(rt.hjemmel))
                },
            )
        )
    }

    private fun yrkesskadevurdering(): Seksjon? {
        val vurdering = sykdomGrunnlag?.yrkesskadevurdering ?: return null
        return Seksjon(
            tittel = Tekst("Yrkesskadevurdering"),
            Fritekstfelt("Begrunnelse", vurdering.begrunnelse),
            Dict(
                "Årsakssammenheng" to JaNeiValg(vurdering.erÅrsakssammenheng),
                "Andel av nedsettelsen" to (vurdering.andelAvNedsettelsen?.let { Prosent(it) } ?: Tekst("Ikke angitt")),
                "Relevante yrkesskadesaksnumre" to Tekst(
                    vurdering.relevanteSaker.joinToString(", ") { it.referanse }.ifEmpty { "—" }
                ),
            )
        )
    }

    private fun sykepengererstatning(): Seksjon? {
        val grunnlag = sykepengerErstatningGrunnlag ?: return null
        val tidslinje = grunnlag.vurderinger.gjeldendeVurderinger()
        if (tidslinje.isEmpty()) return null
        return Seksjon(
            tittel = Tekst("Sykepengererstatning (§ 11-13)"),
            subseksjoner = tidslinje.segmenter().map { (periode, v) ->
                Seksjon(
                    tittel = vurderingsoverskrift(v.vurdertIBehandling, periode),
                    Fritekstfelt("Begrunnelse", v.begrunnelse),
                    Dict(
                        "Har rett på sykepengererstatning" to JaNeiValg(v.harRettPå),
                        "Grunn" to PrettyEnum(v.grunn),
                        "Gjelder fra" to Dato(v.fom),
                        "Gjelder til" to (v.tom?.let { Dato(it) } ?: Tekst("Ikke satt")),
                    )
                )
            }
        )
    }

    private fun refusjonkrav(): Seksjon? {
        val vurderinger = refusjonkravVurderinger?.takeIf { it.isNotEmpty() } ?: return null
        return Seksjon(
            tittel = Tekst("Refusjonskrav"),
            subseksjoner = vurderinger.map { v ->
                Seksjon(
                    tittel = Tekst("Vurdering"),
                    Dict(
                        "Har krav" to JaNeiValg(v.harKrav),
                        "NAV-kontor" to Tekst(v.navKontor ?: "—"),
                        "Fra og med" to (v.fom?.let { Dato(it) } ?: Tekst("Ikke satt")),
                        "Til og med" to (v.tom?.let { Dato(it) } ?: Tekst("Ikke satt")),
                    )
                )
            }
        )
    }

    private fun overstyringMeldeplikt(): Seksjon? {
        val grunnlag = overstyringMeldepliktGrunnlag ?: return null
        val tidslinje = grunnlag.tilTidslinje()
        if (tidslinje.isEmpty()) return null
        return Seksjon(
            tittel = Tekst("Overstyring av meldeplikt"),
            Tabell.ofTidslinje(
                kolonner = listOf(Tekst("Status"), Tekst("Begrunnelse")),
                tidslinje = tidslinje.map { data ->
                    listOf(
                        PrettyEnum(data.meldepliktOverstyringStatus),
                        Tekst(data.begrunnelse),
                    )
                }
            )
        )
    }

    private fun samordning(): Seksjon? {
        val grunnlag = samordningGrunnlag ?: return null
        if (grunnlag.samordningPerioder.isEmpty()) return null
        return Seksjon(
            tittel = Tekst("Samordning"),
            subseksjoner = grunnlag.samordningPerioder.sortedBy { it.periode.fom }.map { p ->
                Seksjon(
                    tittel = Periode(p.periode),
                    Dict(
                        "Samordningsgradering" to Prosent(p.gradering),
                    )
                )
            }
        )
    }

    private fun institusjonsopphold(): Seksjon? {
        val grunnlag = institusjonsoppholdGrunnlag ?: return null
        val harData = grunnlag.oppholdene != null || grunnlag.soningsVurderinger != null || grunnlag.helseoppholdvurderinger != null
        if (!harData) return null

        return Seksjon(
            tittel = Tekst("Institusjonsopphold"),
            subseksjoner = listOfNotNull(
                grunnlag.oppholdene?.takeIf { it.opphold.isNotEmpty() }?.let { oppholdene ->
                    Seksjon(
                        tittel = Tekst("Registrerte opphold"),
                        Tabell.ofTidslinje(
                            kolonner = listOf(Tekst("Type"), Tekst("Kategori"), Tekst("Navn"), Tekst("Org.nr.")),
                            tidslinje = no.nav.aap.komponenter.tidslinje.Tidslinje(oppholdene.opphold).map { inst ->
                                listOf(
                                    Tekst(inst.type.beskrivelse),
                                    Tekst(inst.kategori.beskrivelse),
                                    Tekst(inst.navn),
                                    Tekst(inst.orgnr),
                                )
                            }
                        )
                    )
                },
                grunnlag.soningsVurderinger?.tilTidslinje()?.takeIf { !it.isEmpty() }?.let { tidslinje ->
                    Seksjon(
                        tittel = Tekst("Soningsvurderinger"),
                        Tabell.ofTidslinje(
                            kolonner = listOf(Tekst("Skal opphøre"), Tekst("Begrunnelse")),
                            tidslinje = tidslinje.map { v ->
                                listOf(JaNeiValg(v.skalOpphøre), Tekst(v.begrunnelse))
                            }
                        )
                    )
                },
                grunnlag.helseoppholdvurderinger?.tilTidslinje()?.takeIf { !it.isEmpty() }?.let { tidslinje ->
                    Seksjon(
                        tittel = Tekst("Helseinstitusjonsvurderinger"),
                        subseksjoner = tidslinje.segmenter().map { (_, v) ->
                            Seksjon(
                                tittel = Tekst("Vurdering"),
                                Fritekstfelt("Begrunnelse", v.begrunnelse),
                                Dict(
                                    "Får fri kost og losji" to JaNeiValg(v.faarFriKostOgLosji),
                                    "Forsørger ektefelle" to JaNeiValg(v.forsoergerEktefelle),
                                    "Har faste utgifter" to JaNeiValg(v.harFasteUtgifter),
                                )
                            )
                        }
                    )
                },
            )
        )
    }

    private fun forutgåendeMedlemskap(): Seksjon? {
        val grunnlag = forutgåendeMedlemskapGrunnlag ?: return null
        val tidslinje = grunnlag.gjeldendeVurderinger()
        if (tidslinje.isEmpty()) return null
        return Seksjon(
            tittel = Tekst("Forutgående medlemskap"),
            subseksjoner = tidslinje.segmenter().map { (periode, v) ->
                Seksjon(
                    tittel = vurderingsoverskrift(v.vurdertIBehandling, periode),
                    Fritekstfelt("Begrunnelse", v.begrunnelse),
                    Dict(
                        "Har forutgående medlemskap" to JaNeiValg(v.harForutgåendeMedlemskap),
                        "Var medlem med nedsatt arbeidsevne" to JaNeiValg(v.varMedlemMedNedsattArbeidsevne),
                        "Unntak fra maks 5 år" to JaNeiValg(v.medlemMedUnntakAvMaksFemAar),
                    )
                )
            }
        )
    }

    private fun oppholdskrav(): Seksjon? {
        val grunnlag = oppholdskravGrunnlag ?: return null
        val tidslinje = grunnlag.tidslinje()
        if (tidslinje.isEmpty()) return null
        return Seksjon(
            tittel = Tekst("Oppholdskrav"),
            Tabell.ofTidslinje(
                kolonner = listOf(Tekst("Land"), Tekst("Oppfylt"), Tekst("Begrunnelse")),
                tidslinje = tidslinje.map { data ->
                    listOf(
                        Tekst(data.land ?: "—"),
                        JaNeiValg(data.oppfylt),
                        Tekst(data.begrunnelse),
                    )
                }
            )
        )
    }

    private fun manuellInntekt(): Seksjon? {
        val grunnlag = manuellInntektGrunnlag ?: return null
        if (grunnlag.manuelleInntekter.isEmpty()) return null
        return Seksjon(
            tittel = Tekst("Manuell inntekt"),
            subseksjoner = grunnlag.manuelleInntekter.sortedBy { it.år }.map { v ->
                Seksjon(
                    tittel = Tekst("År ${v.år}"),
                    Fritekstfelt("Begrunnelse", v.begrunnelse),
                    Dict(
                        "Beløp (kr)" to (v.belop?.let { Kroner(it) } ?: Tekst("Ikke angitt")),
                        "EØS-beløp (kr)" to (v.eøsBeløp?.let { Kroner(it) } ?: Tekst("Ikke angitt")),
                        "Ferdiglignet PGI (kr)" to (v.ferdigLignetPGI?.let { Kroner(it) } ?: Tekst("Ikke angitt")),
                    )
                )
            }
        )
    }

    private fun beregningVurdering(): Seksjon? {
        val grunnlag = beregningVurderingGrunnlag ?: return null
        val tidspunkt = grunnlag.tidspunktVurdering
        val yrkesskade = grunnlag.yrkesskadeBeløpVurdering
        if (tidspunkt == null && yrkesskade == null) return null
        return Seksjon(
            tittel = Tekst("Beregningstidspunkt og yrkesskadebeløp"),
            subseksjoner = listOfNotNull(
                tidspunkt?.let { t ->
                    Seksjon(
                        tittel = Tekst("Beregningstidspunkt"),
                        Fritekstfelt("Begrunnelse", t.begrunnelse),
                        Dict(
                            "Dato for nedsatt arbeidsevne" to Dato(t.nedsattArbeidsevneEllerStudieevneDato),
                            "Årsak" to PrettyEnum(t.årsak),
                            "Ytterligere nedsatt begrunnelse" to Tekst(t.ytterligereNedsattBegrunnelse ?: "—"),
                            "Ytterligere nedsatt dato" to (t.ytterligereNedsattArbeidsevneDato?.let { Dato(it) } ?: Tekst("Ikke satt")),
                        )
                    )
                },
                yrkesskade?.takeIf { it.vurderinger.isNotEmpty() }?.let { y ->
                    Seksjon(
                        tittel = Tekst("Yrkesskadebeløp"),
                        subseksjoner = y.vurderinger.map { v ->
                            Seksjon(
                                tittel = Tekst("Sak ${v.referanse}"),
                                Fritekstfelt("Begrunnelse", v.begrunnelse),
                                Dict(
                                    "Antatt årlig inntekt (kr)" to Kroner(v.antattÅrligInntekt),
                                )
                            )
                        }
                    )
                },
            )
        )
    }

    private fun vilkår(): Seksjon = Seksjon(
        tittel = Tekst("Vilkårsvurderinger"),
        subseksjoner = vilkårsresultat.alle().mapNotNull { vilkår(it, forrigeVilkårsresultat.optionalVilkår(it.type)) }
    )

    private fun vilkår(vilkår: Vilkår, forrigeVilkår: Vilkår?): Seksjon? {
        val tittel = Span(PrettyEnum(vilkår.type), Tekst(" (${vilkår.type.hjemmel})"))
        if (vilkår.tidslinje().isEmpty()) {
            return Seksjon(tittel, Avsnitt(Tekst("Ingen vurderinger.")))
        }

        fun forDiff(vilkårsvurderinger: Tidslinje<Vilkårsvurdering>?) = vilkårsvurderinger.orEmpty().map {
            listOf(it.utfall, it.innvilgelsesårsak, it.avslagsårsak, it.manuellVurdering, it.begrunnelse)
        }.komprimer()

        fun vilkårsvurderingRad(
            periode: DomenePeriode,
            diff: Diff<Unit>?,
            gjeldende: Vilkårsvurdering?,
        ): Pair<LøpendeTekst, LøpendeTekst> =
            Periode(periode) to Span(
                listOfNotNull(
                    when (diff) {
                        is Endret<*> -> Tekst("Endret fra forrige behandling.")
                        is Fjernet<*> -> Tekst("Perioden er ikke lenger vurdert.")
                        is LagtTil<*> -> Tekst("Ny periode vurdert.")
                        is Uendret<*> -> Tekst("Perioden er uendret fra forrige behandling.")
                        null -> error("")
                    },
                    when (diff) {
                        is Endret<*>,
                        is LagtTil<*>,
                        is Uendret<*> ->
                            Span(
                                Tekst(" Utfall: ${gjeldende?.utfall}. Vurderingsmåte: "),
                                when (gjeldende?.manuellVurdering) {
                                    true -> Tekst("Manuell. ")
                                    false -> Tekst("Maskinell. ")
                                    null -> Tekst("Ikke tilgjengelig. ")
                                },
                                gjeldende?.innvilgelsesårsak?.let { Tekst("Innvilgelsesvariant: $it. ") },
                                gjeldende?.avslagsårsak?.let { Tekst("Avslagsårsak: $it, ${it.hjemmel}.") },
                                Tekst(" Begrunnelse: "), Tekst(gjeldende?.begrunnelse ?: "—"), Tekst("."),
                            )

                        is Fjernet<*> -> null
                    }
                )
            )
        return Seksjon(
            tittel = tittel,
            Dict(
                Tidslinje.zip2(
                    diffTidslinjer(forDiff(forrigeVilkår?.tidslinje()), forDiff(vilkår.tidslinje())).map { it.map { } },
                    vilkår.tidslinje().komprimer(),
                )
                    .segmenter()
                    .map { vilkårsvurderingRad(it.periode, it.verdi.first, it.verdi.second) }
            )
        )
    }


    private fun tilkjentYtelse(): Seksjon {
        fun tilkjent(tilkjent: Tilkjent) = listOf(
            Kroner(tilkjent.dagsats),
            Prosent(tilkjent.gradering),
            G(tilkjent.grunnlagsfaktor),
            Kroner(tilkjent.grunnbeløp),
            Tekst(tilkjent.antallBarn.toString()),
            Kroner(tilkjent.barnetilleggsats),
            Kroner(tilkjent.barnetillegg),
            PrettyEnum(tilkjent.minsteSats),
            Kroner(tilkjent.redusertDagsats()),
            Dato(tilkjent.utbetalingsdato),
            Kroner(tilkjent.barnepensjonDagsats),
            Prosent(tilkjent.graderingGrunnlag.samordningGradering),
            Prosent(tilkjent.graderingGrunnlag.institusjonGradering),
            Prosent(tilkjent.graderingGrunnlag.arbeidGradering),
            Prosent(tilkjent.graderingGrunnlag.samordningUføregradering),
            Prosent(tilkjent.graderingGrunnlag.samordningArbeidsgiverGradering),
            Prosent(tilkjent.graderingGrunnlag.meldepliktGradering),
        )

        val diff = diffTidslinjer(forrigeTilkjentYtelse.komprimer(), tilkjentYtelse.komprimer())
        val dagsatsDenneBehandling = tilkjentYtelse.map<List<LøpendeTekst>> {
            listOf(
                G(it.grunnlagsfaktor),
                Kroner(it.grunnbeløp),
                Kroner(it.dagsats)
            )
        }
            .komprimer()
        val dagsatsForrigeBehandling = forrigeTilkjentYtelse.map<List<LøpendeTekst>> {
            listOf(
                G(it.grunnlagsfaktor),
                Kroner(it.grunnbeløp),
                Kroner(it.dagsats)
            )
        }
            .komprimer()
        val diffDagsats = diffTidslinjer(dagsatsForrigeBehandling, dagsatsDenneBehandling)

        return Seksjon(
            tittel = Tekst("Tilkjent ytelse"),
            Seksjon(
                "Dagsats før gradering, før reduksjoner, og uten barnetillegg",
                Seksjon(
                    "Perioder endret i denne behandlingen",
                    if (diffDagsats.segmenter().all { it.verdi is Uendret<*> })
                        Avsnitt(
                            Tekst("Ingen endringer i dagsats fra forrige behandling")
                        )
                    else
                        Tabell.ofTidslinje(
                            kolonner = listOf(
                                Tekst("Dagsats (G)"),
                                Tekst("Grunnbeløp benyttet"),
                                Tekst("Dagsats (kroner)")
                            ),
                            tidslinje = diffDagsats.mapNotNull<List<LøpendeTekst>> {
                                when (it) {
                                    is Endret<List<LøpendeTekst>> ->it.fra.zip(it.til).map { (fra, til) ->
                                        if (fra == til) til else Span(fra, Tekst(" → "), til)
                                    }
                                    is Fjernet<List<LøpendeTekst>> -> it.fjernet.map { Span(it, Tekst(" → –")) }
                                    is LagtTil<List<LøpendeTekst>> -> it.lagtTil.map { Span(Tekst("– → "), it) }
                                    is Uendret<*> -> null
                                }
                            },
                        )
                ),
                Seksjon(
                    "Perioder uendret fra forrige behandling",
                    Tabell.ofTidslinje(
                        kolonner = listOf(
                            Tekst("Dagsats (G)"),
                            Tekst("Grunnbeløp benyttet"),
                            Tekst("Dagsats (kroner)")
                        ),
                        tidslinje = diffDagsats.mapNotNull { when (it) {
                            is Uendret<List<LøpendeTekst>> -> it.uendret
                            is Endret<*>,
                            is Fjernet<*>,
                            is LagtTil<*> -> null
                        }}
                    )
                ),
            ),
            Seksjon(
                "Barnetillegg før gradering og før reduksjoner",
                Tabell.ofTidslinje(
                    kolonner = listOf(
                        Tekst("Antall barn som gir barnetillegg"),
                        Tekst("Sats"),
                        Tekst("Sum barnetillegg")
                    ),
                    tidslinje = tilkjentYtelse.map {
                        listOf(
                            Tekst(it.antallBarn.toString()),
                            Kroner(it.barnetilleggsats),
                            Kroner(it.barnetillegg),
                        )
                    }.komprimer(),
                ),
            ),
            Seksjon(
                "Samordning, graderinger og reduksjoner",
                Tabell.ofTidslinje(
                    kolonner = listOf(
                        Tekst("Samordning misc"),
                        Tekst("Arbeid"),
                        Tekst("Uføre"),
                        Tekst("Arbeidsgiver"),
                        Tekst("Meldeplikt"),
                        Tekst("Insitusjon"),
                        Tekst("Endelig"),
                    ),
                    tidslinje =
                        tilkjentYtelse
                            .map<List<LøpendeTekst>> { tilkjent ->
                                listOf(
                                    Span(Tekst("-"), Prosent(tilkjent.graderingGrunnlag.samordningGradering)),
                                    Span(
                                        Tekst("-"),
                                        Prosent(tilkjent.graderingGrunnlag.arbeidGradering.komplement())
                                    ),
                                    Span(Tekst("-"), Prosent(tilkjent.graderingGrunnlag.samordningUføregradering)),
                                    Span(
                                        Tekst("-"),
                                        Prosent(tilkjent.graderingGrunnlag.samordningArbeidsgiverGradering)
                                    ),
                                    Span(Tekst("-"), Prosent(tilkjent.graderingGrunnlag.meldepliktGradering)),
                                    Span(
                                        Tekst("×"),
                                        Prosent(tilkjent.graderingGrunnlag.institusjonGradering.komplement())
                                    ),
                                    Prosent(tilkjent.gradering),
                                )
                            }
                            .komprimer()
                )
            ),
            Seksjon(
                "Dagsats",
                Tabell.ofTidslinje(
                    kolonner = listOf(
                        Tekst("Dagsats før gradering"),
                        Tekst("Barnetillegg før gradering"),
                        Tekst("Gradering"),
                        Tekst("Endelig dagsats"),
                    ),
                    tidslinje = tilkjentYtelse.map<List<LøpendeTekst>> {
                        listOf(
                            Kroner(it.dagsats),
                            Kroner(it.barnetillegg),
                            Prosent(it.gradering),
                            Kroner(it.redusertDagsats())
                        )
                    }.komprimer()
                )
            ),
        )
    }

    private fun opplysningerOmBehandlingen(): Seksjon = Seksjon(
        tittel = Tekst("Opplysninger om behandlingen"),
        blokker = listOf(
            Dict(
                "Referanse" to Tekst(behandling.referanse.toString()),
                "Opprettet" to Tidspunkt(behandling.opprettetTidspunkt),
                "Årsak til opprettelse" to PrettyEnum(behandling.årsakTilOpprettelse),
                "Vurderingsbehov" to
                        behandling.vurderingsbehov().join(separator = " ") {
                            Span(
                                PrettyEnum(it.type),
                                Tekst(", sist oppdatert "),
                                Tidspunkt(it.oppdatertTid),
                                Tekst("."),
                            )
                        },
                "Vedtakstidspunkt" to Tidspunkt(behandlinger.single { it.id == behandling.id }.vedtakstidspunkt),
            )
        ),
    )

    private fun vedleggTidligereBehandlinger(): Seksjon {
        return Seksjon(
            "Tidligere behandlinger av retten til og størrelsen på AAP",
            Dict(
                behandlinger.filter { it.id != behandling.id }
                    .map {
                        Span(Tekst("Vedtatt "), Tidspunkt(it.vedtakstidspunkt), Tekst(".")) to
                                Span(
                                    Tekst("Opprettet "),
                                    Tidspunkt(it.opprettetTidspunkt),
                                    Tekst("."),
                                    Tekst(" Årsak "),
                                    it.årsakTilOpprettelse?.let { PrettyEnum(it) },
                                    Tekst("."),
                                    Tekst(" Referanse ${it.referanse}.")
                                )
                    }
            ),
        )
    }

    private fun vedleggDokumentoversikt(): Seksjon {
        fun referanse(mottattDokument: MottattDokument): LøpendeTekst? {
            return when (mottattDokument.type) {
                InnsendingType.SØKNAD,
                InnsendingType.MELDEKORT ->
                    ReferanseJournalpost(mottattDokument.referanse.asJournalpostId)

                InnsendingType.AKTIVITETSKORT,
                InnsendingType.LEGEERKLÆRING,
                InnsendingType.LEGEERKLÆRING_AVVIST,
                InnsendingType.DIALOGMELDING,
                InnsendingType.KLAGE,
                InnsendingType.ANNET_RELEVANT_DOKUMENT,
                InnsendingType.MANUELL_REVURDERING,
                InnsendingType.OMGJØRING_KLAGE_REVURDERING,
                InnsendingType.MIGRERING_FRA_ARENA,
                InnsendingType.NY_ÅRSAK_TIL_BEHANDLING,
                InnsendingType.KABAL_HENDELSE,
                InnsendingType.TILBAKEKREVING_HENDELSE,
                InnsendingType.FAGSYSTEMINFO_BEHOV_HENDELSE,
                InnsendingType.PDL_HENDELSE_DODSFALL_BRUKER,
                InnsendingType.PDL_HENDELSE_DODSFALL_BARN,
                InnsendingType.PDL_HENDELSE_FOLKEREGISTERIDENT,
                InnsendingType.OPPFØLGINGSOPPGAVE,
                InnsendingType.INSTITUSJONSOPPHOLD,
                InnsendingType.SYKEPENGE_VEDTAK_HENDELSE,
                InnsendingType.FORELDREPENGE_VEDTAK_HENDELSE,
                InnsendingType.UFØRE_VEDTAK_HENDELSE ->
                    Tekst("TODO ${mottattDokument.referanse}")
            }
        }

        fun dokumentTabell(
            dokumenter: List<MottattDokument>,
            inkludererBehandling: Boolean,
        ): Tabell? {
            val kolonner = buildList<LøpendeTekst> {
                add(Tekst("Journalpost"))
                add(Tekst("Type"))
                add(Tekst("Mottatt"))
                add(Tekst("Registrert"))
                if (inkludererBehandling) add(Tekst("Behandlet i"))
            }
            val rader = dokumenter.mapNotNull { mottattDokument ->
                referanse(mottattDokument)?.let { referanse ->
                    buildList<LøpendeTekst> {
                        add(referanse)
                        add(PrettyEnum(mottattDokument.type))
                        add(Tidspunkt(mottattDokument.mottattTidspunkt))
                        add(Tidspunkt(mottattDokument.opprettetTid))
                        if (inkludererBehandling) {
                            add(
                                mottattDokument.behandlingId?.let(::ReferanseBehandling)
                                    ?: Tekst("—")
                            )
                        }
                    }
                }
            }
            return rader.takeIf { it.isNotEmpty() }?.let { Tabell(kolonner, it) }
        }

        return Seksjon(
            "Dokumenter",
            Seksjon(
                "Nye dokumenter for behandlingen",
                dokumentTabell(
                    dokumenter = mottatteDokumenter.filter { it.behandlingId == behandling.id },
                    inkludererBehandling = false,
                )
            ),
            Seksjon(
                "Dokumenter fra tidligere behandlinger",
                dokumentTabell(
                    dokumenter = mottatteDokumenter
                        .filter { it.behandlingId != behandling.id }
                        .filter { it.opprettetTid <= behandling.opprettetTidspunkt },
                    inkludererBehandling = true,
                )
            ),
        )
    }
}

fun Tidslinje<Sykdomsvurdering>.tilSeksjon() = Seksjon(
    tittel = Tekst("Vurderinger av § 11-5"),
    subseksjoner = this.segmenter().map { it.verdi.tilSeksjon(it.periode) }.toList(),
)

fun Sykdomsvurdering.tilSeksjon(bruktForPeriode: DomenePeriode): Seksjon = Seksjon(
    vurderingsoverskrift(this.vurdertIBehandling, bruktForPeriode),
    Dict(
        "Vurderingen gjelder fra og med" to Dato(vurderingenGjelderFra),
        "Vurderingen gjelder til og med" to (vurderingenGjelderTil?.let { Dato(it) } ?: Tekst("Ikke satt")),
    ),
    Fritekstfelt("Begrunnelse", this.begrunnelse),
    Dict(
        "Har skade, sykdom eller lyte" to JaNeiValg(this.harSkadeSykdomEllerLyte),
        "Skade, sykdom, eller lyte er vesentlig del" to JaNeiValg(this.erSkadeSykdomEllerLyteVesentligdel),
        "Nedsettelse i arbeidsevne er mer enn halvparten" to JaNeiValg(this.erNedsettelseIArbeidsevneMerEnnHalvparten),
        "Er nedsettelse i arbeidsevne mer enn yrkesskadegrense" to JaNeiValg(this.erNedsettelseIArbeidsevneMerEnnYrkesskadeGrense),
        "Hoveddiagnose" to if (diagnose?.hoveddiagnose == null)
            Tekst("Ikke valgt")
        else
            Tekst("${diagnose.hoveddiagnose} (${diagnose.kodeverk})"),
        "Bidiagnoser" to if (diagnose?.bidiagnoser.isNullOrEmpty())
            Tekst("Ikke valgt")
        else
            Tekst("${diagnose.bidiagnoser.joinToString(", ")} (${diagnose.kodeverk})"),
        "Nedsatt arbeidsevne" to PrettyEnum(harNedsattArbeidsevne),
    ),
    yrkesskadeBegrunnelse?.let { Fritekstfelt("Begrunnelse for vurdering av yrkesskade", it) }
)

class VedtakDokumentGenerator(
    private val behandlingRepository: BehandlingRepository,
    private val sakRepository: SakRepository,
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val underveisRepository: UnderveisRepository,
    private val mottattDokumentRepository: MottattDokumentRepository,
    private val beregningsgrunnlagRepository: BeregningsgrunnlagRepository,
    private val sykdomRepository: SykdomRepository,
    private val bistandRepository: BistandRepository,
    private val studentRepository: StudentRepository,
    private val overgangUføreRepository: OvergangUføreRepository,
    private val etableringEgenVirksomhetRepository: EtableringEgenVirksomhetRepository,
    private val arbeidsevneRepository: ArbeidsevneRepository,
    private val arbeidsopptrappingRepository: ArbeidsopptrappingRepository,
    private val overgangArbeidRepository: OvergangArbeidRepository,
    private val vedtakslengdeRepository: VedtakslengdeRepository,
    private val meldepliktRepository: MeldepliktRepository,
    private val stønadsperiodeRepository: StønadsperiodeRepository,
    private val barnetilleggRepository: BarnetilleggRepository,
    private val samordningRepository: SamordningRepository,
    private val rettighetstypeRepository: RettighetstypeRepository,
    private val institusjonsoppholdRepository: InstitusjonsoppholdRepository,
    private val sykepengerErstatningRepository: SykepengerErstatningRepository,
    private val refusjonkravRepository: RefusjonkravRepository,
    private val overstyringMeldepliktRepository: OverstyringMeldepliktRepository,
    private val manuellInntektGrunnlagRepository: ManuellInntektGrunnlagRepository,
    private val beregningVurderingRepository: BeregningVurderingRepository,
    private val medlemskapArbeidInntektForutgåendeRepository: MedlemskapArbeidInntektForutgåendeRepository,
    private val oppholdskravGrunnlagRepository: OppholdskravGrunnlagRepository,
) {
    constructor(repositoryProvider: RepositoryProvider) : this(
        behandlingRepository = repositoryProvider.provide(),
        sakRepository = repositoryProvider.provide(),
        vilkårsresultatRepository = repositoryProvider.provide(),
        tilkjentYtelseRepository = repositoryProvider.provide(),
        underveisRepository = repositoryProvider.provide(),
        mottattDokumentRepository = repositoryProvider.provide(),
        beregningsgrunnlagRepository = repositoryProvider.provide(),
        sykdomRepository = repositoryProvider.provide(),
        bistandRepository = repositoryProvider.provide(),
        studentRepository = repositoryProvider.provide(),
        overgangUføreRepository = repositoryProvider.provide(),
        etableringEgenVirksomhetRepository = repositoryProvider.provide(),
        arbeidsevneRepository = repositoryProvider.provide(),
        arbeidsopptrappingRepository = repositoryProvider.provide(),
        overgangArbeidRepository = repositoryProvider.provide(),
        vedtakslengdeRepository = repositoryProvider.provide(),
        meldepliktRepository = repositoryProvider.provide(),
        stønadsperiodeRepository = repositoryProvider.provide(),
        barnetilleggRepository = repositoryProvider.provide(),
        samordningRepository = repositoryProvider.provide(),
        rettighetstypeRepository = repositoryProvider.provide(),
        institusjonsoppholdRepository = repositoryProvider.provide(),
        sykepengerErstatningRepository = repositoryProvider.provide(),
        refusjonkravRepository = repositoryProvider.provide(),
        overstyringMeldepliktRepository = repositoryProvider.provide(),
        manuellInntektGrunnlagRepository = repositoryProvider.provide(),
        beregningVurderingRepository = repositoryProvider.provide(),
        medlemskapArbeidInntektForutgåendeRepository = repositoryProvider.provide(),
        oppholdskravGrunnlagRepository = repositoryProvider.provide(),
    )

    fun genererDokument(
        behandlingId: BehandlingId,
        sakId: SakId,
        vedtakstidspunkt: LocalDateTime,
        forrigeBehandlingId: BehandlingId?,
    ) = lagGrunnlag(
        behandlingId = behandlingId,
        sakId = sakId,
        vedtakstidspunkt = vedtakstidspunkt,
        forrigeBehandlingId = forrigeBehandlingId,
    ).genererDokument()

    internal fun lagGrunnlag(
        behandlingId: BehandlingId,
        sakId: SakId,
        vedtakstidspunkt: LocalDateTime,
        forrigeBehandlingId: BehandlingId?,
    ): BehandlingFaktagrunnlag {
        val behandling = behandlingRepository.hent(behandlingId)
        val sak = sakRepository.hent(behandling.sakId)
        val behandlinger = behandlingRepository.hentAlleMedVedtakFor(
            sak.person.id,
            TypeBehandling.ytelseBehandlingstyper()
        ).filter { it.vedtakstidspunkt <= vedtakstidspunkt.plusSeconds(1) }

        return BehandlingFaktagrunnlag(
            behandling = behandling,
            behandlinger = behandlinger,
            vilkårsresultat = vilkårsresultatRepository.hent(behandlingId),
            tilkjentYtelse = tilkjentYtelseRepository.hentHvisEksisterer(behandlingId)
                ?.tilTidslinje().orEmpty(),
            underveis = underveisRepository.hentHvisEksisterer(behandlingId)?.somTidslinje().orEmpty(),
            mottatteDokumenter = mottattDokumentRepository.hentDokumenterForSak(sakId).toList(),
            beregningsgrunnlag = beregningsgrunnlagRepository.hentHvisEksisterer(behandlingId),

            forrigeTilkjentYtelse = forrigeBehandlingId
                ?.let { tilkjentYtelseRepository.hentHvisEksisterer(it) }
                ?.tilTidslinje().orEmpty(),
            forrigeUnderveis = forrigeBehandlingId
                ?.let { underveisRepository.hentHvisEksisterer(it) }
                ?.somTidslinje().orEmpty(),
            forrigeVilkårsresultat = forrigeBehandlingId
                ?.let { vilkårsresultatRepository.hent(it) }
                ?: Vilkårsresultat(),

            sykdomGrunnlag = sykdomRepository.hentHvisEksisterer(behandlingId),
            bistandGrunnlag = bistandRepository.hentHvisEksisterer(behandlingId),
            studentGrunnlag = studentRepository.hentHvisEksisterer(behandlingId),
            overgangUføreGrunnlag = overgangUføreRepository.hentHvisEksisterer(behandlingId),
            etableringEgenVirksomhetGrunnlag = etableringEgenVirksomhetRepository.hentHvisEksisterer(behandlingId),
            arbeidsevneGrunnlag = arbeidsevneRepository.hentHvisEksisterer(behandlingId),
            arbeidsopptrappingGrunnlag = arbeidsopptrappingRepository.hentHvisEksisterer(behandlingId),
            overgangArbeidGrunnlag = overgangArbeidRepository.hentHvisEksisterer(behandlingId),
            vedtakslengdeGrunnlag = vedtakslengdeRepository.hentHvisEksisterer(behandlingId),
            meldepliktGrunnlag = meldepliktRepository.hentHvisEksisterer(behandlingId),
            stønadsperiodeGrunnlag = stønadsperiodeRepository.hentHvisEksisterer(behandlingId),
            barnetilleggGrunnlag = barnetilleggRepository.hentHvisEksisterer(behandlingId),
            samordningGrunnlag = samordningRepository.hentHvisEksisterer(behandlingId),
            rettighetstypeGrunnlag = rettighetstypeRepository.hentHvisEksisterer(behandlingId),
            institusjonsoppholdGrunnlag = institusjonsoppholdRepository.hentHvisEksisterer(behandlingId),
            sykepengerErstatningGrunnlag = sykepengerErstatningRepository.hentHvisEksisterer(behandlingId),
            refusjonkravVurderinger = refusjonkravRepository.hentHvisEksisterer(behandlingId),
            overstyringMeldepliktGrunnlag = overstyringMeldepliktRepository.hentHvisEksisterer(behandlingId),
            manuellInntektGrunnlag = manuellInntektGrunnlagRepository.hentHvisEksisterer(behandlingId),
            beregningVurderingGrunnlag = beregningVurderingRepository.hentHvisEksisterer(behandlingId),
            forutgåendeMedlemskapGrunnlag = medlemskapArbeidInntektForutgåendeRepository.hentHvisEksisterer(behandlingId),
            oppholdskravGrunnlag = oppholdskravGrunnlagRepository.hentHvisEksisterer(behandlingId),
        )
    }
}
