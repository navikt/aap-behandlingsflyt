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
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon
import org.slf4j.LoggerFactory

class HentSamIdJobbUtfører(
    private val repositoryProvider: RepositoryProvider,
    private val gatewayProvider: GatewayProvider,
    private val samGateway: SamGateway = gatewayProvider.provide(),
    private val samIdRepository: SamIdRepository = repositoryProvider.provide(),
    private val behandlingRepository: BehandlingRepository = repositoryProvider.provide(),
    private val sakRepository: SakRepository = repositoryProvider.provide(),
    private val vedtakRepository: VedtakRepository = repositoryProvider.provide(),
    private val flytJobbRepository: FlytJobbRepository = repositoryProvider.provide(),
    private val tjenestepensjonRefusjonsKravVurderingRepository: TjenestepensjonRefusjonsKravVurderingRepository = repositoryProvider.provide(),

    ): JobbUtfører {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(input: JobbInput) {
        val behandlingId = input.payload<BehandlingId>()
        val behandling = behandlingRepository.hent(behandlingId)
        val sak = sakRepository.hent(behandling.sakId)
        val vedtakId = requireNotNull(vedtakRepository.hentId(behandling.id))
        val vedtaksTidspunkt = requireNotNull(vedtakRepository.hent(behandling.id)).vedtakstidspunkt
        val tpRefusjonskravVurdering = tjenestepensjonRefusjonsKravVurderingRepository.hentHvisEksisterer(behandlingId)

        if (tpRefusjonskravVurdering != null && tpRefusjonskravVurdering.harKrav) {
            val samId = samGateway.hentSamId(sak.person.aktivIdent(), sak.id.id, vedtakId)

            if (samId != null) {
                // Får null tilbake hvis det ikke er en _aktiv_ TP-ytelse
                // Se Slack-tråd her https://nav-it.slack.com/archives/CQ08JC3UG/p1764056287208599
                samIdRepository.lagre(behandlingId, samId.toString())
            } else {
                log.warn("Fant ingen SAM-ID for behandling $behandlingId / vedtak $vedtakId.")
            }
        }

        flytJobbRepository.leggTil(
            jobbInput = JobbInput(DatadelingBehandlingJobbUtfører).medPayload(
                Pair(
                    behandlingId, vedtaksTidspunkt
                )
            ).forSak(sak.id.id)
        )
    }

    companion object : ProvidersJobbSpesifikasjon {
        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): JobbUtfører {
            return HentSamIdJobbUtfører(
                repositoryProvider,
                gatewayProvider
            )
        }

        override val type: String = "flyt.HentSamId"
        override val navn: String = "HentSamId"
        override val beskrivelse: String = "Henter SamId fra SAM og lagrer ned for datadeling"

    }
}