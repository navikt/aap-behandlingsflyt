package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelseRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.hendelse.datadeling.ApiInternGateway
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.BehandlingFlytStoppetHendelse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryRegistry
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører

class DatadelingBehandlingJobbUtfører(
    private val apiInternGateway: ApiInternGateway,
    private val sakRepository: SakRepository,
    private val behandlingRepository: BehandlingRepository,
    private val tilkjentRepository: TilkjentYtelseRepository,
    private val underveisRepository: UnderveisRepository,
    private val vilkårsresultatRepository: VilkårsresultatRepository
) : JobbUtfører {
    override fun utfør(input: JobbInput) {
        val hendelse = input.payload<BehandlingFlytStoppetHendelse>()
        val behandling = behandlingRepository.hent(hendelse.referanse)
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

    companion object : Jobb {
        override fun beskrivelse(): String {
            return "Sender data rundt behandling til api-intern."
        }

        override fun konstruer(connection: DBConnection): JobbUtfører {
            val repositoryProvider = RepositoryRegistry.provider(connection)
            val behandlingRepository: BehandlingRepository = repositoryProvider.provide<BehandlingRepository>()
            val sakRepository: SakRepository = repositoryProvider.provide<SakRepository>()
            val tilkjentRepository: TilkjentYtelseRepository = repositoryProvider.provide<TilkjentYtelseRepository>()
            val underveisRepository: UnderveisRepository = repositoryProvider.provide<UnderveisRepository>()
            val vilkårsresultatRepository: VilkårsresultatRepository =
                repositoryProvider.provide<VilkårsresultatRepository>()

            return DatadelingBehandlingJobbUtfører(
                apiInternGateway = GatewayProvider.provide(),
                sakRepository = sakRepository,
                behandlingRepository = behandlingRepository,
                tilkjentRepository = tilkjentRepository,
                underveisRepository = underveisRepository,
                vilkårsresultatRepository = vilkårsresultatRepository
            )
        }

        override fun navn(): String {
            return "BehandlingDatadelingUtfører"
        }

        override fun type(): String {
            return "flyt.DatadelingBehandlingsdata"
        }
    }

}