package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakRepository
import no.nav.aap.behandlingsflyt.datadeling.sam.SamGateway
import no.nav.aap.behandlingsflyt.datadeling.sam.SamordneVedtakRequest
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.refusjonskrav.TjenestepensjonRefusjonsKravVurderingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProviderJobbSpesifikasjon

class VarsleVedtakJobbUtfører(
    private val repositoryProvider: RepositoryProvider,
    private val gatewayProvider: GatewayProvider,
    private val sakRepository: SakRepository = repositoryProvider.provide(),
    private val behandlingRepository: BehandlingRepository = repositoryProvider.provide(),
    private val vedtakRepository: VedtakRepository = repositoryProvider.provide(),
    private val flytJobbRepository: FlytJobbRepository = repositoryProvider.provide(),
    private val tjenestepensjonRefusjonskravVurdering: TjenestepensjonRefusjonsKravVurderingRepository = repositoryProvider.provide(),
    private val samGateway: SamGateway = gatewayProvider.provide(),
) : JobbUtfører {
    override fun utfør(input: JobbInput) {
        val behandlingId = input.payload<BehandlingId>()
        val behandling = behandlingRepository.hent(behandlingId)
        val sak = sakRepository.hent(behandling.sakId)
        val vedtak = vedtakRepository.hent(behandling.id)
        val vedtakId = requireNotNull(vedtakRepository.hentId(behandling.id))

        val request = SamordneVedtakRequest(
            pid = sak.person.aktivIdent().identifikator,
            vedtakId = vedtakId.toString(),
            sakId = sak.id.id,
            virkFom = vedtak!!.vedtakstidspunkt.toLocalDate(),
            virkTom = sak.rettighetsperiode.tom,
            fagomrade = "AAP",
            ytelseType = "AAP",
            etterbetaling = vedtak.virkningstidspunkt?.let { vedtak.vedtakstidspunkt.toLocalDate() > it } ?: false,
            utvidetFrist = null,
        )

        samGateway.varsleVedtak(request)

        val tpRefusjonskravVurdering = tjenestepensjonRefusjonskravVurdering.hentHvisEksisterer(behandlingId)

        if(tpRefusjonskravVurdering != null && tpRefusjonskravVurdering.harKrav) {
            flytJobbRepository.leggTil(JobbInput(HentSamIdJobbUtfører).medPayload(behandling.id))
        }
    }

    companion object : ProviderJobbSpesifikasjon {
        override fun konstruer(repositoryProvider: RepositoryProvider): JobbUtfører {
            return VarsleVedtakJobbUtfører(
                repositoryProvider,
                GatewayProvider,
            )
        }

        override val beskrivelse = "Varsler om endring nytt eller endring i vedtak til SAM"
        override val navn = "VarsleVedtakSam"
        override val type = "flyt.Varsler"
    }
}