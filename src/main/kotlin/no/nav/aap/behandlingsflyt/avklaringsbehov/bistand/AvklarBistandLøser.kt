package no.nav.aap.behandlingsflyt.avklaringsbehov.bistand

import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovsLøser
import no.nav.aap.behandlingsflyt.avklaringsbehov.LøsningsResultat
import no.nav.aap.behandlingsflyt.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.bistand.BistandRepository
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst

class AvklarBistandLøser(val connection: DBConnection) : AvklaringsbehovsLøser<AvklarBistandsbehovLøsning> {

    private val behandlingRepository = BehandlingRepository(connection)
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
