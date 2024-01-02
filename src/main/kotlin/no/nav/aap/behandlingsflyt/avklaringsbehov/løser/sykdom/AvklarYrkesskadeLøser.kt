package no.nav.aap.behandlingsflyt.avklaringsbehov.løser.sykdom

import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.AvklaringsbehovsLøser
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.behandling.behandlingRepository
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst

class AvklarYrkesskadeLøser(val connection: DBConnection) : AvklaringsbehovsLøser<AvklarYrkesskadeLøsning> {

    private val behandlingRepository = behandlingRepository(connection)
    private val sykdomRepository = SykdomRepository(connection)

    override fun løs(kontekst: FlytKontekst, løsning: AvklarYrkesskadeLøsning): LøsningsResultat {
        val behandling = behandlingRepository.hent(kontekst.behandlingId)

        sykdomRepository.lagre(
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
