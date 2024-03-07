package no.nav.aap.behandlingsflyt.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.avklaringsbehov.løsning.AvklarSykdomLøsning
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.verdityper.flyt.FlytKontekst

class AvklarSykdomLøser(connection: DBConnection) : AvklaringsbehovsLøser<AvklarSykdomLøsning> {

    private val behandlingRepository = BehandlingRepositoryImpl(connection)
    private val sykdomRepository = SykdomRepository(connection)

    override fun løs(kontekst: FlytKontekst, løsning: AvklarSykdomLøsning): LøsningsResultat {
        val behandling = behandlingRepository.hent(kontekst.behandlingId)

        sykdomRepository.lagre(
            behandlingId = behandling.id,
            sykdomsvurdering = løsning.sykdomsvurdering.toSykdomsvurdering(),
            yrkesskadevurdering = løsning.sykdomsvurdering.toYrkesskadevurdering()
        )

        return LøsningsResultat(
            begrunnelse = løsning.sykdomsvurdering.begrunnelse
        )
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_SYKDOM
    }
}
