package no.nav.aap.behandlingsflyt.avklaringsbehov.sykdom

import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovsLøser
import no.nav.aap.behandlingsflyt.avklaringsbehov.LøsningsResultat
import no.nav.aap.behandlingsflyt.domene.behandling.BehandlingTjeneste
import no.nav.aap.behandlingsflyt.domene.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.grunnlag.sykdom.SykdomsTjeneste

class AvklarYrkesskadeLøser : AvklaringsbehovsLøser<AvklarYrkesskadeLøsning> {

    override fun løs(kontekst: FlytKontekst, løsning: AvklarYrkesskadeLøsning): LøsningsResultat {
        val behandling = BehandlingTjeneste.hent(kontekst.behandlingId)
        val sykdomsGrunnlag = SykdomsTjeneste.hentHvisEksisterer(behandlingId = kontekst.behandlingId)

        SykdomsTjeneste.lagre(
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
