package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSykdomLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.komponenter.dbconnect.DBConnection

class AvklarSykdomLøser(connection: DBConnection) : AvklaringsbehovsLøser<AvklarSykdomLøsning> {

    private val behandlingRepository = BehandlingRepositoryImpl(connection)
    private val sykdomRepository = SykdomRepository(connection)

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: AvklarSykdomLøsning): LøsningsResultat {
        val behandling = behandlingRepository.hent(kontekst.kontekst.behandlingId)

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
