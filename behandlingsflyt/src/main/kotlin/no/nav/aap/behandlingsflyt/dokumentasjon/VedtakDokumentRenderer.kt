package no.nav.aap.behandlingsflyt.dokumentasjon

import no.nav.aap.behandlingsflyt.behandling.vilkår.innsikt.PdfDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.tilTidslinje
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Grunnlag11_19
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagInntekt
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagUføre
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagYrkesskade
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.UføreInntekt
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.gjeldendeVurderinger
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Klage
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.KravVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.OverstyrMuligRettFraÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.RelevantKrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.SøknadsdatoÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Tilleggsopplysning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.TrukketSøknad
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.rettighetsperiode.RettighetsperiodeHarRett
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.stønadsperiode.RelevantKravType
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.utils.Diff
import no.nav.aap.behandlingsflyt.utils.Endret
import no.nav.aap.behandlingsflyt.utils.Fjernet
import no.nav.aap.behandlingsflyt.utils.LagtTil
import no.nav.aap.behandlingsflyt.utils.Uendret
import no.nav.aap.behandlingsflyt.utils.diffTidslinjer
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.komponenter.type.Periode as DomenePeriode

internal fun vilkårsvurderingOppsummeringTittel(
    saksnummer: Saksnummer,
    vedtaksdato: String,
) = "Oppsummering av vilkårsvurderinger for sak $saksnummer – $vedtaksdato"

internal object VedtakDokumentRenderer {
    fun render(grunnlag: VedtakDokumentGrunnlag): PdfDokument = grunnlag.tilDokument()

    private fun VedtakDokumentGrunnlag.tilDokument(): PdfDokument {
        val kontekst = RenderKontekst(
            gjeldendeBehandlingId = behandling.id,
            vedtak = behandlinger,
        )
        val vedtaksdato = formaterVedtaksdato(behandling.id, kontekst)
        return PdfDokument(
            tittel = vilkårsvurderingOppsummeringTittel(saksnummer, vedtaksdato),
            body = tilSeksjon().render(kontekst),
        )
    }

    private fun VedtakDokumentGrunnlag.tilSeksjon(): Seksjon {
        return Seksjon(
            tittel = Tekst("Vedtak"),
            subseksjoner = listOfNotNull(
                opplysningerOmBehandlingenSub(),
                kravSub(),
                rettighetsperiodeSub(),
                stønadsperiodeSub(),
                lovvalgMedlemskapSub(),
                avslag11_27Sub(),
                studentvurderingerSub(),
                sykdomsvurderingerSub(),
                bistandsvurderingerSub(),
                etableringEgenVirksomhetSub(),
                arbeidsopptrappingSub(),
                fritakSub(),
                arbeidsevnevurderingerSub(),
                overgangUføreSub(),
                overgangArbeidSub(),
                refusjonkravSub(),
                yrkesskadevurderingSub(),
                sykepengererstatningSub(),
                beregningVurderingSub(),
                manuellInntektSub(),
                grunnlagetSub(),
                inntektsbortfallSub(),
                forutgåendeMedlemskapSub(),
                oppholdskravSub(),
                barnetilleggSub(),
                institusjonsoppholdSub(),
                samordningSub(),
                samordningUføreSub(),
                tjenestepensjonRefusjonskravSub(),
                samordningArbeidsgiverSub(),
                samordningBarnepensjonSub(),
                sykestipendSub(),
                samordningAndreStatligeYtelserSub(),
                aktivitetsplikt11_7Sub(),
                aktivitetsplikt11_9Sub(),
                rettighetstypeSub(),
                vedtakslengdeSub(),
                overstyringMeldepliktSub(),
                vilkårSub(),
                tilkjentYtelseSub(),
                vedleggTidligereBehandlingerSub(),
                vedleggDokumentoversiktSub(),
            )
        )
    }

    private fun VedtakDokumentGrunnlag.grunnlagetSub(): Seksjon {
        if (beregningsgrunnlag == null) return Seksjon(
            tittel = Tekst("Grunnlaget for størrelsen på AAP"),
            Avsnitt(Tekst("Beregningsgrunnlag er ikke tilgjengelig for denne behandlingen."))
        )
        return Seksjon(
            "Grunnlaget for størrelsen på AAP",
            Dict(
                when (beregningsgrunnlag) {
                    is Grunnlag11_19 -> grunnlag11_19Rader(beregningsgrunnlag)
                    is GrunnlagUføre -> grunnlagUføreRader(beregningsgrunnlag)

                    is GrunnlagYrkesskade -> grunnlag11_19RaderForYrkesskade(beregningsgrunnlag) + listOf(
                        Tekst("Yrkesskadeprosent") to Prosent(beregningsgrunnlag.andelYrkesskade()),
                        Tekst("Benyttet yrkesskadeandel") to Prosent(beregningsgrunnlag.benyttetAndelForYrkesskade()),
                        Tekst("Terskelverdi yrkesskade") to Prosent(beregningsgrunnlag.terskelverdiForYrkesskade()),
                        Tekst("Inntekt på yrkesskadetidspunktet (kr)") to Kroner(beregningsgrunnlag.antattÅrligInntektYrkesskadeTidspunktet()),
                        Tekst("Yrkesskadeinntekt (G)") to G(beregningsgrunnlag.yrkesskadeinntektIG()),
                        Tekst("Grunnbeløp på yrkesskadetidspunktet") to Kroner(beregningsgrunnlag.grunnbeløp()),
                        Tekst("Grunnlag med yrkesskadefordel (§§ 11-19 / 11-22)") to G(
                            beregningsgrunnlag.grunnlagEtterYrkesskadeFordel()
                        ),
                        Tekst("Andel som skyldes yrkesskade (G)") to G(beregningsgrunnlag.andelSomSkyldesYrkesskade()),
                        Tekst("Andel som ikke skyldes yrkesskade (G)") to G(beregningsgrunnlag.andelSomIkkeSkyldesYrkesskade()),
                        Tekst("Grunnlag (G)") to G(beregningsgrunnlag.grunnlaget()),
                    )
                }
            )
        )
    }

    private fun VedtakDokumentGrunnlag.kravSub(): Seksjon? {
        val vurderinger = kravGrunnlag
            ?.gjeldendeVurderinger()
            ?.sortedBy { it.opprettet }
            ?.takeIf { it.isNotEmpty() }
            ?: return null

        return Seksjon(
            tittel = Tekst("Krav"),
            subseksjoner = vurderinger.map { vurdering ->
                val overstyring = (vurdering as? RelevantKrav)?.overstyrMuligRettFra
                Seksjon(
                    tittel = Span(
                        Tekst(vurdering.typeTekst()),
                        ReferanseBehandling(vurdering.vurdertIBehandling),
                    ),
                    Fritekstfelt("Begrunnelse", vurdering.begrunnelse),
                    when (vurdering) {
                        is RelevantKrav -> Dict(
                            "Søknadsdato" to Dato(vurdering.søknadsdato.dato),
                            "Årsak til søknadsdato" to Tekst(vurdering.søknadsdato.årsak.visningsnavn()),
                            "Mulig rett fra" to Dato(vurdering.muligRettFra),
                            "Overstyrt mulig rett fra" to
                                (overstyring?.dato?.let(::Dato) ?: Tekst("Ikke overstyrt")),
                            "Årsak til overstyring" to
                                (overstyring?.årsak?.visningsnavn()?.let(::Tekst) ?: Tekst("Ikke overstyrt")),
                        )

                        is Klage,
                        is Tilleggsopplysning,
                        is TrukketSøknad -> null
                    },
                )
            },
        )
    }

    private fun KravVurdering.typeTekst(): String = when (this) {
        is RelevantKrav -> "Relevant krav"
        is TrukketSøknad -> "Trukket søknad"
        is Klage -> "Klage"
        is Tilleggsopplysning -> "Tilleggsopplysning"
    }

    private fun SøknadsdatoÅrsak.visningsnavn(): String = when (this) {
        SøknadsdatoÅrsak.BrukerHarSøktTidligere -> "Bruker har søkt tidligere"
        SøknadsdatoÅrsak.FeilregistrertSøknadsdato -> "Feilregistrert søknadsdato"
        SøknadsdatoÅrsak.SøknadMottatt -> "Søknad mottatt"
    }

    private fun OverstyrMuligRettFraÅrsak.visningsnavn(): String = when (this) {
        OverstyrMuligRettFraÅrsak.IkkeIStandTilÅSøkeTidligere -> "Ikke i stand til å søke tidligere"
        OverstyrMuligRettFraÅrsak.MisvisendeOpplysninger -> "Misvisende opplysninger"
    }

    private fun VedtakDokumentGrunnlag.rettighetsperiodeSub(): Seksjon? {
        val vurdering = rettighetsperiodeVurdering ?: return null
        return Seksjon(
            tittel = Tekst("Rettighetsperiode"),
            Fritekstfelt("Begrunnelse", vurdering.begrunnelse),
            Dict(
                "Rett utover søknadsdato" to Tekst(vurdering.harRettUtoverSøknadsdato.visningsnavn()),
                "Startdato" to (vurdering.startDato?.let(::Dato) ?: Tekst("Ikke satt")),
            ),
        )
    }

    private fun RettighetsperiodeHarRett.visningsnavn(): String = when (this) {
        RettighetsperiodeHarRett.Ja -> "Ja"
        RettighetsperiodeHarRett.Nei -> "Nei"
        RettighetsperiodeHarRett.HarRettIkkeIStandTilÅSøkeTidligere ->
            "Ja, ikke i stand til å søke tidligere"

        RettighetsperiodeHarRett.HarRettMisvisendeOpplysninger ->
            "Ja, mottok misvisende opplysninger"
    }

    private fun grunnlag11_19Rader(
        g: Grunnlag11_19
    ): List<Pair<LøpendeTekst, LøpendeTekst>> =
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
            Tekst("Grunnlag § 11-19") to G(g.grunnlaget()),
        )

    private fun grunnlagUføreRader(
        grunnlag: GrunnlagUføre
    ): List<Pair<LøpendeTekst, LøpendeTekst>> {
        return grunnlag11_19Rader(grunnlag.underliggende()) +
                uføreBeregningsalternativRader(grunnlag) +
                listOf(
                    Tekst("Grunnlag §11-19 (ytterligere nedsatt)") to G(
                        grunnlag.underliggendeYtterligereNedsatt().grunnlaget()
                    ),
                    Tekst("Type beregning") to PrettyEnum(grunnlag.type()),
                    Tekst("Grunnlag § 11-28") to G(grunnlag.grunnlaget()),
                ) +
                grunnlag.uføreInntekterFraForegåendeÅr().map { uføreInntektRad(it) }
    }

    private fun grunnlag11_19RaderForYrkesskade(
        g: GrunnlagYrkesskade
    ): List<Pair<LøpendeTekst, LøpendeTekst>> =
        when (val under = g.underliggende()) {
            is Grunnlag11_19 -> grunnlag11_19Rader(under)
            is GrunnlagUføre -> grunnlagUføreRader(under)

            is GrunnlagYrkesskade -> emptyList() // Ikke rekursjon i praksis
        }

    private fun uføreBeregningsalternativRader(
        grunnlag: GrunnlagUføre
    ): List<Pair<LøpendeTekst, LøpendeTekst>> {
        val uføreGrunnlag = grunnlag.underliggendeYtterligereNedsatt()
        val inntekter = uføreGrunnlag.inntekter()
        if (inntekter.isEmpty()) return emptyList()

        val førsteÅr = inntekter.minOf { it.år }
        val inntektSisteÅr = inntekter.maxBy { it.år }
        return listOf(
            Tekst("Gjennomsnitt inntekt siste 3 år etter §§ 11-19 / 11-28 ($førsteÅr - ${inntektSisteÅr.år})") to
                    G(uføreGrunnlag.gjennomsnittligInntektIG()),
            Tekst("Inntekt siste år etter §§ 11-19 / 11-28 (${inntektSisteÅr.år})") to
                    G(inntektSisteÅr.inntekt6GBegrenset),
        )
    }

    private fun uføreInntektRad(i: UføreInntekt): Pair<LøpendeTekst, LøpendeTekst> =
        Tekst("Uføreinntekt ${i.år}") to Span(
            Kroner(i.inntektIKroner), Tekst(" / justert: "), Kroner(i.inntektJustertForUføregrad),
        )

    private fun VedtakDokumentGrunnlag.sykdomsvurderingerSub(): Seksjon? {
        val tidslinje = sykdomGrunnlag?.somSykdomsvurderingstidslinje() ?: return null
        if (tidslinje.isEmpty()) return null
        return tidslinje.tilSeksjon()
    }

    private fun VedtakDokumentGrunnlag.bistandsvurderingerSub(): Seksjon? {
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

    private fun VedtakDokumentGrunnlag.studentvurderingerSub(): Seksjon? {
        val grunnlag = studentGrunnlag ?: return null
        val tidslinje = grunnlag.somStudenttidslinje()
        if (tidslinje.isEmpty()) return null
        return Seksjon(
            tittel = Tekst("Student (§ 11-14)"),
            subseksjoner = tidslinje.segmenter().map { (periode, v) ->
                Seksjon(
                    tittel = vurderingsoverskrift(v.vurdertIBehandling, periode),
                    Dict(
                        "Avbrutt studie" to JaNeiValg(v.harAvbruttStudie),
                        "Dato for avbrutt studie" to (v.avbruttStudieDato?.let { Dato(it) } ?: Tekst("Ikke satt")),
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

    private fun VedtakDokumentGrunnlag.overgangUføreSub(): Seksjon? {
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

    private fun VedtakDokumentGrunnlag.etableringEgenVirksomhetSub(): Seksjon? {
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

    private fun VedtakDokumentGrunnlag.arbeidsevnevurderingerSub(): Seksjon? {
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

    private fun VedtakDokumentGrunnlag.arbeidsopptrappingSub(): Seksjon? {
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

    private fun VedtakDokumentGrunnlag.overgangArbeidSub(): Seksjon? {
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

    private fun VedtakDokumentGrunnlag.vedtakslengdeSub(): Seksjon? {
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

    private fun VedtakDokumentGrunnlag.fritakSub(): Seksjon? {
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

    private fun VedtakDokumentGrunnlag.aktivitetsplikt11_7Sub(): Seksjon? {
        val tidslinje = aktivitetsplikt11_7Grunnlag?.tidslinje() ?: return null
        if (tidslinje.isEmpty()) return null
        return Seksjon(
            tittel = Tekst("Aktivitetsplikt (§ 11-7)"),
            Tabell.ofTidslinje(
                kolonner = listOf(
                    Tekst("Oppfylt"),
                    Tekst("Utfall"),
                    Tekst("Varselfrist skal ignoreres"),
                    Tekst("Begrunnelse"),
                ),
                tidslinje = tidslinje.map { vurdering ->
                    listOf(
                        JaNeiValg(vurdering.erOppfylt),
                        PrettyEnum(vurdering.utfall),
                        JaNeiValg(vurdering.skalIgnorereVarselFrist),
                        Tekst(vurdering.begrunnelse),
                    )
                },
            )
        )
    }

    private fun VedtakDokumentGrunnlag.aktivitetsplikt11_9Sub(): Seksjon? {
        val vurderinger = aktivitetsplikt11_9Grunnlag
            ?.gjeldendeVurderinger()
            ?.sortedBy { it.dato }
            ?.takeIf { it.isNotEmpty() }
            ?: return null
        return Seksjon(
            tittel = Tekst("Brudd på aktivitetsplikten (§ 11-9)"),
            subseksjoner = vurderinger.map { vurdering ->
                Seksjon(
                    tittel = Span(
                        Tekst("Brudd "),
                        Dato(vurdering.dato),
                        ReferanseBehandling(vurdering.vurdertIBehandling),
                    ),
                    Fritekstfelt("Begrunnelse", vurdering.begrunnelse),
                    Dict(
                        "Type brudd" to PrettyEnum(vurdering.brudd),
                        "Grunn" to PrettyEnum(vurdering.grunn),
                    ),
                )
            },
        )
    }

    private fun VedtakDokumentGrunnlag.stønadsperiodeSub(): Seksjon? {
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

    private fun VedtakDokumentGrunnlag.barnetilleggSub(): Seksjon? {
        val perioder = barnetilleggGrunnlag?.perioder?.tilTidslinje().orEmpty()
        val vurderteBarn = barnetilleggVurderinger?.barn.orEmpty()
        if (perioder.isEmpty() && vurderteBarn.isEmpty()) return null

        return Seksjon(
            tittel = Tekst("Barnetillegg"),
            blokker = listOfNotNull(
                perioder.takeIf { it.isNotEmpty() }?.let {
                    Tabell.ofTidslinje(
                        kolonner = listOf(Tekst("Antall barn med rett til barnetillegg")),
                        tidslinje = it.map { periode ->
                            listOf(Tekst(periode.barnMedRettTil().size.toString()))
                        },
                    )
                },
            ),
            subseksjoner = vurderteBarn.mapIndexed { index, barn ->
                Seksjon(
                    tittel = Tekst("Barn ${index + 1}"),
                    Tabell.ofTidslinje(
                        kolonner = listOf(
                            Tekst("Har foreldreansvar"),
                            Tekst("Er fosterforelder"),
                            Tekst("Begrunnelse"),
                        ),
                        tidslinje = barn.tilTidslinje().map { vurdering ->
                            listOf(
                                JaNeiValg(vurdering.harForeldreAnsvar),
                                JaNeiValg(vurdering.erFosterforelder),
                                Tekst(vurdering.begrunnelse),
                            )
                        },
                    ),
                )
            },
        )
    }

    private fun VedtakDokumentGrunnlag.rettighetstypeSub(): Seksjon? {
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

    private fun VedtakDokumentGrunnlag.yrkesskadevurderingSub(): Seksjon? {
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

    private fun VedtakDokumentGrunnlag.sykepengererstatningSub(): Seksjon? {
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

    private fun VedtakDokumentGrunnlag.refusjonkravSub(): Seksjon? {
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

    private fun VedtakDokumentGrunnlag.overstyringMeldepliktSub(): Seksjon? {
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

    private fun VedtakDokumentGrunnlag.samordningSub(): Seksjon? {
        val vurdering = samordningVurderingGrunnlag?.let { grunnlag ->
            val rader = grunnlag.vurderinger
                .flatMap { vurdering ->
                    vurdering.vurderingPerioder.map { periode -> vurdering.ytelseType to periode }
                }
                .sortedWith(compareBy({ it.second.periode.fom }, { it.first.name }))

            Seksjon(
                tittel = Tekst("Vurdering"),
                grunnlag.begrunnelse?.let { Fritekstfelt("Begrunnelse", it) },
                if (rader.isEmpty()) {
                    Avsnitt(Tekst("Ingen ytelser er vurdert for samordning."))
                } else {
                    Tabell(
                        kolonner = listOf(
                            Tekst("Ytelse"),
                            Tekst("Periode"),
                            Tekst("Gradering"),
                            Tekst("Manuelt vurdert"),
                        ),
                        rader = rader.map { (ytelse, periode) ->
                            listOf(
                                PrettyEnum(ytelse),
                                Periode(periode.periode),
                                periode.gradering?.let(::Prosent) ?: Tekst("Ikke vurdert"),
                                JaNeiValg(periode.manuell),
                            )
                        },
                    )
                },
            )
        }

        val resultat = samordningGrunnlag
            ?.samordningPerioder
            ?.takeIf { it.isNotEmpty() }
            ?.let { perioder ->
                Seksjon(
                    tittel = Tekst("Resultat"),
                    subseksjoner = perioder.sortedBy { it.periode.fom }.map { periode ->
                        Seksjon(
                            tittel = Periode(periode.periode),
                            Dict(
                                "Samordningsgradering" to Prosent(periode.gradering),
                            ),
                        )
                    },
                )
            }

        if (vurdering == null && resultat == null) return null

        return Seksjon(
            tittel = Tekst("Samordning"),
            vurdering,
            resultat,
        )
    }

    private fun VedtakDokumentGrunnlag.samordningUføreSub(): Seksjon? {
        val vurdering = samordningUføreGrunnlag?.vurdering ?: return null
        return Seksjon(
            tittel = Tekst("Samordning med uføretrygd"),
            Fritekstfelt("Begrunnelse", vurdering.begrunnelse),
            Tabell.ofTidslinje(
                kolonner = listOf(Tekst("Uføregrad til samordning")),
                tidslinje = vurdering.tilTidslinje().map { listOf(Prosent(it)) },
            ),
        )
    }

    private fun VedtakDokumentGrunnlag.tjenestepensjonRefusjonskravSub(): Seksjon? {
        val vurdering = tjenestepensjonRefusjonskravVurdering ?: return null
        return Seksjon(
            tittel = Tekst("Refusjonskrav fra tjenestepensjonsordning"),
            Fritekstfelt("Begrunnelse", vurdering.begrunnelse),
            Dict(
                "Har refusjonskrav" to JaNeiValg(vurdering.harKrav),
                "Fra og med" to (vurdering.fom?.let(::Dato) ?: Tekst("Ikke satt")),
                "Til og med" to (vurdering.tom?.let(::Dato) ?: Tekst("Ikke satt")),
            ),
        )
    }

    private fun VedtakDokumentGrunnlag.samordningArbeidsgiverSub(): Seksjon? {
        val vurdering = samordningArbeidsgiverGrunnlag?.vurdering ?: return null
        return Seksjon(
            tittel = Tekst("Reduksjon ved ytelser fra arbeidsgiver (§ 11-24)"),
            Fritekstfelt("Begrunnelse", vurdering.begrunnelse),
            Tabell(
                kolonner = listOf(Tekst("Periode")),
                rader = vurdering.perioder
                    .sortedBy { it.fom }
                    .map { listOf(Periode(it)) },
            ),
        )
    }

    private fun VedtakDokumentGrunnlag.samordningBarnepensjonSub(): Seksjon? {
        val vurdering = barnepensjonGrunnlag?.vurdering ?: return null
        return Seksjon(
            tittel = Tekst("Samordning med barnepensjon (§ 11-27)"),
            blokker = listOf(Fritekstfelt("Begrunnelse", vurdering.begrunnelse)),
            subseksjoner = vurdering.perioder
                .sortedBy { it.fom }
                .map { periode ->
                    Seksjon(
                        tittel = vurderingsoverskrift(vurdering.vurdertIBehandling, periode.periode),
                        Dict(
                            "Månedsbeløp" to Kroner(periode.månedsats),
                        ),
                    )
                },
        )
    }

    private fun VedtakDokumentGrunnlag.samordningAndreStatligeYtelserSub(): Seksjon? {
        val vurdering = samordningAndreStatligeYtelserGrunnlag?.vurdering ?: return null
        return Seksjon(
            tittel = Tekst("Samordning med andre statlige ytelser"),
            Fritekstfelt("Begrunnelse", vurdering.begrunnelse),
            if (vurdering.vurderingPerioder.isEmpty()) {
                Avsnitt(Tekst("Ingen andre statlige ytelser er registrert."))
            } else {
                Tabell(
                    kolonner = listOf(Tekst("Ytelse"), Tekst("Periode")),
                    rader = vurdering.vurderingPerioder
                        .sortedWith(compareBy({ it.periode.fom }, { it.ytelse.name }))
                        .map {
                            listOf(
                                PrettyEnum(it.ytelse),
                                Periode(it.periode),
                            )
                        },
                )
            },
        )
    }

    private fun VedtakDokumentGrunnlag.avslag11_27Sub(): Seksjon? {
        val vurderinger = avslag11_27Grunnlag
            ?.gjeldendeVurderinger()
            ?.sortedBy { it.opprettet }
            ?.takeIf { it.isNotEmpty() }
            ?: return null
        return Seksjon(
            tittel = Tekst("Vurdering av annen full ytelse (§ 11-27)"),
            subseksjoner = vurderinger.map { vurdering ->
                Seksjon(
                    tittel = Tekst("Vurdering"),
                    Fritekstfelt("Begrunnelse", vurdering.begrunnelse),
                    Dict(
                        "Har annen full ytelse" to JaNeiValg(vurdering.harAnnenFullYtelse),
                        "Ytelse" to PrettyEnum(vurdering.brukersYtelse),
                        "Ytelse til og med" to (vurdering.brukersYtelseTom?.let { Dato(it) } ?: Tekst("Ikke satt")),
                        "Sykepengegrunnlag over 2 G" to JaNeiValg(vurdering.harSykepengegrunnlagOver2G),
                        "Arbeidsgiver utbetaler sykepenger" to JaNeiValg(vurdering.harArbeidsgiverSykepengerUtbetaling),
                        "Skal avslås etter § 11-27" to JaNeiValg(vurdering.skalAvslås1127),
                    )
                )
            }
        )
    }

    private fun VedtakDokumentGrunnlag.sykestipendSub(): Seksjon? {
        val vurdering = sykestipendGrunnlag?.vurdering ?: return null
        val tidslinje = vurdering.tilMottarSykestipendTidslinje()
        return Seksjon(
            tittel = Tekst("Sykestipend (§ 11-29)"),
            Seksjon(
                tittel = Tekst("Vurdering"),
                Fritekstfelt("Begrunnelse", vurdering.begrunnelse),
                Dict(
                    "Mottar sykestipend" to JaNeiValg(!tidslinje.isEmpty()),
                ),
                tidslinje.takeIf { !it.isEmpty() }?.let {
                    Tabell(
                        kolonner = listOf(Tekst("Perioder med sykestipend")),
                        rader = it.segmenter().map { segment ->
                            listOf(Periode(segment.periode, kompakt = true))
                        },
                    )
                },
            )
        )
    }

    private fun VedtakDokumentGrunnlag.inntektsbortfallSub(): Seksjon? {
        val vurdering = inntektsbortfallVurdering ?: return null
        return Seksjon(
            tittel = Tekst("Inntektsbortfall (§ 11-4 andre ledd)"),
            Seksjon(
                tittel = Tekst("Vurdering"),
                Fritekstfelt("Begrunnelse", vurdering.begrunnelse),
                Dict(
                    "Rett til fullt uttak av alderspensjon" to JaNeiValg(vurdering.rettTilUttak),
                ),
            )
        )
    }

    private fun VedtakDokumentGrunnlag.institusjonsoppholdSub(): Seksjon? {
        val grunnlag = institusjonsoppholdGrunnlag ?: return null
        val harData =
            grunnlag.oppholdene != null || grunnlag.soningsVurderinger != null || grunnlag.helseoppholdvurderinger != null
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

    private fun VedtakDokumentGrunnlag.forutgåendeMedlemskapSub(): Seksjon? {
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

    private fun VedtakDokumentGrunnlag.lovvalgMedlemskapSub(): Seksjon? {
        val grunnlag = lovvalgMedlemskapGrunnlag ?: return null
        val tidslinje = grunnlag.gjeldendeVurderinger()
        if (tidslinje.isEmpty()) return null
        return Seksjon(
            tittel = Tekst("Lovvalg og medlemskap"),
            subseksjoner = tidslinje.segmenter().map { (periode, vurdering) ->
                Seksjon(
                    tittel = vurderingsoverskrift(vurdering.vurdertIBehandling, periode),
                    Fritekstfelt("Begrunnelse for lovvalg", vurdering.lovvalg.begrunnelse),
                    Dict(
                        "Lovvalgsland" to Tekst(vurdering.lovvalg.lovvalgsEØSLandEllerLandMedAvtale.name),
                        "Medlem i folketrygden" to JaNeiValg(vurdering.medlemskap?.varMedlemIFolketrygd),
                        "Overstyrt" to JaNeiValg(vurdering.overstyrt),
                    ),
                    vurdering.medlemskap?.let {
                        Fritekstfelt("Begrunnelse for medlemskap", it.begrunnelse)
                    },
                )
            }
        )
    }

    private fun VedtakDokumentGrunnlag.oppholdskravSub(): Seksjon? {
        val grunnlag = oppholdskravGrunnlag ?: return null
        val tidslinje = grunnlag.somPeriodiserteVurderinger().gjeldendeVurderinger()
        if (tidslinje.isEmpty()) return null
        return Seksjon(
            tittel = Tekst("Oppholdskrav"),
            subseksjoner = tidslinje.segmenter().map { (periode, vurdering) ->
                Seksjon(
                    tittel = vurderingsoverskrift(vurdering.vurdertIBehandling, periode),
                    Fritekstfelt("Begrunnelse", vurdering.begrunnelse),
                    Dict(
                        "Land" to Tekst(vurdering.land ?: "—"),
                        "Oppfylt" to JaNeiValg(vurdering.oppfylt),
                    )
                )
            }
        )
    }

    private fun VedtakDokumentGrunnlag.manuellInntektSub(): Seksjon? {
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

    private fun VedtakDokumentGrunnlag.beregningVurderingSub(): Seksjon? {
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
                            "Ytterligere nedsatt dato" to (t.ytterligereNedsattArbeidsevneDato?.let { Dato(it) }
                                ?: Tekst("Ikke satt")),
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

    private fun VedtakDokumentGrunnlag.vilkårSub(): Seksjon = Seksjon(
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


    private fun VedtakDokumentGrunnlag.tilkjentYtelseSub(): Seksjon {
        val dagsatsDenneBehandling = tilkjentYtelse.map {
            listOf(
                G(it.grunnlagsfaktor),
                Kroner(it.grunnbeløp),
                Kroner(it.dagsats)
            )
        }
            .komprimer()
        val dagsatsForrigeBehandling = forrigeTilkjentYtelse.map {
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
                            tidslinje = diffDagsats.mapNotNull {
                                when (it) {
                                    is Endret<List<LøpendeTekst>> -> it.fra.zip(it.til).map { (fra, til) ->
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
                        tidslinje = diffDagsats.mapNotNull {
                            when (it) {
                                is Uendret<List<LøpendeTekst>> -> it.uendret
                                is Endret<*>,
                                is Fjernet<*>,
                                is LagtTil<*> -> null
                            }
                        }
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
                            .map { tilkjent ->
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
                    tidslinje = tilkjentYtelse.map {
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

    private fun VedtakDokumentGrunnlag.opplysningerOmBehandlingenSub(): Seksjon = Seksjon(
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

    private fun VedtakDokumentGrunnlag.vedleggTidligereBehandlingerSub(): Seksjon {
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

    private fun VedtakDokumentGrunnlag.vedleggDokumentoversiktSub(): Seksjon {
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
            inkludererBehandlingsdetaljer: Boolean,
        ): Tabell? {
            val kolonner = buildList<LøpendeTekst> {
                add(Tekst("Journalpost"))
                add(Tekst("Type"))
                add(Tekst("Mottatt"))
                add(Tekst("Registrert"))
                if (inkludererBehandlingsdetaljer) {
                    add(Tekst("Vedtakstidspunkt"))
                }
            }
            val rader = dokumenter.mapNotNull { mottattDokument ->
                referanse(mottattDokument)?.let { referanse ->
                    buildList<LøpendeTekst> {
                        add(referanse)
                        add(PrettyEnum(mottattDokument.type))
                        add(Tidspunkt(mottattDokument.mottattTidspunkt))
                        add(Tidspunkt(mottattDokument.opprettetTid))
                        if (inkludererBehandlingsdetaljer) {
                            val dokumentetsBehandling = mottattDokument.behandlingId?.let { behandlingId ->
                                behandlinger.singleOrNull { it.id == behandlingId }
                            }
                            add(dokumentetsBehandling?.let { Tidspunkt(it.vedtakstidspunkt) } ?: Tekst("—"))
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
                    inkludererBehandlingsdetaljer = false,
                )
            ),
            Seksjon(
                "Dokumenter fra tidligere behandlinger",
                dokumentTabell(
                    dokumenter = mottatteDokumenter
                        .filter { it.behandlingId != behandling.id }
                        .filter { it.opprettetTid <= behandling.opprettetTidspunkt },
                    inkludererBehandlingsdetaljer = true,
                )
            ),
        )
    }

    private fun Tidslinje<Sykdomsvurdering>.tilSeksjon() = Seksjon(
        tittel = Tekst("Vurderinger av § 11-5"),
        subseksjoner = this.segmenter().map { it.verdi.tilSeksjon(it.periode) }.toList(),
    )

    private fun Sykdomsvurdering.tilSeksjon(bruktForPeriode: DomenePeriode): Seksjon = Seksjon(
        vurderingsoverskrift(this.vurdertIBehandling, bruktForPeriode),
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
}
