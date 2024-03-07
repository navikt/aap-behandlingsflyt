package no.nav.aap.behandlingsflyt.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.avklaringsbehov.løsning.FastsettBeregningstidspunktLøsning
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurderingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.verdityper.flyt.FlytKontekst

class FastsettBeregningstidspunktLøser(connection: DBConnection) : AvklaringsbehovsLøser<FastsettBeregningstidspunktLøsning> {

    private val behandlingRepository = BehandlingRepositoryImpl(connection)
    private val beregningVurderingRepository = BeregningVurderingRepository(connection)

    override fun løs(kontekst: FlytKontekst, løsning: FastsettBeregningstidspunktLøsning): LøsningsResultat {
        val behandling = behandlingRepository.hent(kontekst.behandlingId)

        beregningVurderingRepository.lagre(
            behandlingId = behandling.id,
            vurdering = løsning.vurdering
        )

        return LøsningsResultat(
            begrunnelse = løsning.vurdering.begrunnelse
        )
    }

    override fun forBehov(): Definisjon {
        return Definisjon.FASTSETT_BEREGNINGSTIDSPUNKT
    }
}
