package no.nav.aap.behandlingsflyt.avklaringsbehov.sykdom

import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovsLøser
import no.nav.aap.behandlingsflyt.avklaringsbehov.LøsningsResultat
import no.nav.aap.behandlingsflyt.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.dbstuff.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.sykdom.SykdomsRepository
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst

class AvklarYrkesskadeLøser(val connection: DBConnection) : AvklaringsbehovsLøser<AvklarYrkesskadeLøsning> {

    override fun løs(kontekst: FlytKontekst, løsning: AvklarYrkesskadeLøsning): LøsningsResultat {
        val behandling = BehandlingRepository(connection).hent(kontekst.behandlingId)
        val sykdomsGrunnlag = SykdomsRepository.hentHvisEksisterer(behandlingId = kontekst.behandlingId)

        SykdomsRepository.lagre(
            behandlingId = behandling.id,
            yrkesskadevurdering = løsning.yrkesskadevurdering,
            sykdomsvurdering = sykdomsGrunnlag?.sykdomsvurdering
        )

        return LøsningsResultat(
            begrunnelse = løsning.yrkesskadevurdering.begrunnelse
        )
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_SYKDOM
    }
}
