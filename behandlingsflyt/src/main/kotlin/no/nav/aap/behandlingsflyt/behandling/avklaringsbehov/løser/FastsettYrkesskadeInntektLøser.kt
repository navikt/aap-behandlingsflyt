package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FastsettYrkesskadeInntektLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurderingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.YrkesskadeBeløpVurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.lookup.repository.RepositoryProvider

class FastsettYrkesskadeInntektLøser(
    private val behandlingRepository: BehandlingRepository,
    private val beregningVurderingRepository: BeregningVurderingRepository,
    ) : AvklaringsbehovsLøser<FastsettYrkesskadeInntektLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        behandlingRepository = repositoryProvider.provide(),
        beregningVurderingRepository = repositoryProvider.provide(),
    )

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: FastsettYrkesskadeInntektLøsning): LøsningsResultat {
        val behandling = behandlingRepository.hent(kontekst.kontekst.behandlingId)

        beregningVurderingRepository.lagre(
            behandlingId = behandling.id,
            vurdering =
                løsning.yrkesskadeInntektVurdering.vurderinger.map {
                    YrkesskadeBeløpVurdering(
                        antattÅrligInntekt = it.antattÅrligInntekt,
                        referanse = it.referanse,
                        begrunnelse = it.begrunnelse,
                        vurdertAv = kontekst.bruker.ident
                    )
                }
        )

        return LøsningsResultat(
            begrunnelse = løsning.yrkesskadeInntektVurdering.vurderinger.joinToString { it.begrunnelse }
        )
    }

    override fun forBehov(): Definisjon {
        return Definisjon.FASTSETT_YRKESSKADEINNTEKT
    }
}