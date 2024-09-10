package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FritakMeldepliktLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.Fritaksvurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.komponenter.dbconnect.DBConnection

class FritakFraMeldepliktLøser(val connection: DBConnection) : AvklaringsbehovsLøser<FritakMeldepliktLøsning> {

    private val behandlingRepository = BehandlingRepositoryImpl(connection)
    private val meldepliktRepository = MeldepliktRepository(connection)

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: FritakMeldepliktLøsning): LøsningsResultat {
        if (løsning.fritaksvurderinger.fritaksvurderingPerioderOverlapper()) throw IllegalStateException("Valideringsfeil: Perioder overlapper")
        
        val behandling = behandlingRepository.hent(kontekst.kontekst.behandlingId)

        meldepliktRepository.lagre(
            behandlingId = behandling.id,
            vurderinger = løsning.fritaksvurderinger
        )

        return LøsningsResultat(
            begrunnelse = "Vurdert fritak fra meldeplikt",
            kreverToTrinn = løsning.fritaksvurderinger.kreverToTrinn()
        )
    }

    override fun forBehov(): Definisjon {
        return Definisjon.FRITAK_MELDEPLIKT
    }

    private fun List<Fritaksvurdering>.fritaksvurderingPerioderOverlapper() = this
        .sortedBy { it.periode.fom }
        .zipWithNext()
        .any { (tidlig, sent) ->  tidlig overlapperMed sent }
    
    private infix fun Fritaksvurdering.overlapperMed(other: Fritaksvurdering) = periode.overlapper(other.periode)
    
    private fun List<Fritaksvurdering>.kreverToTrinn() = this.any { it.harFritak }
}
