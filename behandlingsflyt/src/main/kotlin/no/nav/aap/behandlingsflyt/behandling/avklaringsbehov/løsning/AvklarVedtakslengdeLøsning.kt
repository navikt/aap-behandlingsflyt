package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.AvklarVedtakslengdeLøser
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde.VedtakslengdeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde.VedtakslengdeVurderingDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AVKLAR_VEDTAKSLENGDE_KODE
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.lookup.repository.RepositoryProvider

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName(value = AVKLAR_VEDTAKSLENGDE_KODE)
class AvklarVedtakslengdeLøsning(
    @param:JsonProperty("løsningerForPerioder", required = true)
    override val løsningerForPerioder: List<VedtakslengdeVurderingDto>,
    @param:JsonProperty("behovstype", required = true, defaultValue = AVKLAR_VEDTAKSLENGDE_KODE)
    val behovstype: AvklaringsbehovKode = AvklaringsbehovKode.`5059`,
) : PeriodisertAvklaringsbehovLøsning<VedtakslengdeVurderingDto> {
    override fun løs(
        repositoryProvider: RepositoryProvider,
        kontekst: AvklaringsbehovKontekst,
        gatewayProvider: GatewayProvider
    ): LøsningsResultat {
        return AvklarVedtakslengdeLøser(repositoryProvider).løs(kontekst, this)
    }

    override fun hentLagredeLøstePerioder(
        behandlingId: BehandlingId,
        repositoryProvider: RepositoryProvider
    ): Tidslinje<*> {
        val vedtakslengdeRepository = repositoryProvider.provide<VedtakslengdeRepository>()
        val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
        val sakRepository = repositoryProvider.provide<SakRepository>()

        val behandling = behandlingRepository.hent(behandlingId)
        val sak = sakRepository.hent(behandling.sakId)
        return vedtakslengdeRepository.hentHvisEksisterer(behandlingId)?.gjeldendeVurderinger(sak.rettighetsperiode.fom) ?: Tidslinje<Unit>()
    }
}

