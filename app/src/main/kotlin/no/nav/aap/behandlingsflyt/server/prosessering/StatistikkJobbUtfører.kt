package no.nav.aap.behandlingsflyt.server.prosessering

import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.hendelse.avløp.BehandlingFlytStoppetHendelse
import no.nav.aap.behandlingsflyt.hendelse.avløp.VilkårsResultatHendelseDTO
import no.nav.aap.behandlingsflyt.hendelse.statistikk.StatistikkGateway
import no.nav.aap.behandlingsflyt.hendelse.statistikk.StatistikkHendelseDTO
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
    private val sakService: SakService
) : JobbUtfører {
    override fun utfør(input: JobbInput) {
        log.info("Utfører jobbinput statistikk: $input")
        val payload = input.payload()

        val type = StatistikkType.valueOf(input.parameter("statistikk-type"))

        when (type) {
            StatistikkType.BehandlingStoppet -> håndterBehandlingStoppet(payload)
            StatistikkType.AvsluttetBehandling -> håndterVilkårsResultat(payload)
        }

    }

    private fun håndterBehandlingStoppet(payload: String) {
        val hendelse = DefaultJsonMapper.fromJson<BehandlingFlytStoppetHendelse>(payload)

        statistikkGateway.avgiStatistikk(
            StatistikkHendelseDTO(
                saksnummer = hendelse.saksnummer.toString(),
                behandlingType = hendelse.behandlingType,
                status = hendelse.status
            )
        )
    }

    private fun håndterVilkårsResultat(payload: String) {
        val hendelse = DefaultJsonMapper.fromJson<VilkårsResultatHendelseDTO>(payload)

        val behandling = behandlingRepository.hent(hendelse.behandlingId)
        val vilkårsresultat = vilkårsresultatRepository.hent(hendelse.behandlingId)
        val sak = sakService.hent(behandling.sakId)

        val fraDomeneObjekt = VilkårsResultatDTO.fraDomeneObjekt(
            saksnummer = sak.saksnummer,
            typeBehandling = behandling.typeBehandling(),
            vilkårsresultat = vilkårsresultat
        )
        statistikkGateway.vilkårsResultat(fraDomeneObjekt)
    }


    companion object : Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            val vilkårsresultatRepository = VilkårsresultatRepository(connection)
            val behandlingRepository = BehandlingRepositoryImpl(connection)
            val sakService = SakService(connection)

            return StatistikkJobbUtfører(
                StatistikkGateway(),
                vilkårsresultatRepository,
                behandlingRepository,
                sakService
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