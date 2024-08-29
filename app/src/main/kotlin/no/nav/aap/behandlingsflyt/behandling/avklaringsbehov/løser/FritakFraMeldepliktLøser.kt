package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FritakMeldepliktLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.komponenter.dbconnect.DBConnection

class FritakFraMeldepliktLøser(val connection: DBConnection) : AvklaringsbehovsLøser<FritakMeldepliktLøsning> {

    private val behandlingRepository = BehandlingRepositoryImpl(connection)
    private val meldepliktRepository = MeldepliktRepository(connection)

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: FritakMeldepliktLøsning): LøsningsResultat {
        val behandling = behandlingRepository.hent(kontekst.kontekst.behandlingId)

        val meldepliktGrunnlag = meldepliktRepository.hentHvisEksisterer(behandling.id)

        val eksisterendeFritaksvurderinger = meldepliktGrunnlag?.vurderinger.orEmpty()

        val vurderinger = mutableListOf(løsning.vurdering) + eksisterendeFritaksvurderinger

        meldepliktRepository.lagre(
            behandlingId = behandling.id,
            vurderinger = vurderinger
        )

        return LøsningsResultat(
            begrunnelse = løsning.vurdering.begrunnelse,
            kreverToTrinn = løsning.vurdering.harFritak
        )
    }

    override fun forBehov(): Definisjon {
        return Definisjon.FRITAK_MELDEPLIKT
    }
}
