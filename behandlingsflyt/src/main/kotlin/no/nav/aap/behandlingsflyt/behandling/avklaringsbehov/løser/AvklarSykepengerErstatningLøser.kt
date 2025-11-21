package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.PeriodisertAvklarSykepengerErstatningLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerErstatningRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykepengerVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.flate.PeriodisertSykepengerVurderingDto
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class AvklarSykepengerErstatningLøser(
    private val behandlingRepository: BehandlingRepository,
    private val sykepengerErstatningRepository: SykepengerErstatningRepository
) : AvklaringsbehovsLøser<PeriodisertAvklarSykepengerErstatningLøsning> {

    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        behandlingRepository = repositoryProvider.provide(),
        sykepengerErstatningRepository = repositoryProvider.provide()
    )

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: PeriodisertAvklarSykepengerErstatningLøsning
    ): LøsningsResultat {
        val behandling = behandlingRepository.hent(kontekst.kontekst.behandlingId)
        val tidligereVurderinger = behandling.forrigeBehandlingId?.let { sykepengerErstatningRepository.hentHvisEksisterer(it)?.vurderinger }.orEmpty()

        val nyeVurderinger = løsning.løsningerForPerioder.map { tilVurdering(it, behandling.id, kontekst.bruker.ident) }

        sykepengerErstatningRepository.lagre(
            behandlingId = behandling.id,
            vurderinger = tidligereVurderinger + nyeVurderinger
        )

        return LøsningsResultat(
            begrunnelse = nyeVurderinger.joinToString("\n") { it.begrunnelse }
        )
    }

    private fun tilVurdering(
        dto: PeriodisertSykepengerVurderingDto,
        behandlingId: BehandlingId,
        vurdertAv: String
    ): SykepengerVurdering = SykepengerVurdering(
        begrunnelse = dto.begrunnelse,
        dokumenterBruktIVurdering = dto.dokumenterBruktIVurdering,
        harRettPå = dto.harRettPå,
        grunn = dto.grunn,
        vurdertIBehandling = behandlingId,
        vurdertAv = vurdertAv,
        gjelderFra = dto.fom
    )

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_SYKEPENGEERSTATNING
    }
}
