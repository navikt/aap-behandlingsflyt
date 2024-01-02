package no.nav.aap.behandlingsflyt.avklaringsbehov.løser.bistand

import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.AvklaringsbehovsLøser
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.behandling.behandlingRepository
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.bistand.BistandRepository
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst

class AvklarBistandLøser(val connection: DBConnection) : AvklaringsbehovsLøser<AvklarBistandsbehovLøsning> {

    private val behandlingRepository = behandlingRepository(connection)
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
