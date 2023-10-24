package no.nav.aap.behandlingsflyt.avklaringsbehov.sykdom

import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovsLøser
import no.nav.aap.behandlingsflyt.avklaringsbehov.LøsningsResultat
import no.nav.aap.behandlingsflyt.domene.behandling.BehandlingTjeneste
import no.nav.aap.behandlingsflyt.domene.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.faktagrunnlag.sykdom.SykepengerErstatningTjeneste

class AvklarSykepengerErstatningLøser : AvklaringsbehovsLøser<AvklarSykepengerErstatningLøsning> {

    override fun løs(kontekst: FlytKontekst, løsning: AvklarSykepengerErstatningLøsning): LøsningsResultat {
        val behandling = BehandlingTjeneste.hent(kontekst.behandlingId)

        SykepengerErstatningTjeneste.lagre(
            behandlingId = behandling.id,
            vurdering = løsning.vurdering
        )

        return LøsningsResultat(
            begrunnelse = løsning.vurdering.begrunnelse
        )
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_SYKEPENGEERSTATNING
    }
}
