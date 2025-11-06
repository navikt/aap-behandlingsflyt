package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSykepengerErstatningLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerErstatningRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.SykepengerVurderingDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class AvklarSykepengerErstatningLøser(
    private val behandlingRepository: BehandlingRepository,
    private val sykepengerErstatningRepository: SykepengerErstatningRepository
) : AvklaringsbehovsLøser<AvklarSykepengerErstatningLøsning> {

    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        behandlingRepository = repositoryProvider.provide(),
        sykepengerErstatningRepository = repositoryProvider.provide()
    )

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: AvklarSykepengerErstatningLøsning
    ): LøsningsResultat {
        val behandling = behandlingRepository.hent(kontekst.kontekst.behandlingId)

        val nyVurdering = tilVurdering(løsning.sykepengeerstatningVurdering, kontekst.bruker.ident)

        val eksisterendeVurderinger =
            behandling.forrigeBehandlingId
                ?.let { sykepengerErstatningRepository.hentHvisEksisterer(it) }
                ?.vurderinger.orEmpty()

        val nyeVurderinger = eksisterendeVurderinger + nyVurdering

        sykepengerErstatningRepository.lagre(
            behandlingId = behandling.id,
            vurderinger = nyeVurderinger
        )

        return LøsningsResultat(
            begrunnelse = løsning.sykepengeerstatningVurdering.begrunnelse
        )
    }

    private fun tilVurdering(
        dto: SykepengerVurderingDto,
        vurdertAv: String
    ): SykepengerVurdering = SykepengerVurdering(
        begrunnelse = dto.begrunnelse,
        dokumenterBruktIVurdering = dto.dokumenterBruktIVurdering,
        harRettPå = dto.harRettPå,
        grunn = dto.grunn,
        vurdertAv = vurdertAv,
        gjelderFra = dto.gjelderFra
    )

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_SYKEPENGEERSTATNING
    }
}
