package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FastsettBeregningstidspunktLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurderingRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.repository.RepositoryProvider

class FastsettBeregningstidspunktLøser(connection: DBConnection) :
    AvklaringsbehovsLøser<FastsettBeregningstidspunktLøsning> {

    private val repositoryProvider = RepositoryProvider(connection)
    private val behandlingRepository = repositoryProvider.provide(BehandlingRepository::class)
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
