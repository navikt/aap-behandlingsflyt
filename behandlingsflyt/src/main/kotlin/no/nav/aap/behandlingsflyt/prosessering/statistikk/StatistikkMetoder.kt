package no.nav.aap.behandlingsflyt.prosessering.statistikk

import no.nav.aap.behandlingsflyt.behandling.Resultat
import no.nav.aap.behandlingsflyt.behandling.ResultatUtleder
import no.nav.aap.behandlingsflyt.behandling.avbrytrevurdering.AvbrytRevurderingService
import no.nav.aap.behandlingsflyt.behandling.søknad.TrukketSøknadService
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakService
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Beregningsgrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.BeregningsgrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Grunnlag11_19
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagUføre
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagYrkesskade
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.MeldekortRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.dokument.KlagedokumentInformasjonUtleder
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.påklagetbehandling.PåklagetBehandlingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.påklagetbehandling.PåklagetVedtakType
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.IKlageresultatUtleder
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.KlageResultatType
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.resultat.KlageresultatUtleder
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsopptrapping.ArbeidsopptrappingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsopptrapping.perioderMedArbeidsopptrapping
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status.AVSLUTTET
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.ArbeidIPeriode
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.AvsluttetBehandlingDTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.BeregningsgrunnlagDTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.Diagnoser
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.Fritakvurdering
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.Grunnlag11_19DTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.GrunnlagUføreDTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.GrunnlagYrkesskadeDTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.MeldekortDTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.PeriodeDTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.ResultatKode
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.RettighetstypePeriode
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.StoppetBehandling
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.TilkjentYtelseDTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.TilkjentYtelsePeriodeDTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.Uføre
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.UføreType
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.Utfall
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.VilkårDTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.VilkårsPeriodeDTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.VilkårsResultatDTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vilkårtype
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vurderingsbehov
import no.nav.aap.behandlingsflyt.pip.PipService
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.tidslinje.Segment
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.tidslinje.orEmpty
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.verdityper.dokument.Kanal
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

class StatistikkMetoder(
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val behandlingRepository: BehandlingRepository,
    private val sakService: SakService,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val beregningsgrunnlagRepository: BeregningsgrunnlagRepository,
    private val pipService: PipService,
    private val dokumentRepository: MottattDokumentRepository,
    private val sykdomRepository: SykdomRepository,
    private val underveisRepository: UnderveisRepository,
    private val meldekortRepository: MeldekortRepository,
    private val påklagetBehandlingRepository: PåklagetBehandlingRepository,
    private val vedtakService: VedtakService,
    private val klagedokumentInformasjonUtleder: KlagedokumentInformasjonUtleder,
    trukketSøknadService: TrukketSøknadService,
    private val klageresultatUtleder: IKlageresultatUtleder,
    avbrytRevurderingService: AvbrytRevurderingService,
    private val meldepliktRepository: MeldepliktRepository,
    private val arbeidsopptrappingRepository: ArbeidsopptrappingRepository,
) {

    constructor(repositoryProvider: RepositoryProvider) : this(
        vilkårsresultatRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
        sakService = SakService(repositoryProvider.provide(), repositoryProvider.provide()),
        tilkjentYtelseRepository = repositoryProvider.provide(),
        beregningsgrunnlagRepository = repositoryProvider.provide(),
        pipService = PipService(repositoryProvider),
        dokumentRepository = repositoryProvider.provide(),
        sykdomRepository = repositoryProvider.provide(),
        underveisRepository = repositoryProvider.provide(),
        meldekortRepository = repositoryProvider.provide(),
        påklagetBehandlingRepository = repositoryProvider.provide(),
        vedtakService = VedtakService(repositoryProvider),
        trukketSøknadService = TrukketSøknadService(repositoryProvider.provide()),
        klagedokumentInformasjonUtleder = KlagedokumentInformasjonUtleder(repositoryProvider),
        klageresultatUtleder = KlageresultatUtleder(repositoryProvider),
        avbrytRevurderingService = AvbrytRevurderingService(repositoryProvider),
        meldepliktRepository = repositoryProvider.provide(),
        arbeidsopptrappingRepository = repositoryProvider.provide()
    )

    private val log = LoggerFactory.getLogger(javaClass)

    private val resultatUtleder =
        ResultatUtleder(underveisRepository, behandlingRepository, trukketSøknadService, avbrytRevurderingService)

    fun oversettHendelseTilKontrakt(hendelse: BehandlingFlytStoppetHendelseTilStatistikk): StoppetBehandling {
        log.info("Oversetter hendelse for behandling ${hendelse.referanse} og saksnr ${hendelse.saksnummer}")
        val behandling = behandlingRepository.hent(hendelse.referanse)
        val sisteEndring = behandlingRepository.hentStegHistorikk(behandling.id).lastOrNull()?.tidspunkt()
        val relevanteDokumenter = hentRelevanteDokumenterForBehandling(behandling)
        val mottattTidspunkt = utledMottattTidspunkt(behandling, relevanteDokumenter)
        val søknadIder = relevanteDokumenter
            .filter { it.type == InnsendingType.SØKNAD }
            .map { it.referanse.asJournalpostId }

        val kanal = hentSøknadsKanal(behandling, relevanteDokumenter)

        val sak = sakService.hent(hendelse.saksnummer)

        val meldekort = meldekortRepository.hentHvisEksisterer(behandling.id)
        val forrigeBehandlingMeldekort =
            behandling.forrigeBehandlingId?.let { meldekortRepository.hentHvisEksisterer(it) }

        val nyeMeldekort =
            meldekort?.meldekort().orEmpty().toSet().minus(forrigeBehandlingMeldekort?.meldekort().orEmpty().toSet())
                .toList()

        val vurderingsbehovForBehandling = utledVurderingsbehovForBehandling(behandling)
        return StoppetBehandling(
            saksnummer = hendelse.saksnummer.toString(),
            behandlingType = hendelse.behandlingType,
            behandlingStatus = hendelse.status,
            ident = hendelse.personIdent,
            avklaringsbehov = hendelse.avklaringsbehov,
            behandlingReferanse = hendelse.referanse.referanse,
            relatertBehandling = relatertBehandling(behandling),
            behandlingOpprettetTidspunkt = hendelse.opprettetTidspunkt,
            tidspunktSisteEndring = sisteEndring ?: hendelse.hendelsesTidspunkt,
            soknadsFormat = kanal,
            versjon = hendelse.versjon,
            mottattTid = mottattTidspunkt,
            sakStatus = sak.status(),
            hendelsesTidspunkt = hendelse.hendelsesTidspunkt,
            avsluttetBehandling = if (hendelse.status == AVSLUTTET) hentAvsluttetBehandlingDTO(hendelse) else null,
            identerForSak = hentIdenterPåSak(sak.saksnummer),
            vurderingsbehov = vurderingsbehovForBehandling,
            årsakTilOpprettelse = behandling.årsakTilOpprettelse?.name ?: "UDEFINERT",
            opprettetAv = hendelse.opprettetAv,
            nyeMeldekort = nyeMeldekort.map { meldekort ->
                MeldekortDTO(
                    meldekort.journalpostId.identifikator,
                    meldekort.timerArbeidPerPeriode.map {
                        ArbeidIPeriode(it.periode.fom, it.periode.tom, it.timerArbeid.antallTimer)
                    }
                )
            },
            søknadIder = søknadIder
        )
    }

    private fun relatertBehandling(behandling: Behandling): UUID? {
        return when (behandling.typeBehandling()) {
            TypeBehandling.Førstegangsbehandling -> null
            TypeBehandling.Revurdering -> if (behandling.forrigeBehandlingId != null) behandlingRepository.hent(
                behandling.forrigeBehandlingId
            ).referanse.referanse else null

            TypeBehandling.Tilbakekreving -> TODO()
            TypeBehandling.Klage -> {
                val påklagetBehandling =
                    påklagetBehandlingRepository.hentGjeldendeVurderingMedReferanse(behandling.referanse)

                check(påklagetBehandling == null || påklagetBehandling.påklagetVedtakType == PåklagetVedtakType.KELVIN_BEHANDLING) {
                    "Hvis det klages på en behandling utenfor Kelvin, må dette være synlig i statistikk med referanse til eksternt system."
                }

                påklagetBehandling?.referanse?.referanse
            }

            TypeBehandling.SvarFraAndreinstans -> {
                klagedokumentInformasjonUtleder.utledKlagebehandlingForSvar(behandling.id).referanse
            }

            TypeBehandling.OppfølgingsBehandling -> null
            TypeBehandling.Aktivitetsplikt -> null
            TypeBehandling.Aktivitetsplikt11_9 -> null
        }
    }

    private fun utledVurderingsbehovForBehandling(behandling: Behandling): List<Vurderingsbehov> =
        behandling.vurderingsbehov().map { it.tilKontraktVurderingsbehov() }.distinct()

    private fun hentIdenterPåSak(saksnummer: Saksnummer): List<String> {
        return pipService.finnIdenterPåSak(saksnummer).map { it.ident }
    }

    private fun hentSøknadsKanal(behandling: Behandling, hentDokumenterAvType: Set<MottattDokument>): Kanal {
        val kanaler = hentDokumenterAvType.filter { it.behandlingId == behandling.id }.map { it.kanal }

        // Om minst én av søknadene er papir, regn med at hele behandlingen er papir
        return kanaler.reduceOrNull { acc, curr ->
            when (acc) {
                Kanal.DIGITAL -> curr
                Kanal.PAPIR -> Kanal.PAPIR
            }
        } ?: Kanal.DIGITAL
    }

    private fun utledMottattTidspunkt(
        behandling: Behandling, mottatteDokumenter: Set<MottattDokument>
    ): LocalDateTime {
        val mottattTidspunkt =
            mottatteDokumenter.filter { it.behandlingId == behandling.id }
                .minByOrNull { it.opprettetTid }?.mottattTidspunkt

        if (mottattTidspunkt == null) {
            log.info("Ingen søknader funnet for behandling ${behandling.referanse} av type ${behandling.typeBehandling()}.")
            return behandling.opprettetTidspunkt
        }
        return minOf(mottattTidspunkt, behandling.opprettetTidspunkt)
    }

    private fun hentRelevanteDokumenterForBehandling(behandling: Behandling): Set<MottattDokument> {
        val hentDokumenterAvType = dokumentRepository.hentDokumenterAvType(
            behandling.id,
            InnsendingType.entries
        )
        return hentDokumenterAvType
    }

    /**
     * Skal kalles når en behandling er avsluttet for å levere statistikk til statistikk-appen.
     * Payload er JSON siden dette kommer fra en jobb.
     */
    private fun hentAvsluttetBehandlingDTO(hendelse: BehandlingFlytStoppetHendelseTilStatistikk): AvsluttetBehandlingDTO {
        val behandling = behandlingRepository.hent(hendelse.referanse)
        val vilkårsresultat = vilkårsresultatRepository.hent(behandling.id)
        val sak = sakService.hent(behandling.sakId)

        if (behandling.status() != AVSLUTTET) {
            log.warn("Kjører statistikkjobb for behandling som ikke er avsluttet. Behandling-ref: ${behandling.referanse.referanse}. Sak: ${sak.saksnummer}")
        }

        val vedtakTidspunkt = vedtakService.vedtakstidspunkt(behandling)

        val tilkjentYtelse =
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
                        barnetilleggSats = verdi.barnetilleggsats.verdi().toDouble(),
                        barnetillegg = verdi.barnetillegg.verdi().toDouble(),
                        utbetalingsdato = verdi.utbetalingsdato,
                        minsteSats = verdi.tilKontrakt()
                    )
                }

        if (tilkjentYtelse == null) {
            log.info("Ingen tilkjente ytelser knyttet til avsluttet behandling ${behandling.id}.")
        }

        val grunnlag = beregningsgrunnlagRepository.hentHvisEksisterer(behandling.id)

        val beregningsGrunnlagDTO: BeregningsgrunnlagDTO? =
            if (grunnlag == null) null else beregningsgrunnlagDTO(grunnlag)

        log.info("Kaller aap-statistikk for sak ${sak.saksnummer} og behandling ${behandling.referanse}")

        val rettighetstypePerioder = underveisRepository.hentHvisEksisterer(behandling.id)?.perioder.orEmpty()
            .filter { it.rettighetsType != null }.map { Segment(it.periode, it.rettighetsType) }.let(::Tidslinje)
            .komprimer().segmenter().map {
                RettighetstypePeriode(
                    fraDato = it.periode.fom,
                    tilDato = it.periode.tom,
                    rettighetstype = it.verdi.tilKontrakt()
                )
            }

        val fritaksvurderinger =
            meldepliktRepository.hentHvisEksisterer(behandling.id)?.tilTidslinje().orEmpty().komprimer()
                .map { periode, data ->
                    Fritakvurdering(data.harFritak, periode.fom, periode.tom)
                }.verdier()

        val perioderMedArbeidsopptrapping =
            arbeidsopptrappingRepository.hentHvisEksisterer(behandling.id).perioderMedArbeidsopptrapping()

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
            perioderMedArbeidsopptrapping = perioderMedArbeidsopptrapping.map { PeriodeDTO(it.fom, it.tom) }
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

    private fun hentDiagnose(behandling: Behandling): Diagnoser? {
        val sykdomsvurdering = sykdomRepository.hentHvisEksisterer(behandling.id)?.sykdomsvurderinger.orEmpty()
            .maxByOrNull { it.opprettet }

        if (sykdomsvurdering == null) {
            log.info("Fant ikke sykdomsvurdering for behandling ${behandling.referanse} (id: ${behandling.id})")
            return null
        }

        if (sykdomsvurdering.hoveddiagnose == null || sykdomsvurdering.kodeverk == null) {
            log.info("Fant sykdomsvurdering, men ingen diagnose eller kodeverk for behandling ${behandling.referanse} (id: ${behandling.id})")
            return null
        }

        return Diagnoser(
            kodeverk = sykdomsvurdering.kodeverk,
            diagnosekode = sykdomsvurdering.hoveddiagnose,
            bidiagnoser = sykdomsvurdering.bidiagnoser.orEmpty(),
        )
    }

    private fun beregningsgrunnlagDTO(
        grunnlag: Beregningsgrunnlag
    ): BeregningsgrunnlagDTO = when (grunnlag) {
        is Grunnlag11_19 -> BeregningsgrunnlagDTO(
            grunnlag11_19dto = grunnlag1119dto(grunnlag),
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
                    .associate { it.år.value.toString() to it.inntektIKroner.verdi().toDouble() })
        )

        is GrunnlagYrkesskade -> BeregningsgrunnlagDTO(
            grunnlagYrkesskade = GrunnlagYrkesskadeDTO(
                beregningsgrunnlag = when (grunnlag.underliggende()) {
                    is Grunnlag11_19 -> beregningsgrunnlagDTO(grunnlag.underliggende())
                    is GrunnlagUføre -> beregningsgrunnlagDTO(grunnlag.underliggende())
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
            )
        )
    }

    private fun grunnlag1119dto(beregningsgrunnlag: Grunnlag11_19) = Grunnlag11_19DTO(
        inntekter = beregningsgrunnlag.inntekter()
            .associate { it.år.value.toString() to it.inntektIKroner.verdi().toDouble() },
        grunnlaget = beregningsgrunnlag.grunnlaget().verdi().toDouble(),
        er6GBegrenset = beregningsgrunnlag.inntekter().any { it.er6GBegrenset },
        erGjennomsnitt = beregningsgrunnlag.erGjennomsnitt(),
    )
}
