package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FastsettYrkesskadeInntektLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurderingRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryRegistry

class FastsettYrkesskadeInntektLøser(connection: DBConnection) :
    AvklaringsbehovsLøser<FastsettYrkesskadeInntektLøsning> {

    private val repositoryProvider = RepositoryRegistry.provider(connection)
    private val behandlingRepository = repositoryProvider.provide<BehandlingRepository>()
    private val beregningVurderingRepository = repositoryProvider.provide<BeregningVurderingRepository>()

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: FastsettYrkesskadeInntektLøsning): LøsningsResultat {
        val behandling = behandlingRepository.hent(kontekst.kontekst.behandlingId)

        beregningVurderingRepository.lagre(
            behandlingId = behandling.id,
            vurdering = løsning.yrkesskadeInntektVurdering.vurderinger
        )

        return LøsningsResultat(
            begrunnelse = løsning.yrkesskadeInntektVurdering.vurderinger.map { it.begrunnelse }.joinToString()
        )
    }

    override fun forBehov(): Definisjon {
        return Definisjon.FASTSETT_YRKESSKADEINNTEKT
    }
}