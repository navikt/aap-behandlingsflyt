package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.samid.SamIdRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.hendelse.datadeling.ApiInternGateway
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.BehandlingFlytStoppetHendelse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProviderJobbSpesifikasjon
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class DatadelingBehandlingJobbUtfører(
    private val apiInternGateway: ApiInternGateway,
    private val sakRepository: SakRepository,
    private val behandlingRepository: BehandlingRepository,
    private val tilkjentRepository: TilkjentYtelseRepository,
    private val underveisRepository: UnderveisRepository,
    private val vilkårsresultatRepository: VilkårsresultatRepository,
    private val vedtakRepository: VedtakRepository,
    private val samIdRepository: SamIdRepository
) : JobbUtfører {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(input: JobbInput) {
        val (behandlingId, vedtaksTidspunkt) = input.payload< Pair<BehandlingId, LocalDateTime>>()
        val behandling = behandlingRepository.hent(behandlingId)
        
        if (behandling.typeBehandling() !in listOf(
                TypeBehandling.Førstegangsbehandling,
                TypeBehandling.Revurdering
            )
        ) {
            // TODO: Avgjør om vi skal sende klagebehandlinger til api-intern
            log.info("Fant behandling av type ${behandling.typeBehandling()} - oversender ikke til api-intern")
            return
        }
        
        val sak = sakRepository.hent(behandling.sakId)
        val tilkjentYtelse = tilkjentRepository.hentHvisEksisterer(behandling.id)
        val underveis = underveisRepository.hentHvisEksisterer(behandling.id)
        val vilkårsresultatTidslinje = vilkårsresultatRepository.hent(behandling.id).rettighetstypeTidslinje()
        val vedtakId = vedtakRepository.hentId(behandling.id)
        val samId = samIdRepository.hentHvisEksisterer(behandling.id)

        apiInternGateway.sendBehandling(
            sak,
            behandling,
            vedtakId,
            samId,
            tilkjentYtelse,
            underveis?.perioder.orEmpty(),
            vedtaksTidspunkt.toLocalDate(),
            vilkårsresultatTidslinje
        )
    }

    companion object : ProviderJobbSpesifikasjon {
        override val beskrivelse = "Sender data rundt behandling til api-intern."
        override val navn = "BehandlingDatadelingUtfører"
        override val type = "flyt.DatadelingBehandlingsdata"

        override fun konstruer(repositoryProvider: RepositoryProvider): JobbUtfører {
            return DatadelingBehandlingJobbUtfører(
                apiInternGateway = GatewayProvider.provide(),
                sakRepository = repositoryProvider.provide(),
                behandlingRepository = repositoryProvider.provide(),
                tilkjentRepository = repositoryProvider.provide(),
                underveisRepository = repositoryProvider.provide(),
                vilkårsresultatRepository = repositoryProvider.provide(),
                vedtakRepository = repositoryProvider.provide(),
                samIdRepository = repositoryProvider.provide(),
            )
        }
    }
}