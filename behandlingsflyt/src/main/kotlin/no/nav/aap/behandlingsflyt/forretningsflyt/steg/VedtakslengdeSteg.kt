package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.vedtakslengde.VedtakslengdeService
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import org.slf4j.LoggerFactory

class VedtakslengdeSteg(
    private val vedtakslengdeService: VedtakslengdeService,
    private val unleashGateway: UnleashGateway,
) : BehandlingSteg {
    private val log = LoggerFactory.getLogger(VedtakslengdeSteg::class.java)

    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        vedtakslengdeService = VedtakslengdeService(repositoryProvider, gatewayProvider),
        unleashGateway = gatewayProvider.provide()
    )

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        when (kontekst.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING, VurderingType.REVURDERING -> {
                if (unleashGateway.isDisabled(BehandlingsflytFeature.ForlengelseIManuellBehandling)) {
                    return Fullført
                }
                vedtakslengdeService.lagreGjeldendeSluttdatoHvisIkkeEksisterer(
                    behandlingId = kontekst.behandlingId,
                    forrigeBehandlingId = kontekst.forrigeBehandlingId,
                    rettighetsperiode = kontekst.rettighetsperiode
                )
            }

            VurderingType.MIGRER_RETTIGHETSPERIODE -> {
                vedtakslengdeService.lagreGjeldendeSluttdatoHvisIkkeEksisterer(
                    behandlingId = kontekst.behandlingId,
                    forrigeBehandlingId = kontekst.forrigeBehandlingId,
                    rettighetsperiode = kontekst.rettighetsperiode
                )
            }

            VurderingType.UTVID_VEDTAKSLENGDE -> {
                if (vedtakslengdeService.skalUtvideSluttdato(kontekst.behandlingId, kontekst.forrigeBehandlingId)) {
                    vedtakslengdeService.utvidSluttdato(
                        behandlingId = kontekst.behandlingId,
                        forrigeBehandlingId = kontekst.forrigeBehandlingId,
                    )
                } else {
                    log.info("Ingen utvidelse av vedtakslengde nødvendig")
                }
            }

            else -> {} // Noop
        }

        return Fullført
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return VedtakslengdeSteg(repositoryProvider, gatewayProvider)
        }

        override fun type(): StegType {
            return StegType.FASTSETT_VEDTAKSLENGDE
        }
    }
}

