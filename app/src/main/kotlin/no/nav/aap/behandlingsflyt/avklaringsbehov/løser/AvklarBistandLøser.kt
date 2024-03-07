package no.nav.aap.behandlingsflyt.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.avklaringsbehov.løsning.AvklarBistandsbehovLøsning
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.verdityper.flyt.FlytKontekst

class AvklarBistandLøser(val connection: DBConnection) : AvklaringsbehovsLøser<AvklarBistandsbehovLøsning> {

    private val behandlingRepository = BehandlingRepositoryImpl(connection)
    private val bistandRepository = BistandRepository(connection)

    override fun løs(kontekst: FlytKontekst, løsning: AvklarBistandsbehovLøsning): LøsningsResultat {
        val behandling = behandlingRepository.hent(kontekst.behandlingId)

        bistandRepository.lagre(
            behandlingId = behandling.id,
            bistandVurdering = løsning.bistandVurdering
        )

        return LøsningsResultat(
            begrunnelse = løsning.bistandVurdering.begrunnelse
        )
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_BISTANDSBEHOV
    }
}
