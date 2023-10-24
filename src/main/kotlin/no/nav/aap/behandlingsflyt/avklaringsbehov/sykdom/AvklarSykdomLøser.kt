package no.nav.aap.behandlingsflyt.avklaringsbehov.sykdom

import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovsLøser
import no.nav.aap.behandlingsflyt.avklaringsbehov.LøsningsResultat
import no.nav.aap.behandlingsflyt.domene.behandling.BehandlingTjeneste
import no.nav.aap.behandlingsflyt.domene.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.faktagrunnlag.sykdom.SykdomsTjeneste

class AvklarSykdomLøser : AvklaringsbehovsLøser<AvklarSykdomLøsning> {

    override fun løs(kontekst: FlytKontekst, løsning: AvklarSykdomLøsning): LøsningsResultat {
        val behandling = BehandlingTjeneste.hent(kontekst.behandlingId)
        val sykdomsGrunnlag = SykdomsTjeneste.hentHvisEksisterer(kontekst.behandlingId)

        SykdomsTjeneste.lagre(
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
