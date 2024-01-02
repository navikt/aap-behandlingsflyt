package no.nav.aap.behandlingsflyt.avklaringsbehov.løser.meldeplikt

import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.AvklaringsbehovsLøser
import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.LøsningsResultat
import no.nav.aap.behandlingsflyt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.behandling.behandlingRepository
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.meldeplikt.MeldepliktRepository
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst

class FritakFraMeldepliktLøser(val connection: DBConnection) : AvklaringsbehovsLøser<FritakMeldepliktLøsning> {

    private val behandlingRepository = behandlingRepository(connection)
    private val meldepliktRepository = MeldepliktRepository(connection)

    override fun løs(kontekst: FlytKontekst, løsning: FritakMeldepliktLøsning): LøsningsResultat {
        val behandling = behandlingRepository.hent(kontekst.behandlingId)

        val meldepliktGrunnlag = meldepliktRepository.hentHvisEksisterer(behandling.id)

        val eksisterendeFritaksvurderinger = meldepliktGrunnlag?.vurderinger.orEmpty()

        if (løsning.vurdering != null) {
            val vurderinger = mutableListOf(løsning.vurdering) + eksisterendeFritaksvurderinger

            meldepliktRepository.lagre(
                behandlingId = behandling.id,
                vurderinger = vurderinger
            )

            return LøsningsResultat(
                begrunnelse = løsning.vurdering.begrunnelse,
                kreverToTrinn = løsning.vurdering.harFritak
            )

        } else {
            return LøsningsResultat("N/A")
        }
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_BISTANDSBEHOV
    }
}
