package no.nav.aap.behandlingsflyt.avklaringsbehov.vedtak

import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovsLøser
import no.nav.aap.behandlingsflyt.avklaringsbehov.LøsningsResultat
import no.nav.aap.behandlingsflyt.domene.behandling.BehandlingTjeneste
import no.nav.aap.behandlingsflyt.domene.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.flyt.kontroll.FlytKontekst

class FatteVedtakLøser : AvklaringsbehovsLøser<FatteVedtakLøsning> {

    override fun løs(kontekst: FlytKontekst, løsning: FatteVedtakLøsning): LøsningsResultat {
        val behandling = BehandlingTjeneste.hent(kontekst.behandlingId)
        val avklaringsbehovene = behandling.avklaringsbehovene()

        løsning.vurderinger.forEach { vurdering ->
            avklaringsbehovene.vurderTotrinn(
                Definisjon.forKode(vurdering.definisjon),
                vurdering.godkjent,
                vurdering.begrunnelse
            )
        }

        return LøsningsResultat(sammenstillBegrunnelse(løsning))
    }

    private fun sammenstillBegrunnelse(løsning: FatteVedtakLøsning): String {
        return løsning.vurderinger.joinToString("\\n") { it.begrunnelse }
    }

    override fun forBehov(): Definisjon {
        return Definisjon.FATTE_VEDTAK
    }
}
