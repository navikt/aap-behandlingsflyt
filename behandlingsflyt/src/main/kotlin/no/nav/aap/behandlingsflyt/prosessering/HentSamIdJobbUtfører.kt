package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakRepository
import no.nav.aap.behandlingsflyt.datadeling.sam.SamGateway
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.samid.SamIdRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.refusjonskrav.TjenestepensjonRefusjonsKravVurderingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryProvider
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProviderJobbSpesifikasjon

class HentSamIdJobbUtfører(
    private val repositoryProvider: RepositoryProvider,
    private val gatewayProvider: GatewayProvider,
    private val samGateway: SamGateway = gatewayProvider.provide(),
    private val samIdRepository: SamIdRepository = repositoryProvider.provide(),
    private val behandlingRepository: BehandlingRepository = repositoryProvider.provide(),
    private val sakRepository: SakRepository = repositoryProvider.provide(),
    private val vedtakRepository: VedtakRepository = repositoryProvider.provide(),
    private val tjenestepensjonRefusjonsKravVurderingRepository: TjenestepensjonRefusjonsKravVurderingRepository = repositoryProvider.provide(),

    ): JobbUtfører {
    override fun utfør(input: JobbInput) {
        val behandlingId = input.payload<BehandlingId>()
        val behandling = behandlingRepository.hent(behandlingId)
        val sak = sakRepository.hent(behandling.sakId)
        val vedtakId = requireNotNull(vedtakRepository.hentId(behandling.id))
        val tpRefusjonskravVurdering = tjenestepensjonRefusjonsKravVurderingRepository.hentHvisEksisterer(behandlingId)

        if(tpRefusjonskravVurdering != null && tpRefusjonskravVurdering.harKrav) {
            val samId = samGateway.hentSamId(sak.person.aktivIdent(), sak.id.id, vedtakId)

            samIdRepository.lagre(behandlingId, samId.toString())
        }
    }



companion object : ProviderJobbSpesifikasjon {
    override fun konstruer(repositoryProvider: RepositoryProvider): JobbUtfører {
        return HentSamIdJobbUtfører(
            repositoryProvider,
            GatewayProvider
        )
    }

    override val type: String = "flyt.HentSamId"
    override val navn: String = "HentSamId"
    override val beskrivelse: String = "Henter SamId fra SAM og lagrer ned for datadeling"

    }
}