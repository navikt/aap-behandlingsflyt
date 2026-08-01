package no.nav.aap.behandlingsflyt.avklaringsbehov.løsning

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.AvklarVedtakslengdeLøser
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.steg.tilkjentytelse.VirkningstidspunktUtleder
import no.nav.aap.behandlingsflyt.faktagrunnlag.vilkårsresultat.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde.VedtakslengdeRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.vedtakslengde.VedtakslengdeVurderingDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AVKLAR_VEDTAKSLENGDE_KODE
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.AvklaringsbehovKode
import no.nav.aap.behandling.BehandlingId
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
        val vilkårsresultatRepository = repositoryProvider.provide<VilkårsresultatRepository>()

        val behandling = behandlingRepository.hent(behandlingId)
        val sak = sakRepository.hent(behandling.sakId)
        val vedtakslengdeStartdato =
            VirkningstidspunktUtleder(vilkårsresultatRepository).utledVirkningsTidspunkt(behandling.id)
                ?: sak.rettighetsperiode.fom

        return vedtakslengdeRepository.hentHvisEksisterer(behandlingId)?.gjeldendeVurderinger(vedtakslengdeStartdato) ?: Tidslinje<Unit>()
    }
}

