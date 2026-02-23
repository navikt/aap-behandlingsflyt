package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.EtableringEgenVirksomhetLøsning
import no.nav.aap.behandlingsflyt.behandling.etableringegenvirksomhet.EtableringEgenVirksomhetService
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.etableringegenvirksomhet.EtableringEgenVirksomhetRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import kotlin.collections.orEmpty

class EtableringEgenVirksomhetLøser(
    private val etableringEgenVirksomhetRepository: EtableringEgenVirksomhetRepository,
    private val behandlingRepository: BehandlingRepository,
    private val unleashGateway: UnleashGateway,
    private val etableringEgenVirksomhetService: EtableringEgenVirksomhetService
) : AvklaringsbehovsLøser<EtableringEgenVirksomhetLøsning> {
    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        etableringEgenVirksomhetRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide(),
        unleashGateway = gatewayProvider.provide(),
        etableringEgenVirksomhetService = EtableringEgenVirksomhetService(repositoryProvider)
    )

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: EtableringEgenVirksomhetLøsning
    ): LøsningsResultat {
        if (unleashGateway.isDisabled(BehandlingsflytFeature.VirksomhetsEtablering)) {
            return LøsningsResultat(begrunnelse = "Vurdert etablering egen virksomhet")
        }

        val behandling = behandlingRepository.hent(kontekst.kontekst.behandlingId)
        val nyeVurderinger = løsning.løsningerForPerioder.map { it.toEtableringEgenVirksomhetVurdering(kontekst) }

        val evaluering = etableringEgenVirksomhetService.erVurderingerGyldig(behandling.id, nyeVurderinger)

        require(evaluering.erOppfylt) {
            evaluering.feilmelding!!
        }

        val gamleVurderinger =
            behandling.forrigeBehandlingId?.let { etableringEgenVirksomhetRepository.hentHvisEksisterer(it) }?.vurderinger.orEmpty()
        val alleVurderinger = gamleVurderinger + nyeVurderinger

        etableringEgenVirksomhetRepository.lagre(
            behandlingId = behandling.id,
            etableringEgenvirksomhetVurderinger = alleVurderinger
        )
        return LøsningsResultat(begrunnelse = "Vurdert etablering egen virksomhet")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.ETABLERING_EGEN_VIRKSOMHET
    }
}