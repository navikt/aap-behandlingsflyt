package no.nav.aap.behandlingsflyt.server.prosessering

import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Beregningsgrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.BeregningsgrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Grunnlag11_19
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagUføre
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagYrkesskade
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.MottattDokumentRepository
import no.nav.aap.behandlingsflyt.hendelse.statistikk.StatistikkGateway
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status.AVSLUTTET
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.AvklaringsbehovHendelseDto
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.BehandlingFlytStoppetHendelse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.EndringDTO
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.AvklaringsbehovHendelse
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.Brevkode
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.httpklient.json.DefaultJsonMapper
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.pip.PipRepository
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.AvsluttetBehandlingDTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.BehovType
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.BeregningsgrunnlagDTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.Definisjon
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.Endring
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.EndringStatus
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.Grunnlag11_19DTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.GrunnlagUføreDTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.GrunnlagYrkesskadeDTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.SakStatus
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.StoppetBehandling
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.TilkjentYtelseDTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.TilkjentYtelsePeriodeDTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.UføreType
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.Utfall
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.VilkårDTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.VilkårsPeriodeDTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.VilkårsResultatDTO
import no.nav.aap.behandlingsflyt.kontrakt.statistikk.Vilkårtype
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import no.nav.aap.behandlingsflyt.kontrakt.behandling.Status as BehandlingStatus

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
        val mottattTidspunkt = utledMottattTidspunkt(behandling)

        val forrigeBehandling =
            if (behandling.forrigeBehandlingId != null) behandlingRepository.hent(behandling.forrigeBehandlingId!!) else null

        val sak = sakService.hent(hendelse.saksnummer)

        val statistikkHendelse = StoppetBehandling(
            saksnummer = hendelse.saksnummer.toString(),
            behandlingType = typeBehandlingTilStatistikkKontrakt(hendelse.behandlingType),
            status = behandlingStatusTilStatistikkKontrakt(hendelse.status),
            ident = hendelse.personIdent,
            avklaringsbehov = hendelse.avklaringsbehov.map { avklaringsbehovHendelseDto ->
                AvklaringsbehovHendelse(
                    definisjon = Definisjon(
                        type = avklaringsbehovHendelseDto.definisjon.type.name,
                        behovType = behovTypeTilStatistikkKontraktBehovsType(avklaringsbehovHendelseDto),
                        løsesISteg = stegTypeTilStatistikkKontrakt(avklaringsbehovHendelseDto.definisjon.løsesISteg)
                    ),
                    status = avklaringsBehovStatusTilStatistikkKontrakt(avklaringsbehovHendelseDto),
                    endringer = avklaringsbehovHendelseDto.endringer.map { endring ->
                        Endring(
                            status = endringStatusTilStatistikkKontrakt(endring),
                            tidsstempel = endring.tidsstempel,
                            frist = endring.frist,
                            endretAv = endring.endretAv,
                        )
                    }
                )
            },
            behandlingReferanse = hendelse.referanse.referanse,
            relatertBehandling = forrigeBehandling?.referanse?.referanse,
            behandlingOpprettetTidspunkt = hendelse.opprettetTidspunkt,
            versjon = hendelse.versjon,
            mottattTid = mottattTidspunkt,
            sakStatus = behandlingflytSakStatusTilStatistikk(sak.status()),
            hendelsesTidspunkt = hendelse.hendelsesTidspunkt,
            avsluttetBehandling = if (hendelse.status == AVSLUTTET) hentAvsluttetBehandlingDTO(hendelse) else null,
            identerForSak = hentIdenterPåSak(sak.saksnummer)
        )
        return statistikkHendelse
    }

    private fun hentIdenterPåSak(saksnummer: Saksnummer): List<String> {
        return pipRepository.finnIdenterPåSak(saksnummer).map { it.ident }
    }

    private fun utledMottattTidspunkt(behandling: Behandling): LocalDateTime {
        val hentDokumenterAvType = dokumentRepository.hentDokumenterAvType(
            behandling.sakId,
            Brevkode.SØKNAD
        )

        val mottattTidspunkt = hentDokumenterAvType
            .filter { it.behandlingId == behandling.id }
            .minOfOrNull { it.mottattTidspunkt }

        if (mottattTidspunkt == null) {
            log.info("Ingen søknader funnet for behandling ${behandling.referanse} av type ${behandling.typeBehandling()}.")
            return behandling.opprettetTidspunkt
        }
        return mottattTidspunkt
    }

    private fun behandlingflytSakStatusTilStatistikk(sakStatus: no.nav.aap.behandlingsflyt.sakogbehandling.sak.Status): SakStatus =
        when (sakStatus) {
            no.nav.aap.behandlingsflyt.sakogbehandling.sak.Status.OPPRETTET -> SakStatus.OPPRETTET
            no.nav.aap.behandlingsflyt.sakogbehandling.sak.Status.UTREDES -> SakStatus.UTREDES
            no.nav.aap.behandlingsflyt.sakogbehandling.sak.Status.LØPENDE -> SakStatus.LØPENDE
            no.nav.aap.behandlingsflyt.sakogbehandling.sak.Status.AVSLUTTET -> SakStatus.AVSLUTTET
        }

    private fun behandlingStatusTilStatistikkKontrakt(status: BehandlingStatus): no.nav.aap.behandlingsflyt.kontrakt.statistikk.BehandlingStatus =
        when (status) {
            no.nav.aap.behandlingsflyt.kontrakt.behandling.Status.OPPRETTET -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.BehandlingStatus.OPPRETTET
            no.nav.aap.behandlingsflyt.kontrakt.behandling.Status.UTREDES ->no.nav.aap.behandlingsflyt.kontrakt.statistikk.BehandlingStatus.UTREDES
            no.nav.aap.behandlingsflyt.kontrakt.behandling.Status.IVERKSETTES -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.BehandlingStatus.IVERKSETTES
            AVSLUTTET -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.BehandlingStatus.AVSLUTTET
        }

    private fun endringStatusTilStatistikkKontrakt(endring: EndringDTO): EndringStatus = when (endring.status) {
        Status.OPPRETTET -> EndringStatus.OPPRETTET
        Status.AVSLUTTET -> EndringStatus.AVSLUTTET
        Status.TOTRINNS_VURDERT -> EndringStatus.TOTRINNS_VURDERT
        Status.SENDT_TILBAKE_FRA_BESLUTTER -> EndringStatus.SENDT_TILBAKE_FRA_BESLUTTER
        Status.KVALITETSSIKRET -> EndringStatus.KVALITETSSIKRET
        Status.SENDT_TILBAKE_FRA_KVALITETSSIKRER -> EndringStatus.SENDT_TILBAKE_FRA_KVALITETSSIKRER
        Status.AVBRUTT -> EndringStatus.AVBRUTT
    }

    private fun avklaringsBehovStatusTilStatistikkKontrakt(avklaringsbehovHendelseDto: AvklaringsbehovHendelseDto): EndringStatus =
        when (avklaringsbehovHendelseDto.status) {
            Status.OPPRETTET -> EndringStatus.OPPRETTET
            Status.AVSLUTTET -> EndringStatus.AVSLUTTET
            Status.TOTRINNS_VURDERT -> EndringStatus.TOTRINNS_VURDERT
            Status.SENDT_TILBAKE_FRA_BESLUTTER -> EndringStatus.SENDT_TILBAKE_FRA_BESLUTTER
            Status.KVALITETSSIKRET -> EndringStatus.KVALITETSSIKRET
            Status.SENDT_TILBAKE_FRA_KVALITETSSIKRER -> EndringStatus.SENDT_TILBAKE_FRA_KVALITETSSIKRER
            Status.AVBRUTT -> EndringStatus.AVBRUTT
        }

    private fun behovTypeTilStatistikkKontraktBehovsType(avklaringsbehovHendelseDto: AvklaringsbehovHendelseDto): BehovType {
        // TODO: bedre oversettelse mellom domeneobjekter
        return BehovType.valueOf(avklaringsbehovHendelseDto.definisjon.behovType.toString())
    }

    private fun typeBehandlingTilStatistikkKontrakt(typeBehandling: no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling): TypeBehandling =
        when (typeBehandling) {
            no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling.Førstegangsbehandling -> TypeBehandling.Førstegangsbehandling
            no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling.Revurdering -> TypeBehandling.Revurdering
            no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling.Tilbakekreving -> TypeBehandling.Tilbakekreving
            no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling.Klage -> TypeBehandling.Klage
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
            behandlingsReferanse = behandling.referanse.referanse,
            saksnummer = sak.saksnummer.toString(),
            vilkårsResultat = VilkårsResultatDTO(
                typeBehandling = behandling.typeBehandling().toString(),
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
            hendelsesTidspunkt = hendelse.hendelsesTidspunkt
        )
        return avsluttetBehandlingDTO
    }

    private fun stegTypeTilStatistikkKontrakt(stegType: StegType): no.nav.aap.behandlingsflyt.kontrakt.statistikk.StegType {
        return when (stegType) {
            StegType.START_BEHANDLING -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.StegType.AVKLAR_SYKDOM
            StegType.VURDER_ALDER -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.StegType.VURDER_ALDER
            StegType.VURDER_LOVVALG -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.StegType.VURDER_LOVVALG
            StegType.VURDER_MEDLEMSKAP -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.StegType.VURDER_MEDLEMSKAP
            StegType.AVKLAR_STUDENT -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.StegType.AVKLAR_STUDENT
            StegType.VURDER_BISTANDSBEHOV -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.StegType.VURDER_BISTANDSBEHOV
            StegType.VURDER_SYKEPENGEERSTATNING -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.StegType.VURDER_SYKEPENGEERSTATNING
            StegType.FRITAK_MELDEPLIKT -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.StegType.FRITAK_MELDEPLIKT
            StegType.KVALITETSSIKRING -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.StegType.KVALITETSSIKRING
            StegType.BARNETILLEGG -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.StegType.BARNETILLEGG
            StegType.AVKLAR_SYKDOM -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.StegType.AVKLAR_SYKDOM
            StegType.FASTSETT_ARBEIDSEVNE -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.StegType.FASTSETT_ARBEIDSEVNE
            StegType.FASTSETT_BEREGNINGSTIDSPUNKT -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.StegType.FASTSETT_BEREGNINGSTIDSPUNKT
            StegType.FASTSETT_GRUNNLAG -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.StegType.FASTSETT_GRUNNLAG
            StegType.VIS_GRUNNLAG -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.StegType.VIS_GRUNNLAG
            StegType.FASTSETT_UTTAK -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.StegType.FASTSETT_UTTAK
            StegType.SAMORDNING_GRADERING -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.StegType.SAMORDNING_GRADERING
            StegType.DU_ER_ET_ANNET_STED -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.StegType.DU_ER_ET_ANNET_STED
            StegType.BEREGN_TILKJENT_YTELSE -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.StegType.BEREGN_TILKJENT_YTELSE
            StegType.SIMULERING -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.StegType.SIMULERING
            StegType.FORESLÅ_VEDTAK -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.StegType.FORESLÅ_VEDTAK
            StegType.FATTE_VEDTAK -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.StegType.FATTE_VEDTAK
            StegType.BREV -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.StegType.BREV
            StegType.IVERKSETT_VEDTAK -> no.nav.aap.behandlingsflyt.kontrakt.statistikk.StegType.IVERKSETT_VEDTAK
            StegType.UDEFINERT -> error("Statistikk vil ikke ha denne :)")
        }
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
            val vilkårsresultatRepository = VilkårsresultatRepository(connection)
            val behandlingRepository = BehandlingRepositoryImpl(connection)
            val sakService = SakService(connection)

            return StatistikkJobbUtfører(
                StatistikkGateway(),
                vilkårsresultatRepository,
                behandlingRepository,
                sakService,
                TilkjentYtelseRepository(connection),
                BeregningsgrunnlagRepository(connection),
                pipRepository = PipRepository(connection),
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