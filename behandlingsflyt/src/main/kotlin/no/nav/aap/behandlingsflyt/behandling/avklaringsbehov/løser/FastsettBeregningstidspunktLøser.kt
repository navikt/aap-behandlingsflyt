package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FastsettBeregningstidspunktLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurderingRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDate

class FastsettBeregningstidspunktLøser(
    private val behandlingRepository: BehandlingRepository,
    private val beregningVurderingRepository: BeregningVurderingRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository
) : AvklaringsbehovsLøser<FastsettBeregningstidspunktLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        behandlingRepository = repositoryProvider.provide(),
        beregningVurderingRepository = repositoryProvider.provide(),
        avklaringsbehovRepository = repositoryProvider.provide()
    )

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: FastsettBeregningstidspunktLøsning): LøsningsResultat {
        val behandling = behandlingRepository.hent(kontekst.kontekst.behandlingId)

        if (løsning.beregningVurdering.nedsattArbeidsevneDato.isAfter(LocalDate.now())) {
            throw IllegalArgumentException("Kan ikke sette beregningstidspunkt frem i tid.")
        }
        val avklaringsbehovene = avklaringsbehovRepository.hentAvklaringsbehovene(kontekst.kontekst.behandlingId)
        val avklaringsbehov = avklaringsbehovene.hentBehovForDefinisjon(Definisjon.FASTSETT_MANUELL_INNTEKT)
        if (avklaringsbehov?.erÅpent() == true) {
            avklaringsbehovene.avbryt(Definisjon.FASTSETT_MANUELL_INNTEKT)
        }

        beregningVurderingRepository.lagre(
            behandlingId = behandling.id,
            vurdering = løsning.beregningVurdering
        )

        return LøsningsResultat(
            begrunnelse = løsning.beregningVurdering.begrunnelse
        )
    }

    override fun forBehov(): Definisjon {
        return Definisjon.FASTSETT_BEREGNINGSTIDSPUNKT
    }
}
