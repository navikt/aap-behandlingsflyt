package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.datadeling.sam.SamGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.samid.HentSamId
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.samid.SamIdRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryProvider
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProviderJobbSpesifikasjon

class HentSamIdJobbUtfører(
    private val samGateway: SamGateway,
    private val samIdRepository: SamIdRepository,
    private val behandlingRepository: BehandlingRepository,
    private val sakRepository: SakRepository,

    ): JobbUtfører {
    override fun utfør(input: JobbInput) {
        val behandlingId = input.payload<HentSamId>().behandlingId
        val behandling = behandlingRepository.hent(behandlingId)
        val sak = sakRepository.hent(behandling.sakId)
        val samId = samGateway.hentSamId(sak.person.aktivIdent(),sak.id.id.toString(),behandling.referanse.toString())

        samIdRepository.lagre(behandlingId, samId.samId.toString()) //TODO: finn ut av riktig typeklasse
    }



companion object : ProviderJobbSpesifikasjon {
    override fun konstruer(repositoryProvider: RepositoryProvider): JobbUtfører {
        return HentSamIdJobbUtfører(
            GatewayProvider.provide(),
            repositoryProvider.provide(),
            repositoryProvider.provide(),
            repositoryProvider.provide()
        )
    }

    override val type: String = "flyt.HentSamId"
    override val navn: String = "HentSamId"
    override val beskrivelse: String = "Henter SamId fra SAM og lagrer ned for datadeling"

}
}