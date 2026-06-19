package no.nav.aap.behandlingsflyt.dokumentasjon

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehov
import no.nav.aap.behandlingsflyt.behandling.meldekort.Dokument
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.Tilkjent
import no.nav.aap.behandlingsflyt.faktagrunnlag.Faktagrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Beregningsgrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Grunnlag11_19
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagInntekt
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagUføre
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagYrkesskade
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsresultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkårsvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vilkårtype
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingMedVedtak
import no.nav.aap.behandlingsflyt.utils.Diff
import no.nav.aap.behandlingsflyt.utils.Endret
import no.nav.aap.behandlingsflyt.utils.Fjernet
import no.nav.aap.behandlingsflyt.utils.LagtTil
import no.nav.aap.behandlingsflyt.utils.Uendret
import no.nav.aap.behandlingsflyt.utils.diffTidslinjer
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.komponenter.type.Periode as DomenePeriode

class BehandlingFaktagrunnlag(
    val behandling: Behandling,
    val behandlinger: List<BehandlingMedVedtak>,
    val vilkårsresultat: Vilkårsresultat,
    val tilkjentYtelse: Tidslinje<Tilkjent>,
    val underveis: Tidslinje<Underveisperiode>,
    val faktagrunnlag: Map<Vilkårtype, Faktagrunnlag>,
    val avklaringsbehovene: List<Avklaringsbehov>,
    val mottatteDokumenter: List<MottattDokument>,
    val beregningsgrunnlag: Beregningsgrunnlag?,

    val forrigeTilkjentYtelse: Tidslinje<Tilkjent>,
    val forrigeUnderveis: Tidslinje<Underveisperiode>,
    val forrigeVilkårsresultat: Vilkårsresultat,
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
            "Vedtak",
            opplysningerOmBehandlingen(),
            grunnlaget(),
            vilkår(),
            tilkjentYtelse(),
            /* Enkeltvurderinger */
            /* Underveis? */
            vedleggTidligereBehandlinger(),
            vedleggDokumentoversikt(),
        )
    }

    private fun grunnlaget(): Seksjon {
        return Seksjon(
            "Grunnlaget for størrelsen på AAP",
            Dict(
                when (beregningsgrunnlag) {
                    null -> TODO()
                    is Grunnlag11_19 ->
                        beregningsgrunnlag.inntekter().map<GrunnlagInntekt, Pair<LøpendeTekst, LøpendeTekst>> {
                            Tekst("År ${it.år}") to Span(
                                Tekst("Inntekt i kroner: "), Kroner(it.inntektIKroner), Tekst("."),
                                Tekst(" Inntekt i G: "), G(it.inntektIG), Tekst("."),
                                Tekst(" Grunnbeløp benyttet i omregning: "), Kroner(it.grunnbeløp), Tekst("."),
                                Tekst(" 6G-begrenset inntekt i G: "), G(it.inntekt6GBegrenset), Tekst("."),
                                Tekst(" Er 6G-begrenset: "), JaNeiValg(it.er6GBegrenset), Tekst("."),
                            )
                        } + listOf(
                            Tekst(" Gjennomsnitt 3 år: ") to Span(
                                G(beregningsgrunnlag.gjennomsnittligInntektIG()),
                                Tekst(".")
                            ),
                            Tekst(" Gjennomsnitt valgt: ") to Span(
                                JaNeiValg(beregningsgrunnlag.erGjennomsnitt()),
                                Tekst(".")
                            ),
                            Tekst(" Endelig grunnlag: ") to Span(G(beregningsgrunnlag.grunnlaget()), Tekst(".")),
                        )

                    is GrunnlagUføre -> TODO()
                    is GrunnlagYrkesskade -> TODO()
                }


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

        fun foo(
            periode: DomenePeriode,
            diff: Diff<Unit>?,
            gjeldende: Vilkårsvurdering?,
        ): Pair<LøpendeTekst, LøpendeTekst> =
            Span(Periode(periode), Tekst(":")) to Span(
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
                                Tekst(" Utfall: ${gjeldende?.utfall}. Manuell vurdering: ${gjeldende?.manuellVurdering}. "),
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
                    .map { foo(it.periode, it.verdi.first, it.verdi.second) }
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
                "Referanse:" to Tekst(behandling.referanse.toString()),
                "Opprettet:" to Tidspunkt(behandling.opprettetTidspunkt),
                "Årsak til opprettelse:" to PrettyEnum(behandling.årsakTilOpprettelse),
                "Vurderingsbehov:" to
                        behandling.vurderingsbehov().join(separator = " ") {
                            Span(
                                PrettyEnum(it.type),
                                Tekst(", sist oppdatert "),
                                Tidspunkt(it.oppdatertTid),
                                Tekst("."),
                            )
                        },
                "Vedtakstidspunkt:" to Tidspunkt(behandlinger.single { it.id == behandling.id }.vedtakstidspunkt),
                "Beslutter:" to (
                        avklaringsbehovene.firstOrNull { it.definisjon == Definisjon.FATTE_VEDTAK }
                            ?.løstAv()
                            ?.let { ReferanseBruker(it) }
                            ?: Tekst("—")
                        ),
                "Kvalitetssikrer:" to (
                        avklaringsbehovene.firstOrNull { it.definisjon == Definisjon.KVALITETSSIKRING }
                            ?.løstAv()
                            ?.let { ReferanseBruker(it) }
                            ?: Tekst("—")
                        ),
                "Saksbehandlere:" to Span(
                    avklaringsbehovene
                        .filter {
                            it.erIkkeAvbrutt() && !it.erVentepunkt() && it.definisjon !in listOf(
                                Definisjon.KVALITETSSIKRING,
                                Definisjon.FATTE_VEDTAK,
                            )
                        }
                        .mapNotNull { it.løstAv() }
                        .toSet()
                        .takeIf { it.isNotEmpty() }
                        ?.join(separator = " ") { Span(ReferanseBruker(it), Tekst(".")) }
                        ?: Tekst("—"),
                ),
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
        fun dokumentInfo(mottattDokument: MottattDokument): LøpendeTekst {
            return Span(
                PrettyEnum(mottattDokument.type),
                Tekst(" mottatt "),
                Tidspunkt(mottattDokument.mottattTidspunkt),
                Tekst(", registrert i saksbehandlingssystemet "),
                Tidspunkt(mottattDokument.opprettetTid),
                Tekst("."),
                mottattDokument.ustrukturerteData()?.let { Tekst(" Strukturert data: $it") }
            )
        }

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
        return Seksjon(
            "Dokumenter",
            Seksjon(
                "Nye dokumenter for behandlingen",
                Dict(
                    mottatteDokumenter
                        .filter { it.behandlingId == behandling.id }
                        .mapNotNull {
                            referanse(it)?.let { referanse -> referanse to dokumentInfo(it) }
                        }
                )
            ),
            Seksjon(
                "Dokumenter fra tidligere behandlinger",
                Dict(
                    mottatteDokumenter
                        .filter { it.behandlingId != behandling.id }
                        .filter { it.opprettetTid <= behandling.opprettetTidspunkt }
                        .mapNotNull {
                            referanse(it)?.let { referanse ->
                                referanse to
                                        Span(
                                            dokumentInfo(it),
                                            it.behandlingId?.let {
                                                Span(
                                                    Tekst(" Behandlet i "),
                                                    ReferanseBehandling(it)
                                                )
                                            }
                                        )
                            }
                        }
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
    vurderingsoverskrift(
        this.vurdertIBehandling,
        bruktForPeriode,
        this.vurdertAv
    ),
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
