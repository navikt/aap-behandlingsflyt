package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Beregningsgrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.BeregningsgrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.BeregningsgrunnlagRepositoryImpl
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Grunnlag11_19
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagUføre
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagYrkesskade
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.hendelse.statistikk.StatistikkGateway
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status.AVSLUTTET
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.BehandlingFlytStoppetHendelse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.AvsluttetBehandlingDTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.BeregningsgrunnlagDTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.Grunnlag11_19DTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.GrunnlagUføreDTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.GrunnlagYrkesskadeDTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.StoppetBehandling
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.TilkjentYtelseDTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.TilkjentYtelsePeriodeDTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.UføreType
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.Utfall
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.VilkårDTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.VilkårsPeriodeDTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.VilkårsResultatDTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vilkårtype
import no.nav.aap.behandlingsflyt.pip.PipRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.ÅrsakTilBehandling
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.httpklient.json.DefaultJsonMapper
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.repository.RepositoryFactory
import no.nav.aap.verdityper.dokument.Kanal
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

private val log = LoggerFactory.getLogger(StatistikkJobbUtfører::class.java)


class StatistikkJobbUtfører(
    private val statistikkGateway: StatistikkGateway,
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val behandlingRepository: BehandlingRepository,
    private val sakService: SakService,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val beregningsgrunnlagRepository: BeregningsgrunnlagRepository,
    private val pipRepository: PipRepository,
    private val dokumentRepository: MottattDokumentRepository,
) : JobbUtfører {
    override fun utfør(input: JobbInput) {
        log.info("Utfører jobbinput statistikk: $input")
        val payload = input.payload()

        håndterBehandlingStoppet(payload)
    }

    private fun håndterBehandlingStoppet(payload: String) {
        val hendelse = DefaultJsonMapper.fromJson<BehandlingFlytStoppetHendelse>(payload)

        val statistikkHendelse = oversettHendelseTilKontrakt(hendelse)

        statistikkGateway.avgiStatistikk(statistikkHendelse)
    }

    private fun oversettHendelseTilKontrakt(hendelse: BehandlingFlytStoppetHendelse): StoppetBehandling {
        log.info("Oversetter hendelse for behandling ${hendelse.referanse} og saksnr ${hendelse.saksnummer}")
        val behandling = behandlingRepository.hent(hendelse.referanse)
        val søknaderForSak = hentSøknanderForSak(behandling)
        val mottattTidspunkt = utledMottattTidspunkt(behandling, søknaderForSak)
        val kanal = hentSøknadsKanal(behandling, søknaderForSak)

        val forrigeBehandling =
            if (behandling.forrigeBehandlingId != null) behandlingRepository.hent(behandling.forrigeBehandlingId!!) else null

        val sak = sakService.hent(hendelse.saksnummer)

        val statistikkHendelse = StoppetBehandling(
            saksnummer = hendelse.saksnummer.toString(),
            behandlingType = hendelse.behandlingType,
            behandlingStatus = hendelse.status,
            ident = hendelse.personIdent,
            avklaringsbehov = hendelse.avklaringsbehov,
            behandlingReferanse = hendelse.referanse.referanse,
            relatertBehandling = forrigeBehandling?.referanse?.referanse,
            behandlingOpprettetTidspunkt = hendelse.opprettetTidspunkt,
            soknadsFormat = kanal,
            versjon = hendelse.versjon,
            mottattTid = mottattTidspunkt,
            sakStatus = sak.status(),
            hendelsesTidspunkt = hendelse.hendelsesTidspunkt,
            avsluttetBehandling = if (hendelse.status == AVSLUTTET) hentAvsluttetBehandlingDTO(hendelse) else null,
            identerForSak = hentIdenterPåSak(sak.saksnummer),
            årsakTilBehandling = behandling.årsaker().map {
                when (it.type) {
                    ÅrsakTilBehandling.MOTTATT_SØKNAD -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.ÅrsakTilBehandling.SØKNAD
                    ÅrsakTilBehandling.MOTTATT_AKTIVITETSMELDING -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.ÅrsakTilBehandling.AKTIVITETSMELDING
                    ÅrsakTilBehandling.MOTTATT_MELDEKORT -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.ÅrsakTilBehandling.MELDEKORT
                    ÅrsakTilBehandling.MOTTATT_LEGEERKLÆRING -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.ÅrsakTilBehandling.LEGEERKLÆRING
                    ÅrsakTilBehandling.MOTTATT_AVVIST_LEGEERKLÆRING -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.ÅrsakTilBehandling.AVVIST_LEGEERKLÆRING
                    ÅrsakTilBehandling.MOTTATT_DIALOGMELDING -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.ÅrsakTilBehandling.DIALOGMELDING
                    ÅrsakTilBehandling.G_REGULERING -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.ÅrsakTilBehandling.G_REGULERING
                }
            }.distinct()
        )
        return statistikkHendelse
    }

    private fun hentIdenterPåSak(saksnummer: Saksnummer): List<String> {
        return pipRepository.finnIdenterPåSak(saksnummer).map { it.ident }
    }

    private fun hentSøknadsKanal(behandling: Behandling, hentDokumenterAvType: Set<MottattDokument>): Kanal {
        val kanaler = hentDokumenterAvType
            .filter { it.behandlingId == behandling.id }
            .map { it.kanal }

        // Om minst én av søknadene er papir, regn med at hele behandlingen er papir
        return kanaler.reduceOrNull() { acc, curr ->
            when (acc) {
                Kanal.DIGITAL -> curr
                Kanal.PAPIR -> Kanal.PAPIR
            }
        } ?: Kanal.DIGITAL
    }

    private fun utledMottattTidspunkt(
        behandling: Behandling,
        hentDokumenterAvType: Set<MottattDokument>
    ): LocalDateTime {
        val mottattTidspunkt = hentDokumenterAvType
            .filter { it.behandlingId == behandling.id }
            .minOfOrNull { it.mottattTidspunkt }

        if (mottattTidspunkt == null) {
            log.info("Ingen søknader funnet for behandling ${behandling.referanse} av type ${behandling.typeBehandling()}.")
            return behandling.opprettetTidspunkt
        }
        return mottattTidspunkt
    }

    private fun hentSøknanderForSak(behandling: Behandling): Set<MottattDokument> {
        val hentDokumenterAvType = dokumentRepository.hentDokumenterAvType(
            behandling.sakId,
            InnsendingType.SØKNAD
        )
        return hentDokumenterAvType
    }

    /**
     * Skal kalles når en behandling er avsluttet for å levere statistikk til statistikk-appen.
     * Payload er JSON siden dette kommer fra en jobb.
     */
    private fun hentAvsluttetBehandlingDTO(hendelse: BehandlingFlytStoppetHendelse): AvsluttetBehandlingDTO {
        val behandling = behandlingRepository.hent(hendelse.referanse)
        val vilkårsresultat = vilkårsresultatRepository.hent(behandling.id)
        val sak = sakService.hent(behandling.sakId)

        if (behandling.status() != AVSLUTTET) {
            log.warn("Kjører statistikkjobb for behandling som ikke er avsluttet. Behandling-ref: ${behandling.referanse.referanse}. Sak: ${sak.saksnummer}")
        }

        val tilkjentYtelse = tilkjentYtelseRepository.hentHvisEksisterer(behandling.id)

        val tilkjentYtelseDTO = TilkjentYtelseDTO(perioder = tilkjentYtelse?.map {
            TilkjentYtelsePeriodeDTO(
                fraDato = it.periode.fom,
                tilDato = it.periode.tom,
                dagsats = it.verdi.dagsats.verdi().toDouble(),
                gradering = it.verdi.gradering.prosentverdi().toDouble()
            )
        } ?: listOf())

        if (tilkjentYtelse == null) {
            log.info("Ingen tilkjente ytelser knyttet til avsluttet behandling ${behandling.id}.")
        }

        val grunnlag =
            beregningsgrunnlagRepository.hentHvisEksisterer(behandling.id)

        val beregningsGrunnlagDTO: BeregningsgrunnlagDTO? =
            if (grunnlag == null) null else beregningsgrunnlagDTO(grunnlag)

        log.info("Kaller aap-statistikk for sak ${sak.saksnummer}.")

        val avsluttetBehandlingDTO = AvsluttetBehandlingDTO(
            vilkårsResultat = VilkårsResultatDTO(
                typeBehandling = behandling.typeBehandling(),
                vilkår = vilkårsresultat.alle().map { res ->
                    VilkårDTO(
                        vilkårType = Vilkårtype.valueOf(res.type.toString()),
                        perioder = res.vilkårsperioder().map { periode ->
                            VilkårsPeriodeDTO(
                                fraDato = periode.periode.fom,
                                tilDato = periode.periode.tom,
                                utfall = Utfall.valueOf(periode.utfall.toString()),
                                manuellVurdering = periode.manuellVurdering,
                                innvilgelsesårsak = periode.innvilgelsesårsak.toString(),
                                avslagsårsak = periode.avslagsårsak.toString()
                            )
                        }
                    )
                }
            ),
            tilkjentYtelse = tilkjentYtelseDTO,
            beregningsGrunnlag = beregningsGrunnlagDTO,
        )
        return avsluttetBehandlingDTO
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
                uføregrad = grunnlag.uføregrad().prosentverdi(),
                uføreInntekterFraForegåendeÅr = grunnlag.uføreInntekterFraForegåendeÅr() // TODO: Vurdere hvilke felter som skal være med fra uføreinntektene
                    .associate { it.år.value.toString() to it.inntektIKroner.verdi().toDouble() }
            )
        )

        is GrunnlagYrkesskade -> BeregningsgrunnlagDTO(
            grunnlagYrkesskade = GrunnlagYrkesskadeDTO(
                beregningsgrunnlag = when (grunnlag.underliggende()) {
                    is Grunnlag11_19 -> beregningsgrunnlagDTO(grunnlag.underliggende())
                    is GrunnlagUføre -> beregningsgrunnlagDTO(grunnlag.underliggende())
                    is GrunnlagYrkesskade -> throw RuntimeException("Grunnlagyrkesskade kan ikke ha grunnlag yrkesskade")
                },
                andelYrkesskade = grunnlag.andelYrkesskade().prosentverdi(),
                andelSomSkyldesYrkesskade = grunnlag.andelSomSkyldesYrkesskade().verdi(),
                andelSomIkkeSkyldesYrkesskade = grunnlag.andelSomIkkeSkyldesYrkesskade().verdi(),
                antattÅrligInntektYrkesskadeTidspunktet = grunnlag.antattÅrligInntektYrkesskadeTidspunktet()
                    .verdi(),
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
        er6GBegrenset = false,
        erGjennomsnitt = false
    )


    companion object : Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            val repositoryFactory = RepositoryFactory(connection)
            val vilkårsresultatRepository = repositoryFactory.create(VilkårsresultatRepository::class)
            val behandlingRepository = repositoryFactory.create(BehandlingRepository::class)
            val sakRepository = repositoryFactory.create(SakRepository::class)
            val pipRepository = repositoryFactory.create(PipRepository::class)
            val sakService = SakService(sakRepository)

            return StatistikkJobbUtfører(
                StatistikkGateway(),
                vilkårsresultatRepository,
                behandlingRepository,
                sakService,
                TilkjentYtelseRepository(connection),
                BeregningsgrunnlagRepositoryImpl(connection),
                pipRepository = pipRepository,
                dokumentRepository = MottattDokumentRepository(connection)
            )
        }

        override fun type(): String {
            return "flyt.statistikk"
        }

        override fun navn(): String {
            return "Lagrer statistikk"
        }

        override fun beskrivelse(): String {
            return "Skal ta i mot data fra steg i en behandling og sender til statistikk-appen."
        }
    }
}