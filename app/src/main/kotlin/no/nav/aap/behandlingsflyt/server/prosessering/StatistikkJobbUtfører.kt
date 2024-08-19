package no.nav.aap.behandlingsflyt.server.prosessering

import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.BeregningsgrunnlagRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Grunnlag11_19
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagUføre
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.GrunnlagYrkesskade
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.hendelse.avløp.AvsluttetBehandlingHendelseDTO
import no.nav.aap.behandlingsflyt.hendelse.avløp.BehandlingFlytStoppetHendelse
import no.nav.aap.behandlingsflyt.hendelse.statistikk.AvsluttetBehandlingDTO
import no.nav.aap.behandlingsflyt.hendelse.statistikk.BeregningsgrunnlagDTO
import no.nav.aap.behandlingsflyt.hendelse.statistikk.Grunnlag11_19DTO
import no.nav.aap.behandlingsflyt.hendelse.statistikk.GrunnlagUføreDTO
import no.nav.aap.behandlingsflyt.hendelse.statistikk.GrunnlagYrkesskadeDTO
import no.nav.aap.behandlingsflyt.hendelse.statistikk.StatistikkGateway
import no.nav.aap.behandlingsflyt.hendelse.statistikk.StatistikkHendelseDTO
import no.nav.aap.behandlingsflyt.hendelse.statistikk.TilkjentYtelseDTO
import no.nav.aap.behandlingsflyt.hendelse.statistikk.TilkjentYtelsePeriodeDTO
import no.nav.aap.behandlingsflyt.hendelse.statistikk.VilkårsResultatDTO
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.json.DefaultJsonMapper
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
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

        statistikkGateway.avgiStatistikk(
            StatistikkHendelseDTO(
                saksnummer = hendelse.saksnummer.toString(),
                behandlingType = hendelse.behandlingType,
                status = hendelse.status,
                ident = hendelse.personIdent,
                avklaringsbehov = hendelse.avklaringsbehov,
                behandlingReferanse = hendelse.referanse,
                behandlingOpprettetTidspunkt = hendelse.opprettetTidspunkt,
            )
        )
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

        val fraDomeneObjekt = VilkårsResultatDTO.fraDomeneObjekt(
            typeBehandling = behandling.typeBehandling(),
            vilkårsresultat = vilkårsresultat
        )

        val tilkjentYtelse = tilkjentYtelseRepository.hentHvisEksisterer(behandling.id)

        val tilkjentYtelseDTO = TilkjentYtelseDTO(perioder = tilkjentYtelse?.map {
            TilkjentYtelsePeriodeDTO(
                fraDato = it.periode.fom,
                tilDato = it.periode.tom,
                dagsats = it.verdi.dagsats.verdi(),
                gradering = it.verdi.gradering.prosentverdi()
            )
        } ?: listOf())

        if (tilkjentYtelseDTO.perioder.isEmpty()) {
            log.info("Ingen tilkjente ytelser knyttet til avsluttet behandling ${behandling.id}.")
        }

        val grunnlag = beregningsgrunnlagRepository.hentHvisEksisterer(hendelse.behandlingId)

        val beregningsGrunnlagDTO: BeregningsgrunnlagDTO = when (grunnlag) {
            is Grunnlag11_19 -> BeregningsgrunnlagDTO(
                grunnlag = grunnlag.grunnlaget().verdi().toDouble(),
                grunnlag11_19dto = grunnlag1119dto(grunnlag),
            )

            is GrunnlagUføre -> BeregningsgrunnlagDTO(
                grunnlag = grunnlag.grunnlaget().verdi().toDouble(),
                grunnlagUføre = GrunnlagUføreDTO(
                    type = grunnlag.type().toString(),
                    grunnlag = grunnlag1119dto(grunnlag.underliggende()),
                    grunnlagYtterligereNedsatt = grunnlag1119dto(grunnlag.underliggendeYtterligereNedsatt()),
                    uføreInntektIKroner = grunnlag.uføreInntekterFraForegåendeÅr()[0].inntektIKroner.verdi(), //TODO: Fjerne dette feltet og erstatt med uføreinntektene
                    uføreYtterligereNedsattArbeidsevneÅr = grunnlag.uføreYtterligereNedsattArbeidsevneÅr().value,
                    uføregrad = grunnlag.uføregrad().prosentverdi(),
                    uføreInntekterFraForegåendeÅr = grunnlag.uføreInntekterFraForegåendeÅr() //TODO: Vurdere hvilke felter som skal være med fra uføreinntektene
                        .associate { it.år.value.toString() to it.inntektIKroner.verdi() }
                )
            )

            is GrunnlagYrkesskade -> BeregningsgrunnlagDTO(
                grunnlag = grunnlag.grunnlaget().verdi().toDouble(),
                grunnlagYrkesskade = GrunnlagYrkesskadeDTO(
                    beregningsgrunnlag = grunnlag1119dto(grunnlag.underliggende() as Grunnlag11_19),
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
                    yrkesskadeinntektIG = grunnlag.yrkesskadeinntektIG().verdi()
                )
            )

            null -> throw RuntimeException("Fant ikke grunnlag for behandling med ID: ${behandling.id}.")
        }

        log.info("Kaller aap-statistikk for sak ${sak.saksnummer}.")

        statistikkGateway.avsluttetBehandling(
            AvsluttetBehandlingDTO(
                behandlingsReferanse = behandling.referanse,
                saksnummer = sak.saksnummer,
                vilkårsResultat = fraDomeneObjekt,
                tilkjentYtelse = tilkjentYtelseDTO,
                beregningsGrunnlag = beregningsGrunnlagDTO
            )
        )
    }

    private fun grunnlag1119dto(beregningsgrunnlag: Grunnlag11_19) =
        Grunnlag11_19DTO(
            inntekter = beregningsgrunnlag.inntekter().associate { it.år.value.toString() to it.inntektIKroner.verdi() }
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