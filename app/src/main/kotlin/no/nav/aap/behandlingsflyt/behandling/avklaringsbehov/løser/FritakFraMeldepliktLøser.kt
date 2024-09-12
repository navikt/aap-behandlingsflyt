package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FritakMeldepliktLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.FritaksPeriodeDto
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.MeldepliktRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.meldeplikt.flate.FritaksPeriode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.komponenter.dbconnect.DBConnection

class FritakFraMeldepliktLøser(val connection: DBConnection) : AvklaringsbehovsLøser<FritakMeldepliktLøsning> {

    private val behandlingRepository = BehandlingRepositoryImpl(connection)
    private val meldepliktRepository = MeldepliktRepository(connection)

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: FritakMeldepliktLøsning): LøsningsResultat {
        if (løsning.fritaksPerioder().fritaksPeriodeOverlapper()) throw IllegalStateException("Valideringsfeil: Perioder overlapper")
        
        val behandling = behandlingRepository.hent(kontekst.kontekst.behandlingId)

        meldepliktRepository.lagre(
            behandlingId = behandling.id,
            vurdering = løsning.fritaksvurdering
        )

        return LøsningsResultat(
            begrunnelse = "Vurdert fritak fra meldeplikt",
            kreverToTrinn = løsning.fritaksPerioder().kreverToTrinn()
        )
    }

    override fun forBehov(): Definisjon {
        return Definisjon.FRITAK_MELDEPLIKT
    }

    private fun List<FritaksPeriode>.fritaksPeriodeOverlapper() = this
        .sortedBy { it.periode.fom }
        .zipWithNext()
        .any { (tidlig, sent) ->  tidlig overlapperMed sent }
    
    private infix fun FritaksPeriode.overlapperMed(other: FritaksPeriode) = periode.overlapper(other.periode)
    
    private fun List<FritaksPeriode>.kreverToTrinn() = this.any { it.harFritak }
}
