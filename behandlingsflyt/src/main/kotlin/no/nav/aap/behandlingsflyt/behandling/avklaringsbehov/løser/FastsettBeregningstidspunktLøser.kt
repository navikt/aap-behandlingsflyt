package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FastsettBeregningstidspunktLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurderingRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningstidspunktVurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDate

class FastsettBeregningstidspunktLøser(
    private val behandlingRepository: BehandlingRepository,
    private val beregningVurderingRepository: BeregningVurderingRepository,
) : AvklaringsbehovsLøser<FastsettBeregningstidspunktLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        behandlingRepository = repositoryProvider.provide(),
        beregningVurderingRepository = repositoryProvider.provide(),
    )

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: FastsettBeregningstidspunktLøsning): LøsningsResultat {
        val behandling = behandlingRepository.hent(kontekst.kontekst.behandlingId)

        if (løsning.beregningVurdering.nedsattArbeidsevneDato.isAfter(LocalDate.now())) {
            throw IllegalArgumentException("Kan ikke sette beregningstidspunkt frem i tid.")
        }

        beregningVurderingRepository.lagre(
            behandlingId = behandling.id,
            vurdering = løsning.beregningVurdering.let {
                BeregningstidspunktVurdering(
                    begrunnelse = it.begrunnelse,
                    nedsattArbeidsevneDato = it.nedsattArbeidsevneDato,
                    ytterligereNedsattBegrunnelse = it.ytterligereNedsattBegrunnelse,
                    ytterligereNedsattArbeidsevneDato = it.ytterligereNedsattArbeidsevneDato,
                    vurdertAv = kontekst.bruker.ident
                )
            }
        )

        return LøsningsResultat(
            begrunnelse = løsning.beregningVurdering.begrunnelse
        )
    }

    override fun forBehov(): Definisjon {
        return Definisjon.FASTSETT_BEREGNINGSTIDSPUNKT
    }
}
