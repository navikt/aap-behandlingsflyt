package no.nav.aap.behandlingsflyt.prosessering.statistikk

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.ApplikasjonsVersjon
import no.nav.aap.behandlingsflyt.hendelse.avløp.sortererteAvklaringsbehov
import no.nav.aap.behandlingsflyt.hendelse.statistikk.StatistikkGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon
import java.time.LocalDateTime

class ResendStatistikkJobbUtfører(
    private val behandlingRepository: BehandlingRepository,
    private val sakService: SakService,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val statistikkGateway: StatistikkGateway,
    private val statistikkMetoder: StatistikkMetoder,
) :
    JobbUtfører {
    override fun utfør(input: JobbInput) {
        val behandlingId = input.payload<Long>().let(::BehandlingId)
        val behandling = behandlingRepository.hent(behandlingId)
        val sak = sakService.hent(behandling.sakId)

        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(behandlingId)

        val hendelseTilStatistikk = BehandlingFlytStoppetHendelseTilStatistikk(
            personIdent = sak.person.aktivIdent().identifikator,
            saksnummer = sak.saksnummer,
            referanse = behandling.referanse,
            behandlingType = behandling.typeBehandling(),
            status = behandling.status(),
            avklaringsbehov = sortererteAvklaringsbehov(behandling, avklaringsbehovene.alle()),
            opprettetTidspunkt = behandling.opprettetTidspunkt,
            hendelsesTidspunkt = LocalDateTime.now(),
            versjon = ApplikasjonsVersjon.versjon
        )

        val kontraktHendelse = statistikkMetoder.oversettHendelseTilKontrakt(hendelseTilStatistikk)

        statistikkGateway.resendBehandling(kontraktHendelse)
    }


    companion object : ProvidersJobbSpesifikasjon {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): ResendStatistikkJobbUtfører {
            return ResendStatistikkJobbUtfører(
                behandlingRepository = repositoryProvider.provide(),
                sakService = SakService(repositoryProvider),
                avklaringsbehovRepository = repositoryProvider.provide(),
                statistikkGateway = gatewayProvider.provide(),
                statistikkMetoder = StatistikkMetoder(repositoryProvider)
            )
        }

        override val type = "flyt.statistikk.resend"
        override val navn = "Resend statistikk"
        override val beskrivelse = "For manuelt å resende informasjon til statistikk-appen."
    }
}