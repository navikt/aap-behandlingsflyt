package no.nav.aap.behandlingsflyt.avklaringsbehov.meldeplikt

import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovsLøser
import no.nav.aap.behandlingsflyt.avklaringsbehov.LøsningsResultat
import no.nav.aap.behandlingsflyt.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.meldeplikt.MeldepliktRepository
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst

class FritakFraMeldepliktLøser(val connection: DBConnection) : AvklaringsbehovsLøser<FritakMeldepliktLøsning> {

    private val behandlingRepository = BehandlingRepository(connection)
    private val meldepliktRepository = MeldepliktRepository(connection)

    override fun løs(kontekst: FlytKontekst, løsning: FritakMeldepliktLøsning): LøsningsResultat {
        val behandling = behandlingRepository.hent(kontekst.behandlingId)

        val meldepliktGrunnlag = meldepliktRepository.hentHvisEksisterer(behandling.id)

        val eksisterendeFritaksvurderinger = meldepliktGrunnlag?.vurderinger.orEmpty()

        val vurderinger = mutableListOf(løsning.vurdering) + eksisterendeFritaksvurderinger

        meldepliktRepository.lagre(
            behandlingId = behandling.id,
            vurderinger = vurderinger
        )

        return LøsningsResultat(begrunnelse = løsning.vurdering.begrunnelse, kreverToTrinn = løsning.vurdering.harFritak)
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_BISTANDSBEHOV
    }
}
