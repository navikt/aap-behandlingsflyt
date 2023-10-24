package no.nav.aap.behandlingsflyt.avklaringsbehov.meldeplikt

import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovsLøser
import no.nav.aap.behandlingsflyt.avklaringsbehov.LøsningsResultat
import no.nav.aap.behandlingsflyt.domene.behandling.BehandlingTjeneste
import no.nav.aap.behandlingsflyt.domene.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.faktagrunnlag.meldeplikt.MeldepliktTjeneste

class FritakFraMeldepliktLøser : AvklaringsbehovsLøser<FritakMeldepliktLøsning> {

    override fun løs(kontekst: FlytKontekst, løsning: FritakMeldepliktLøsning): LøsningsResultat {
        val behandling = BehandlingTjeneste.hent(kontekst.behandlingId)

        val meldepliktGrunnlag = MeldepliktTjeneste.hentHvisEksisterer(behandling.id)

        val eksisterendeFritaksvurderinger = meldepliktGrunnlag?.vurderinger.orEmpty()

        val vurderinger = mutableListOf(løsning.vurdering) + eksisterendeFritaksvurderinger

        MeldepliktTjeneste.lagre(
            behandlingId = behandling.id,
            vurderinger = vurderinger
        )

        return LøsningsResultat(begrunnelse = løsning.vurdering.begrunnelse, kreverToTrinn = løsning.vurdering.harFritak)
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_BISTANDSBEHOV
    }
}
