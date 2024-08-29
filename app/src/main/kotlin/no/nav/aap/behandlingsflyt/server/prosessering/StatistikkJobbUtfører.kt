package no.nav.aap.behandlingsflyt.server.prosessering

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Beregningsgrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.BeregningsgrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Grunnlag11_19
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagUføre
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagYrkesskade
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.hendelse.avløp.AvklaringsbehovHendelseDto
import no.nav.aap.behandlingsflyt.hendelse.avløp.AvsluttetBehandlingHendelseDTO
import no.nav.aap.behandlingsflyt.hendelse.avløp.BehandlingFlytStoppetHendelse
import no.nav.aap.behandlingsflyt.hendelse.avløp.EndringDTO
import no.nav.aap.behandlingsflyt.hendelse.statistikk.StatistikkGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.json.DefaultJsonMapper
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.statistikk.api_kontrakt.AvklaringsbehovHendelse
import no.nav.aap.statistikk.api_kontrakt.AvsluttetBehandlingDTO
import no.nav.aap.statistikk.api_kontrakt.BehovType
import no.nav.aap.statistikk.api_kontrakt.BeregningsgrunnlagDTO
import no.nav.aap.statistikk.api_kontrakt.Definisjon
import no.nav.aap.statistikk.api_kontrakt.Endring
import no.nav.aap.statistikk.api_kontrakt.EndringStatus
import no.nav.aap.statistikk.api_kontrakt.Grunnlag11_19DTO
import no.nav.aap.statistikk.api_kontrakt.GrunnlagUføreDTO
import no.nav.aap.statistikk.api_kontrakt.GrunnlagYrkesskadeDTO
import no.nav.aap.statistikk.api_kontrakt.MottaStatistikkDTO
import no.nav.aap.statistikk.api_kontrakt.TilkjentYtelseDTO
import no.nav.aap.statistikk.api_kontrakt.TilkjentYtelsePeriodeDTO
import no.nav.aap.statistikk.api_kontrakt.TypeBehandling
import no.nav.aap.statistikk.api_kontrakt.UføreType
import no.nav.aap.statistikk.api_kontrakt.Utfall
import no.nav.aap.statistikk.api_kontrakt.VilkårDTO
import no.nav.aap.statistikk.api_kontrakt.VilkårsPeriodeDTO
import no.nav.aap.statistikk.api_kontrakt.VilkårsResultatDTO
import no.nav.aap.statistikk.api_kontrakt.Vilkårtype
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(StatistikkJobbUtfører::class.java)

enum class StatistikkType {
    BehandlingStoppet, AvsluttetBehandling
}

class StatistikkJobbUtfører(
    private val statistikkGateway: StatistikkGateway,
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val behandlingRepository: BehandlingRepository,
    private val sakService: SakService,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val beregningsgrunnlagRepository: BeregningsgrunnlagRepository
) : JobbUtfører {
    override fun utfør(input: JobbInput) {
        log.info("Utfører jobbinput statistikk: $input")
        val payload = input.payload()

        val type = StatistikkType.valueOf(input.parameter("statistikk-type"))

        when (type) {
            StatistikkType.BehandlingStoppet -> håndterBehandlingStoppet(payload)
            StatistikkType.AvsluttetBehandling -> håndterAvsluttetBehandling(payload)
        }

    }

    private fun håndterBehandlingStoppet(payload: String) {
        val hendelse = DefaultJsonMapper.fromJson<BehandlingFlytStoppetHendelse>(payload)

        val statistikkHendelse = oversettHendelseTilKontrakt(hendelse)

        statistikkGateway.avgiStatistikk(
            statistikkHendelse
        )
    }

    private fun oversettHendelseTilKontrakt(hendelse: BehandlingFlytStoppetHendelse): MottaStatistikkDTO {
        val statistikkHendelse = MottaStatistikkDTO(
            saksnummer = hendelse.saksnummer.toString(),
            behandlingType = typeBehandlingTilStatistikkKontrakt(hendelse.behandlingType),
            status = hendelse.status.toString(),
            ident = hendelse.personIdent,
            avklaringsbehov = hendelse.avklaringsbehov.map { avklaringsbehovHendelseDto ->
                AvklaringsbehovHendelse(
                    definisjon = Definisjon(
                        type = avklaringsbehovHendelseDto.definisjon.type,
                        behovType = behovTypeTilStatistikkKontraktBehovsType(avklaringsbehovHendelseDto),
                        løsesISteg = avklaringsbehovHendelseDto.definisjon.løsesISteg.toString()
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
            behandlingOpprettetTidspunkt = hendelse.opprettetTidspunkt,
        )
        return statistikkHendelse
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

    private fun typeBehandlingTilStatistikkKontrakt(typeBehandling: no.nav.aap.verdityper.sakogbehandling.TypeBehandling): TypeBehandling =
        when (typeBehandling) {
            no.nav.aap.verdityper.sakogbehandling.TypeBehandling.Førstegangsbehandling -> TypeBehandling.Førstegangsbehandling
            no.nav.aap.verdityper.sakogbehandling.TypeBehandling.Revurdering -> TypeBehandling.Revurdering
            no.nav.aap.verdityper.sakogbehandling.TypeBehandling.Tilbakekreving -> TypeBehandling.Tilbakekreving
            no.nav.aap.verdityper.sakogbehandling.TypeBehandling.Klage -> TypeBehandling.Klage
        }

    /**
     * Skal kalles når en behandling er avsluttet for å levere statistikk til statistikk-appen.
     * Payload er JSON siden dette kommer fra en jobb.
     */
    private fun håndterAvsluttetBehandling(payload: String) {
        val hendelse = DefaultJsonMapper.fromJson<AvsluttetBehandlingHendelseDTO>(payload)

        val behandling = behandlingRepository.hent(hendelse.behandlingId)
        val vilkårsresultat = vilkårsresultatRepository.hent(hendelse.behandlingId)
        val sak = sakService.hent(behandling.sakId)

        val tilkjentYtelse = tilkjentYtelseRepository.hentHvisEksisterer(behandling.id)

        val tilkjentYtelseDTO = TilkjentYtelseDTO(perioder = tilkjentYtelse?.map {
            TilkjentYtelsePeriodeDTO(
                fraDato = it.periode.fom,
                tilDato = it.periode.tom,
                dagsats = it.verdi.dagsats.verdi().toDouble(),
                gradering = it.verdi.gradering.prosentverdi().toDouble()
            )
        } ?: listOf())

        if (tilkjentYtelseDTO.perioder.isEmpty()) {
            log.info("Ingen tilkjente ytelser knyttet til avsluttet behandling ${behandling.id}.")
        }

        val grunnlag = beregningsgrunnlagRepository.hentHvisEksisterer(hendelse.behandlingId)

        val beregningsGrunnlagDTO: BeregningsgrunnlagDTO = beregningsgrunnlagDTO(grunnlag)

        log.info("Kaller aap-statistikk for sak ${sak.saksnummer}.")

        statistikkGateway.avsluttetBehandling(
            AvsluttetBehandlingDTO(
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
                beregningsGrunnlag = beregningsGrunnlagDTO
            )
        )
    }

    private fun beregningsgrunnlagDTO(
        grunnlag: Beregningsgrunnlag?
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
                uføreInntektIKroner = grunnlag.uføreInntekterFraForegåendeÅr()[0].inntektIKroner.verdi(), //TODO: Fjerne dette feltet og erstatt med uføreinntektene
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

        null -> throw RuntimeException("Beregningsgrunnlag kan ikke være null.")
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
                BeregningsgrunnlagRepository(connection)
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