package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSykepengerErstatningLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerErstatningRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.SykepengerVurderingDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
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
        val nyVurdering = tilVurdering(løsning.sykepengeerstatningVurdering, behandling.id, kontekst.bruker.ident)

        sykepengerErstatningRepository.lagre(
            behandlingId = behandling.id,
            nyeVurderinger = listOf(nyVurdering)
        )

        return LøsningsResultat(
            begrunnelse = løsning.sykepengeerstatningVurdering.begrunnelse
        )
    }

    private fun tilVurdering(
        dto: SykepengerVurderingDto,
        behandlingId: BehandlingId,
        vurdertAv: String
    ): SykepengerVurdering = SykepengerVurdering(
        begrunnelse = dto.begrunnelse,
        dokumenterBruktIVurdering = dto.dokumenterBruktIVurdering,
        harRettPå = dto.harRettPå,
        grunn = dto.grunn,
        vurdertIBehandling = behandlingId,
        vurdertAv = vurdertAv,
        gjelderFra = dto.gjelderFra
    )

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_SYKEPENGEERSTATNING
    }
}
