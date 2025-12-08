package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.ArbeidsopptrappingLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsopptrapping.ArbeidsopptrappingRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class ArbeidsopptrappingLøser(
    private val arbeidsopptrappingRepositiory: ArbeidsopptrappingRepository,
    private val behandlingRepository: BehandlingRepository
) : AvklaringsbehovsLøser<ArbeidsopptrappingLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        arbeidsopptrappingRepositiory = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide()
    )

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: ArbeidsopptrappingLøsning
    ): LøsningsResultat {
        val behandling = behandlingRepository.hent(kontekst.kontekst.behandlingId)

        val nyeVurderinger = løsning.løsningerForPerioder.map { it.toArbeidsopptrappingVurdering(kontekst) }
        val gamleVurderinger =
            behandling.forrigeBehandlingId?.let { arbeidsopptrappingRepositiory.hentHvisEksisterer(it) }?.vurderinger.orEmpty()

        arbeidsopptrappingRepositiory.lagre(
            behandlingId = behandling.id,
            arbeidsopptrappingVurderinger = gamleVurderinger + nyeVurderinger
        )
        return LøsningsResultat(begrunnelse = "Vurdert arbeidsopptrapping")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.ARBEIDSOPPTRAPPING
    }
}