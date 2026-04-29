package no.nav.aap.behandlingsflyt.prosessering.statistikk

import no.nav.aap.behandlingsflyt.behandling.Resultat
import no.nav.aap.behandlingsflyt.behandling.ResultatUtleder
import no.nav.aap.behandlingsflyt.behandling.StansOpphørService
import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.AvbrytRevurderingService
import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Beregningsgrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.BeregningsgrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Grunnlag11_19
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagUføre
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagYrkesskade
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.GjeldendeStansEllerOpphør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.Opphør
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.stansopphør.Stans
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.Underveisperiode
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.IKlageresultatUtleder
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.KlageResultatType
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.KlageresultatUtleder
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsopptrapping.ArbeidsopptrappingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsopptrapping.perioderMedArbeidsopptrapping
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurderingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningstidspunktVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status.AVSLUTTET
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.AvsluttetBehandlingDTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.BeregningsgrunnlagDTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.Diagnoser
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.Fritakvurdering
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.Grunnlag11_19DTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.GrunnlagUføreDTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.GrunnlagYrkesskadeDTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.PeriodeDTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.ResultatKode
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.RettighetstypePeriode
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.TilkjentYtelseDTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.TilkjentYtelsePeriodeDTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.Uføre
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.UføreType
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.Utfall
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.VilkårDTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.VilkårsPeriodeDTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.VilkårsResultatDTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vilkårtype
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.Avslagstype as AvslagstypeDTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.Avslagsårsak as AvslagsårsakDTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.StansEllerOpphør as StansEllerOpphørDTO

class AvsluttetBehandlingTilStatistikk(
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val behandlingRepository: BehandlingRepository,
    private val sakService: SakService,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val beregningsgrunnlagRepository: BeregningsgrunnlagRepository,
    private val beregningVurderingRepository: BeregningVurderingRepository,
    private val sykdomRepository: SykdomRepository,
    private val underveisRepository: UnderveisRepository,
    private val vedtakService: VedtakService,
    trukketSøknadService: TrukketSøknadService,
    private val klageresultatUtleder: IKlageresultatUtleder,
    private val avbrytRevurderingService: AvbrytRevurderingService,
    private val meldepliktRepository: MeldepliktRepository,
    private val arbeidsopptrappingRepository: ArbeidsopptrappingRepository,
    private val stansOpphørService: StansOpphørService,
) {

    constructor(repositoryProvider: RepositoryProvider) : this(
        vilkårsresultatRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
        sakService = SakService(repositoryProvider.provide(), repositoryProvider.provide()),
        tilkjentYtelseRepository = repositoryProvider.provide(),
        beregningsgrunnlagRepository = repositoryProvider.provide(),
        beregningVurderingRepository = repositoryProvider.provide(),
        sykdomRepository = repositoryProvider.provide(),
        underveisRepository = repositoryProvider.provide(),
        vedtakService = VedtakService(repositoryProvider),
        trukketSøknadService = TrukketSøknadService(repositoryProvider.provide()),
        klageresultatUtleder = KlageresultatUtleder(repositoryProvider),
        avbrytRevurderingService = AvbrytRevurderingService(repositoryProvider),
        meldepliktRepository = repositoryProvider.provide(),
        arbeidsopptrappingRepository = repositoryProvider.provide(),
        stansOpphørService = StansOpphørService(
            repositoryProvider.provide(),
            repositoryProvider.provide(), repositoryProvider.provide()
        )
    )

    private val log = LoggerFactory.getLogger(javaClass)

    private val resultatUtleder =
        ResultatUtleder(underveisRepository, behandlingRepository, trukketSøknadService, avbrytRevurderingService)

    /**
     * Skal kalles når en behandling er avsluttet for å levere statistikk til statistikk-appen.
     * Payload er JSON siden dette kommer fra en jobb.
     */
    fun hentAvsluttetBehandlingDTO(hendelse: BehandlingFlytStoppetHendelseTilStatistikk): AvsluttetBehandlingDTO {
        val behandling = behandlingRepository.hent(hendelse.referanse)
        val vilkårsresultat = vilkårsresultatRepository.hent(behandling.id)
        val sak = sakService.hent(behandling.sakId)

        if (behandling.status() != AVSLUTTET) {
            log.warn("Kjører statistikkjobb for behandling som ikke er avsluttet. Behandling-ref: ${behandling.referanse.referanse}. Sak: ${sak.saksnummer}")
        }

        val vedtakTidspunkt = vedtakService.vedtakstidspunkt(behandling)

        val tilkjentYtelse = mapTilkjentYtelse(behandling)

        if (tilkjentYtelse == null) {
            log.info("Ingen tilkjente ytelser knyttet til avsluttet behandling ${behandling.id}.")
        }

        val grunnlag = beregningsgrunnlagRepository.hentHvisEksisterer(behandling.id)
        val beregningstidspunktVurdering =
            beregningVurderingRepository.hentHvisEksisterer(behandling.id)?.tidspunktVurdering

        val beregningsGrunnlagDTO: BeregningsgrunnlagDTO? =
            if (grunnlag == null || beregningstidspunktVurdering == null) null
            else beregningsgrunnlagDTO(grunnlag, beregningstidspunktVurdering)

        log.info("Kaller aap-statistikk for sak ${sak.saksnummer} og behandling ${behandling.referanse}")

        val underveistidslinje = underveisRepository
            .hentHvisEksisterer(behandling.id)
            ?.somTidslinje().orEmpty()

        val rettighetstypePerioder = hentRettighetstypePerioder(underveistidslinje)

        val institusjonsopphold =
            underveistidslinje
                .map { it.institusjonsoppholdReduksjon }
                .filter { it.verdi.prosentverdi() > 0 }
                .komprimer()
                .perioder()
                .toList()

        val fritaksvurderinger = hentFritaksvurderinger(behandling)

        val perioderMedArbeidsopptrapping =
            arbeidsopptrappingRepository.hentHvisEksisterer(behandling.id).perioderMedArbeidsopptrapping()

        val vedtattStansOpphør = if (behandling.typeBehandling()
                .erYtelsesbehandling() && !avbrytRevurderingService.revurderingErAvbrutt(behandling.id)
        ) stansOpphørService.vedtattStansOpphør(behandling.id) else emptyList()

        return AvsluttetBehandlingDTO(
            vilkårsResultat = VilkårsResultatDTO(
                typeBehandling = behandling.typeBehandling(), vilkår = vilkårsresultat.alle().map { res ->
                    VilkårDTO(
                        vilkårType = Vilkårtype.valueOf(res.type.toString()),
                        perioder = res.vilkårsperioder().map { periode ->
                            VilkårsPeriodeDTO(
                                fraDato = periode.periode.fom,
                                tilDato = periode.periode.tom,
                                utfall = Utfall.valueOf(periode.utfall.toString()),
                                manuellVurdering = periode.manuellVurdering,
                                innvilgelsesårsak = periode.innvilgelsesårsak?.toString(),
                                avslagsårsak = periode.avslagsårsak?.toString()
                            )
                        })
                }),
            tilkjentYtelse = TilkjentYtelseDTO(perioder = tilkjentYtelse.orEmpty()),
            beregningsGrunnlag = beregningsGrunnlagDTO,
            diagnoser = hentDiagnose(behandling),
            rettighetstypePerioder = rettighetstypePerioder,
            resultat = hentResultat(behandling),
            vedtakstidspunkt = vedtakTidspunkt,
            fritaksvurderinger = fritaksvurderinger,
            perioderMedArbeidsopptrapping = perioderMedArbeidsopptrapping.map { PeriodeDTO(it.fom, it.tom) },
            institusjonsopphold = institusjonsopphold.map { PeriodeDTO(it.fom, it.tom) },
            vedtattStansOpphør = vedtattStansOpphør.map { it.tilKontrakt() }
        )
    }

    private fun hentRettighetstypePerioder(underveistidslinje: Tidslinje<Underveisperiode>): List<RettighetstypePeriode> {
        val rettighetstypePerioder = underveistidslinje
            .mapNotNull { it.rettighetsType }
            .komprimer().segmenter().map {
                RettighetstypePeriode(
                    fraDato = it.periode.fom,
                    tilDato = it.periode.tom,
                    rettighetstype = it.verdi.tilKontrakt()
                )
            }
        return rettighetstypePerioder
    }

    private fun hentFritaksvurderinger(behandling: Behandling): Iterable<Fritakvurdering> =
        meldepliktRepository.hentHvisEksisterer(behandling.id)?.tilTidslinje().orEmpty().komprimer()
            .map { periode, data ->
                Fritakvurdering(data.harFritak, periode.fom, periode.tom)
            }.verdier()

    private fun mapTilkjentYtelse(behandling: Behandling): List<TilkjentYtelsePeriodeDTO>? =
        tilkjentYtelseRepository.hentHvisEksisterer(behandling.id)
            ?.map { Segment(it.periode, it.tilkjent) }
            ?.let(::Tidslinje)?.mapValue { it }?.komprimer()?.segmenter()?.map {
                val verdi = it.verdi
                TilkjentYtelsePeriodeDTO(
                    fraDato = it.periode.fom,
                    tilDato = it.periode.tom,
                    dagsats = verdi.dagsats.verdi().toDouble(),
                    gradering = verdi.gradering.prosentverdi().toDouble(),
                    redusertDagsats = verdi.redusertDagsats().verdi().toDouble(),
                    antallBarn = verdi.antallBarn,
                    barnepensjonDagsats = verdi.barnepensjonDagsats.verdi().toDouble(),
                    barnetilleggSats = verdi.barnetilleggsats.verdi().toDouble(),
                    barnetillegg = verdi.barnetillegg.verdi().toDouble(),
                    utbetalingsdato = verdi.utbetalingsdato,
                    minsteSats = verdi.tilKontrakt(),
                    samordningGradering = verdi.graderingGrunnlag.samordningGradering.prosentverdi().toDouble(),
                    institusjonGradering = verdi.graderingGrunnlag.institusjonGradering.prosentverdi().toDouble(),
                    arbeidGradering = verdi.graderingGrunnlag.arbeidGradering.prosentverdi().toDouble(),
                    samordningUføregradering = verdi.graderingGrunnlag.samordningUføregradering.prosentverdi()
                        .toDouble(),
                    samordningArbeidsgiverGradering = verdi.graderingGrunnlag.samordningArbeidsgiverGradering.prosentverdi()
                        .toDouble(),
                    meldepliktGradering = verdi.graderingGrunnlag.meldepliktGradering.prosentverdi().toDouble(),
                )
            }

    private fun hentDiagnose(behandling: Behandling): Diagnoser? {
        val sykdomsvurdering = sykdomRepository.hentHvisEksisterer(behandling.id)
            ?.sykdomsvurderinger.orEmpty()
            .filter { it.diagnose != null }
            .maxByOrNull { it.opprettet }

        if (sykdomsvurdering == null) {
            log.info("Fant ikke sykdomsvurdering for behandling ${behandling.referanse} (id: ${behandling.id})")
            return null
        }

        val diagnose = sykdomsvurdering.diagnose
        if (diagnose?.hoveddiagnose == null) {
            log.info("Fant sykdomsvurdering, men ingen diagnose eller kodeverk for behandling ${behandling.referanse} (id: ${behandling.id})")
            return null
        }

        return Diagnoser(
            kodeverk = diagnose.kodeverk,
            diagnosekode = diagnose.hoveddiagnose,
            bidiagnoser = diagnose.bidiagnoser.orEmpty(),
        )
    }

    private fun hentResultat(behandling: Behandling): ResultatKode? {
        return when (behandling.typeBehandling()) {
            TypeBehandling.Førstegangsbehandling -> {
                resultatUtleder.utledResultatFørstegangsBehandling(behandling.id).let {
                    when (it) {
                        Resultat.INNVILGELSE -> ResultatKode.INNVILGET
                        Resultat.AVSLAG -> ResultatKode.AVSLAG
                        Resultat.TRUKKET -> ResultatKode.TRUKKET
                        Resultat.AVBRUTT -> ResultatKode.AVBRUTT
                    }
                }
            }

            TypeBehandling.Klage -> {
                klageresultatUtleder.utledKlagebehandlingResultat(behandling.id).type.let {
                    when (it) {
                        KlageResultatType.OPPRETTHOLDES -> ResultatKode.KLAGE_OPPRETTHOLDES
                        KlageResultatType.OMGJØRES -> ResultatKode.KLAGE_OMGJØRES
                        KlageResultatType.DELVIS_OMGJØRES -> ResultatKode.KLAGE_DELVIS_OMGJØRES
                        KlageResultatType.AVSLÅTT -> ResultatKode.KLAGE_AVSLÅTT
                        KlageResultatType.TRUKKET -> ResultatKode.KLAGE_TRUKKET
                        KlageResultatType.UFULLSTENDIG -> null
                    }
                }
            }

            TypeBehandling.Revurdering -> {
                resultatUtleder.utledRevurderingResultat(behandling.id).let {
                    when (it) {
                        Resultat.AVBRUTT -> ResultatKode.AVBRUTT
                        else -> null
                    }
                }
            }

            TypeBehandling.Tilbakekreving,
            TypeBehandling.SvarFraAndreinstans,
            TypeBehandling.OppfølgingsBehandling,
            TypeBehandling.Aktivitetsplikt,
            TypeBehandling.Aktivitetsplikt11_9 -> {
                null
            }
        }
    }

    private fun beregningsgrunnlagDTO(
        grunnlag: Beregningsgrunnlag,
        tidspunktVurdering: BeregningstidspunktVurdering,
    ): BeregningsgrunnlagDTO = when (grunnlag) {
        is Grunnlag11_19 -> BeregningsgrunnlagDTO(
            grunnlag11_19dto = grunnlag1119dto(grunnlag),
            nedsattArbeidsevneEllerStudieevneDato = tidspunktVurdering.nedsattArbeidsevneEllerStudieevneDato,
            ytterligereNedsattArbeidsevneDato = tidspunktVurdering.ytterligereNedsattArbeidsevneDato,
        )

        is GrunnlagUføre -> BeregningsgrunnlagDTO(
            grunnlagUføre = GrunnlagUføreDTO(
                grunnlaget = grunnlag.grunnlaget().verdi(),
                type = UføreType.valueOf(grunnlag.type().toString()),
                grunnlag = grunnlag1119dto(grunnlag.underliggende()),
                grunnlagYtterligereNedsatt = grunnlag1119dto(grunnlag.underliggendeYtterligereNedsatt()),
                uføreYtterligereNedsattArbeidsevneÅr = grunnlag.uføreYtterligereNedsattArbeidsevneÅr().value,
                uføregrad = grunnlag.uføregrader().maxBy { it.virkningstidspunkt }.uføregrad.prosentverdi(),
                uføregrader = grunnlag.uføregrader().map { Uføre(it.uføregrad.prosentverdi(), it.virkningstidspunkt) },
                uføreInntekterFraForegåendeÅr = grunnlag.uføreInntekterFraForegåendeÅr()
                    .associate { it.år.value.toString() to it.inntektIKroner.verdi().toDouble() }),
            nedsattArbeidsevneEllerStudieevneDato = tidspunktVurdering.nedsattArbeidsevneEllerStudieevneDato,
            ytterligereNedsattArbeidsevneDato = tidspunktVurdering.ytterligereNedsattArbeidsevneDato,
        )

        is GrunnlagYrkesskade -> BeregningsgrunnlagDTO(
            grunnlagYrkesskade = GrunnlagYrkesskadeDTO(
                beregningsgrunnlag = when (grunnlag.underliggende()) {
                    is Grunnlag11_19 -> beregningsgrunnlagDTO(grunnlag.underliggende(), tidspunktVurdering)
                    is GrunnlagUføre -> beregningsgrunnlagDTO(grunnlag.underliggende(), tidspunktVurdering)
                    is GrunnlagYrkesskade -> error("Grunnlagyrkesskade kan ikke ha grunnlag yrkesskade")
                },
                andelYrkesskade = grunnlag.andelYrkesskade().prosentverdi(),
                andelSomSkyldesYrkesskade = grunnlag.andelSomSkyldesYrkesskade().verdi(),
                andelSomIkkeSkyldesYrkesskade = grunnlag.andelSomIkkeSkyldesYrkesskade().verdi(),
                antattÅrligInntektYrkesskadeTidspunktet = grunnlag.antattÅrligInntektYrkesskadeTidspunktet().verdi(),
                benyttetAndelForYrkesskade = grunnlag.benyttetAndelForYrkesskade().prosentverdi(),
                grunnlagForBeregningAvYrkesskadeandel = grunnlag.grunnlagForBeregningAvYrkesskadeandel().verdi(),
                grunnlagEtterYrkesskadeFordel = grunnlag.grunnlagEtterYrkesskadeFordel().verdi(),
                terskelverdiForYrkesskade = grunnlag.terskelverdiForYrkesskade().prosentverdi(),
                yrkesskadeTidspunkt = grunnlag.yrkesskadeTidspunkt().value,
                yrkesskadeinntektIG = grunnlag.yrkesskadeinntektIG().verdi(),
                grunnlaget = grunnlag.grunnlaget().verdi(),
                inkludererUføre = grunnlag.underliggende() is GrunnlagUføre
            ),
            nedsattArbeidsevneEllerStudieevneDato = tidspunktVurdering.nedsattArbeidsevneEllerStudieevneDato,
            ytterligereNedsattArbeidsevneDato = tidspunktVurdering.ytterligereNedsattArbeidsevneDato,
        )
    }

    private fun grunnlag1119dto(beregningsgrunnlag: Grunnlag11_19) = Grunnlag11_19DTO(
        inntekter = beregningsgrunnlag.inntekter()
            .associate { it.år.value.toString() to it.inntektIKroner.verdi().toDouble() },
        grunnlaget = beregningsgrunnlag.grunnlaget().verdi().toDouble(),
        er6GBegrenset = beregningsgrunnlag.inntekter().any { it.er6GBegrenset },
        erGjennomsnitt = beregningsgrunnlag.erGjennomsnitt(),
    )

    private fun GjeldendeStansEllerOpphør.tilKontrakt(): StansEllerOpphørDTO {
        val type = when (vurdering) {
            is Stans -> AvslagstypeDTO.STANS
            is Opphør -> AvslagstypeDTO.OPPHØR
        }
        return StansEllerOpphørDTO(
            type = type,
            fom = fom,
            årsaker = vurdering.årsaker.map { AvslagsårsakDTO.valueOf(it.name) }.toSet()
        )
    }

}