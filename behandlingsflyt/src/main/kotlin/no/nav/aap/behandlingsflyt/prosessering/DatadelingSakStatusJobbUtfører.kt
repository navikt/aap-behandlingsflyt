package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.datadeling.SakStatus
import no.nav.aap.behandlingsflyt.hendelse.datadeling.ApiInternGateway
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.BehandlingFlytStoppetHendelse
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProviderJobbSpesifikasjon

class DatadelingSakStatusJobbUtfører(
    private val apiInternGateway: ApiInternGateway,
    private val sakRepository: SakRepository,
    private val behandlingRepository: BehandlingRepository,
) : JobbUtfører {
    override fun utfør(input: JobbInput) {
        val hendelse = input.payload<BehandlingFlytStoppetHendelse>()
        val behandling = behandlingRepository.hent(hendelse.referanse)
        val sak = sakRepository.hent(behandling.sakId)

        apiInternGateway.sendSakStatus(
            sak.person.aktivIdent().identifikator,
            SakStatus.fromKelvin(sak.saksnummer.toString(), sak.status(), sak.rettighetsperiode)
        )
    }

    companion object : ProviderJobbSpesifikasjon {
        override val beskrivelse = "Sender status på sak til api-intern."
        override val navn = "DatadelingSakStatusJobbUtfører"
        override val type = "flyt.DatadelingSakStatus"

        override fun konstruer(repositoryProvider: RepositoryProvider): JobbUtfører {
            return DatadelingSakStatusJobbUtfører(
                apiInternGateway = GatewayProvider.provide(),
                sakRepository = repositoryProvider.provide(),
                behandlingRepository = repositoryProvider.provide()
            )
        }
    }
}