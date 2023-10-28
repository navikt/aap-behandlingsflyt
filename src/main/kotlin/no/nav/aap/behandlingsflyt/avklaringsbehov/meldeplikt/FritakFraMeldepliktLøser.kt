package no.nav.aap.behandlingsflyt.avklaringsbehov.meldeplikt

import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovsLøser
import no.nav.aap.behandlingsflyt.avklaringsbehov.LøsningsResultat
import no.nav.aap.behandlingsflyt.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.dbstuff.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.meldeplikt.MeldepliktRepository
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst

class FritakFraMeldepliktLøser(val connection: DBConnection) : AvklaringsbehovsLøser<FritakMeldepliktLøsning> {

    override fun løs(kontekst: FlytKontekst, løsning: FritakMeldepliktLøsning): LøsningsResultat {
        val behandling = BehandlingRepository(connection).hent(kontekst.behandlingId)

        val meldepliktGrunnlag = MeldepliktRepository.hentHvisEksisterer(behandling.id)

        val eksisterendeFritaksvurderinger = meldepliktGrunnlag?.vurderinger.orEmpty()

        val vurderinger = mutableListOf(løsning.vurdering) + eksisterendeFritaksvurderinger

        MeldepliktRepository.lagre(
            behandlingId = behandling.id,
            vurderinger = vurderinger
        )

        return LøsningsResultat(begrunnelse = løsning.vurdering.begrunnelse, kreverToTrinn = løsning.vurdering.harFritak)
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_BISTANDSBEHOV
    }
}
