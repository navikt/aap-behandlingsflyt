package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.ArbeidsopptrappingLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsopptrapping.ArbeidsopptrappingPerioder
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsopptrapping.ArbeidsopptrappingRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.unleash.BehandlingsflytFeature
import no.nav.aap.behandlingsflyt.unleash.UnleashGateway
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import kotlin.collections.orEmpty

class ArbeidsopptrappingLøser(
    private val arbeidsopptrappingRepositiory: ArbeidsopptrappingRepository,
    private val unleashGateway: UnleashGateway
) : AvklaringsbehovsLøser<ArbeidsopptrappingLøsning> {

    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        arbeidsopptrappingRepositiory = repositoryProvider.provide(),
        unleashGateway = gatewayProvider.provide()
    )

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: ArbeidsopptrappingLøsning
    ): LøsningsResultat {
        if (unleashGateway.isEnabled(BehandlingsflytFeature.Arbeidsopptrapping)) {
            val arbeidsopptrappingVurderinger =
                løsning.arbeidsopptrappingVurderinger.map { it.toArbeidsopptrappingVurdering(kontekst.bruker.ident) }

            val eksisterendeArbeidsevnePerioder = ArbeidsopptrappingPerioder(
                arbeidsopptrappingRepositiory.hentHvisEksisterer(kontekst.behandlingId())?.vurderinger.orEmpty()
            )

            val nyeArbeidsopptrappingPerioder =
                eksisterendeArbeidsevnePerioder.leggTil(ArbeidsopptrappingPerioder(arbeidsopptrappingVurderinger))

            arbeidsopptrappingRepositiory.lagre(
                behandlingId = kontekst.behandlingId(),
                arbeidsopptrappingVurderinger = nyeArbeidsopptrappingPerioder.gjeldendeArbeidsopptrappingsVurderinger()
            )
        }
        return LøsningsResultat(begrunnelse = "Vurdert arbeidsopptrapping")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.ARBEIDSOPPTRAPPING
    }
}
