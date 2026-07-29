package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.VurderAvslag11_27Løsning
import no.nav.aap.behandlingsflyt.behandling.avslag11_27.Avslag11_27Repository
import no.nav.aap.behandlingsflyt.behandling.avslag11_27.Avslag11_27Vurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.avslag11_27.flate.Avslag11_27VurderingDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.krav.Kravreferanse
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.Instant
import java.util.*

class VurderAvslag11_27Løser(
    private val behandlingRepository: BehandlingRepository,
    private val avslag11_27repository: Avslag11_27Repository,
    private val unleashGateway: UnleashGateway,
) : AvklaringsbehovsLøser<VurderAvslag11_27Løsning> {

    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        behandlingRepository = repositoryProvider.provide(),
        avslag11_27repository = repositoryProvider.provide(),
        unleashGateway = gatewayProvider.provide(),
    )

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: VurderAvslag11_27Løsning
    ): LøsningsResultat {
        check(unleashGateway.isEnabled(BehandlingsflytFeature.Avslag11_27)) {
            "Avslag11_27-toggle er avskrudd, men løsningen ble kalt. Behandling: ${kontekst.behandlingId()}"
        }

        val vurdertAv = kontekst.bruker.ident
        val forrigeBehandlingId = behandlingRepository.hent(kontekst.kontekst.behandlingId).forrigeBehandlingId
        val vurderinger = tilAvslag11_27Vurderinger(
            løsning.avslag11_27Vurdering.vurderinger,
            vurdertAv,
            kontekst.behandlingId()
        )

        val gjeldendeVedtatte =
            forrigeBehandlingId?.let { avslag11_27repository.hentHvisEksisterer(it) }?.vurderinger.orEmpty()

        avslag11_27repository.lagre(
            kontekst.behandlingId(),
            vurderinger + gjeldendeVedtatte
        )

        return LøsningsResultat(løsning.avslag11_27Vurdering.vurderinger.joinToString(" ") { it.begrunnelse })
    }

    override fun forBehov(): Definisjon {
        return Definisjon.VURDER_AVSLAG_11_27
    }

    private fun tilAvslag11_27Vurderinger(
        nyeVurderinger: List<Avslag11_27VurderingDto>,
        vurdertAv: String,
        behandlingId: BehandlingId
    ): Set<Avslag11_27Vurdering> = nyeVurderinger.map { vurdering ->
        Avslag11_27Vurdering(
            referanse = Kravreferanse(UUID.fromString(vurdering.referanse)),
            skalAvslås1127 = vurdering.skalAvslås1127,
            brukersYtelse = vurdering.brukersYtelse,
            harSykepengegrunnlagOver2G = vurdering.harSykepengegrunnlagOver2G,
            harAnnenFullYtelse = vurdering.harAnnenFullYtelse,
            begrunnelse = vurdering.begrunnelse,
            vurdertIBehandling = behandlingId,
            vurdertAv = Bruker(vurdertAv),
            opprettet = Instant.now(),
        )
    }.toSet()
}