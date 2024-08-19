package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarBistandsbehovLøsning
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl

class AvklarBistandLøser(val connection: DBConnection) : AvklaringsbehovsLøser<AvklarBistandsbehovLøsning> {

    private val behandlingRepository = BehandlingRepositoryImpl(connection)
    private val bistandRepository = BistandRepository(connection)

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: AvklarBistandsbehovLøsning): LøsningsResultat {
        val behandling = behandlingRepository.hent(kontekst.kontekst.behandlingId)

        val bistandsVurdering = løsning.bistandsVurderingDto.tilBistandVurdering()
        bistandRepository.lagre(
            behandlingId = behandling.id,
            bistandVurdering = bistandsVurdering
        )

        return LøsningsResultat(
            begrunnelse = bistandsVurdering.begrunnelse
        )
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_BISTANDSBEHOV
    }
}
