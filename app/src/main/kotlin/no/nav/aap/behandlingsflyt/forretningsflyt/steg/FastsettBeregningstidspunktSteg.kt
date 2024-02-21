package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.beregning.BeregningVurderingRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.verdityper.flyt.FlytKontekst
import no.nav.aap.verdityper.flyt.StegType

class FastsettBeregningstidspunktSteg private constructor(private val beregningVurderingRepository: BeregningVurderingRepository) :
    BehandlingSteg {

    override fun utfør(kontekst: FlytKontekst): StegResultat {
        // TODO: Sjekke om det faktisk er behov for innhenting av opplysninger
        // DVS sjekk mot vilkår forutfor dette om alle er oppfylt fra inngang
        val beregningVurdering = beregningVurderingRepository.hentHvisEksisterer(kontekst.behandlingId)
        if (beregningVurdering == null) {
            return StegResultat(listOf(Definisjon.FASTSETT_BEREGNINGSTIDSPUNKT))
        }
        return StegResultat()
    }

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return FastsettBeregningstidspunktSteg(BeregningVurderingRepository(connection))
        }

        override fun type(): StegType {
            return StegType.FASTSETT_BEREGNINGSTIDSPUNKT
        }
    }
}
