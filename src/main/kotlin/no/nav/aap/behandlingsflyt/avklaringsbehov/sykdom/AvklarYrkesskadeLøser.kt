package no.nav.aap.behandlingsflyt.avklaringsbehov.sykdom

import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovsLøser
import no.nav.aap.behandlingsflyt.avklaringsbehov.LøsningsResultat
import no.nav.aap.behandlingsflyt.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.sykdom.SykdomsRepository
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst

class AvklarYrkesskadeLøser(val connection: DBConnection) : AvklaringsbehovsLøser<AvklarYrkesskadeLøsning> {

    private val behandlingRepository = BehandlingRepository(connection)
    private val sykdomsRepository = SykdomsRepository(connection)

    override fun løs(kontekst: FlytKontekst, løsning: AvklarYrkesskadeLøsning): LøsningsResultat {
        val behandling = behandlingRepository.hent(kontekst.behandlingId)

        sykdomsRepository.lagre(
            behandlingId = behandling.id,
            yrkesskadevurdering = løsning.yrkesskadevurdering,
        )

        return LøsningsResultat(
            begrunnelse = løsning.yrkesskadevurdering.begrunnelse
        )
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_SYKDOM
    }
}
