package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FastsettBeregningstidspunktLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurderingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.komponenter.dbconnect.DBConnection

class FastsettBeregningstidspunktLøser(connection: DBConnection) : AvklaringsbehovsLøser<FastsettBeregningstidspunktLøsning> {

    private val behandlingRepository = BehandlingRepositoryImpl(connection)
    private val beregningVurderingRepository = BeregningVurderingRepository(connection)

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: FastsettBeregningstidspunktLøsning): LøsningsResultat {
        val behandling = behandlingRepository.hent(kontekst.kontekst.behandlingId)

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
