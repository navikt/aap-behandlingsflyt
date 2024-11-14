package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FastsettYrkesskadeInntektLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurderingRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.komponenter.dbconnect.DBConnection

class FastsettYrkesskadeInntektLøser(connection: DBConnection) :
    AvklaringsbehovsLøser<FastsettYrkesskadeInntektLøsning> {

    private val behandlingRepository = BehandlingRepositoryImpl(connection)
    private val beregningVurderingRepository = BeregningVurderingRepository(connection)

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