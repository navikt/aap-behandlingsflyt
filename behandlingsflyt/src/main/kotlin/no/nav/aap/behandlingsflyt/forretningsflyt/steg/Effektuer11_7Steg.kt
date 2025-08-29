package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

@Deprecated("Tidligere brukt til ventebehov for effektuering av 11-7 i vanlig flyt, aldri vært brukt i produksjon. Kan ikke slettes pga historikk i revurderinger i produksjon")
class Effektuer11_7Steg() : BehandlingSteg {


    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        return Fullført
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return Effektuer11_7Steg()
        }

        override fun type(): StegType {
            return StegType.EFFEKTUER_11_7
        }
    }
}