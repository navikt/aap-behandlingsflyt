package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.VurderInntektsbortfallLøsning
import no.nav.aap.behandlingsflyt.behandling.inntektsbortfall.InntektsbortfallRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.InntektsbortfallVurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDateTime

class VurderInntektsbortfallLøser(
    private val inntektsbortfallRepository: InntektsbortfallRepository,
    private val behandlingRepository: BehandlingRepository
) : AvklaringsbehovsLøser<VurderInntektsbortfallLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        inntektsbortfallRepository = repositoryProvider.provide(),
        behandlingRepository = repositoryProvider.provide()
    )

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: VurderInntektsbortfallLøsning
    ): LøsningsResultat {
        val behandling = behandlingRepository.hent(kontekst.kontekst.behandlingId)
        inntektsbortfallRepository.lagre(
            behandlingId = behandling.id,
            vurdering = InntektsbortfallVurdering(
                begrunnelse = løsning.vurdering.begrunnelse,
                rettTilUttak = løsning.vurdering.rettTilUttak,
                vurdertAv = kontekst.bruker.ident,
                vurdertIBehandling = kontekst.behandlingId(),
                opprettetTid = LocalDateTime.now(),
            )
        )
        return LøsningsResultat(begrunnelse = "Vurdert inntektsbortfall")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.VURDER_INNTEKTSBORTFALL
    }
}