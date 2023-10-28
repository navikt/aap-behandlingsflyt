package no.nav.aap.behandlingsflyt.avklaringsbehov.sykdom

import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovsLøser
import no.nav.aap.behandlingsflyt.avklaringsbehov.LøsningsResultat
import no.nav.aap.behandlingsflyt.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.dbstuff.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.sykdom.SykdomsRepository
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst

class AvklarSykdomLøser(val connection: DBConnection) : AvklaringsbehovsLøser<AvklarSykdomLøsning> {

    override fun løs(kontekst: FlytKontekst, løsning: AvklarSykdomLøsning): LøsningsResultat {
        val behandling = BehandlingRepository(connection).hent(kontekst.behandlingId)
        val sykdomsGrunnlag = SykdomsRepository.hentHvisEksisterer(kontekst.behandlingId)

        SykdomsRepository.lagre(
            behandlingId = behandling.id,
            yrkesskadevurdering = sykdomsGrunnlag?.yrkesskadevurdering,
            sykdomsvurdering = løsning.sykdomsvurdering
        )

        return LøsningsResultat(
            begrunnelse = løsning.sykdomsvurdering.begrunnelse
        )
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_SYKDOM
    }
}
