package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.hendelse.datadeling.ApiInternGateway
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.BehandlingFlytStoppetHendelse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProviderJobbSpesifikasjon
import org.slf4j.LoggerFactory

class DatadelingBehandlingJobbUtfører(
    private val apiInternGateway: ApiInternGateway,
    private val sakRepository: SakRepository,
    private val behandlingRepository: BehandlingRepository,
    private val tilkjentRepository: TilkjentYtelseRepository,
    private val underveisRepository: UnderveisRepository,
    private val vilkårsresultatRepository: VilkårsresultatRepository
) : JobbUtfører {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(input: JobbInput) {
        val hendelse = input.payload<BehandlingFlytStoppetHendelse>()
        val behandling = behandlingRepository.hent(hendelse.referanse)
        
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

        apiInternGateway.sendBehandling(
            sak,
            behandling,
            tilkjentYtelse,
            underveis?.perioder.orEmpty(),
            hendelse.hendelsesTidspunkt.toLocalDate(),
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
                vilkårsresultatRepository = repositoryProvider.provide()
            )
        }
    }
}