package no.nav.aap.avklaringsbehov.sykdom

import no.nav.aap.avklaringsbehov.AvklaringsbehovsLøser
import no.nav.aap.avklaringsbehov.LøsningsResultat
import no.nav.aap.domene.behandling.BehandlingTjeneste
import no.nav.aap.domene.behandling.avklaringsbehov.Definisjon
import no.nav.aap.domene.behandling.grunnlag.sykdom.SykdomsTjeneste
import no.nav.aap.flyt.kontroll.FlytKontekst

class AvklarSykdomLøser : AvklaringsbehovsLøser<AvklarSykdomLøsning> {

    override fun løs(kontekst: FlytKontekst, løsning: AvklarSykdomLøsning): LøsningsResultat {
        val behandling = BehandlingTjeneste.hent(kontekst.behandlingId)

        SykdomsTjeneste.lagre(behandlingId = behandling.id, yrkesskadevurdering = løsning.yrkesskadevurdering, sykdomsvurdering = løsning.sykdomsvurdering)

        return LøsningsResultat(begrunnelse = lagSamletBegrunnelse(løsning))
    }

    private fun lagSamletBegrunnelse(løsning: AvklarSykdomLøsning): String {
        return "${løsning.yrkesskadevurdering?.begrunnelse} \n ${løsning.sykdomsvurdering.begrunnelse}"
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_SYKDOM
    }
}
