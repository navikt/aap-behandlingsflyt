package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.behandlingsflyt.behandling.vedtak.VedtakRepository
import no.nav.aap.behandlingsflyt.datadeling.sam.SamGateway
import no.nav.aap.behandlingsflyt.datadeling.sam.SamordneVedtakRequest
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.refusjonskrav.TjenestepensjonRefusjonsKravVurderingRepository
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.BehandlingFlytStoppetHendelse
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
    private val sakRepository: SakRepository,
    private val behandlingRepository: BehandlingRepository,
    private val vedtakRepository: VedtakRepository,
    private val tjenestepensjonRefusjonsKravVurderingRepository: TjenestepensjonRefusjonsKravVurderingRepository,
    private val flytJobbRepository: FlytJobbRepository,
    private val samGateway: SamGateway
) : JobbUtfører {
    override fun utfør(input: JobbInput) {
        val behandlingId = input.payload<BehandlingId>() //endre payload
        val behandling = behandlingRepository.hent(behandlingId)
        val sak = sakRepository.hent(behandling.sakId)
        val tjenestepensjonRefusjonsKravVurdering = tjenestepensjonRefusjonsKravVurderingRepository.hentHvisEksisterer(behandling.id)
        val vedtak = vedtakRepository.hent(behandling.id)

        val request = SamordneVedtakRequest(
            pid = sak.person.aktivIdent().identifikator.toString(),
            vedtakId = behandling.referanse.toString(),
            sakId = sak.id.id,
            virkFom = vedtak!!.vedtakstidspunkt.toLocalDate(),
            virkTom = sak.rettighetsperiode.tom,
            fagomrade = "AAP",
            ytelseType = "AAP",
            etterbetaling = sak.rettighetsperiode.fom < vedtak.vedtakstidspunkt.toLocalDate(),
            utvidetFrist = null, //SPK kan få utvidet frist dersom etterbetaling er over 2G
        )

        samGateway.varsleVedtak(request)
    }

    companion object : ProviderJobbSpesifikasjon {
        override fun konstruer(repositoryProvider: RepositoryProvider): JobbUtfører {
            return VarsleVedtakJobbUtfører(
                repositoryProvider.provide(),
                repositoryProvider.provide(),
                repositoryProvider.provide(),
                repositoryProvider.provide(),
                repositoryProvider.provide(),
                GatewayProvider.provide(),
            )
        }

        override val beskrivelse = "Varsler om endring nytt eller endring i vedtak til SAM"
        override val navn = "VarsleVedtakSam"
        override val type = "flyt.Varsler"
    }
}