package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarBistandsbehovLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class AvklarBistandLøser(
    private val bistandRepository: BistandRepository,
    private val sykdomRepository: SykdomRepository,
) : AvklaringsbehovsLøser<AvklarBistandsbehovLøsning> {

    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        bistandRepository = repositoryProvider.provide(),
        sykdomRepository = repositoryProvider.provide(),
    )

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: AvklarBistandsbehovLøsning
    ): LøsningsResultat {
        val forrigeBehandlingId = kontekst.kontekst.forrigeBehandlingId

        val forrigeVedtatteGrunnlag = forrigeBehandlingId
            ?.let { bistandRepository.hentHvisEksisterer(it) }
        val vedtatteVurderinger = forrigeVedtatteGrunnlag?.vurderinger.orEmpty()

        val sykdomGrunnlag = sykdomRepository.hent(kontekst.behandlingId())
        
        val nyesteSykdomsvurdering = sykdomGrunnlag
            .sykdomsvurderinger.maxBy { it.opprettet }
        val bistandsVurdering = løsning.bistandsVurdering.tilBistandVurdering(
            kontekst.bruker,
            defaultVurderingenGjelderFra = løsning.bistandsVurdering.fom ?: nyesteSykdomsvurdering.vurderingenGjelderFra, // TODO: Gjør uavhengig fra sykdom
            kontekst.behandlingId()
        )

        val nyttGrunnlag = bistandsVurdering.let {
            BistandGrunnlag(
                vurderinger = vedtatteVurderinger + it,
            )
        }

        løsning.bistandsVurdering.valider()

        bistandRepository.lagre(
            behandlingId = kontekst.behandlingId(),
            bistandsvurderinger = nyttGrunnlag.vurderinger
        )
        return LøsningsResultat(
            begrunnelse = bistandsVurdering.begrunnelse
        )
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_BISTANDSBEHOV
    }
}
