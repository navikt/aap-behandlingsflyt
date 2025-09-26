package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.VurderingType
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

/**
 * Tidligere brukt for å opprette revurdering dersom samordning hadde usikker sluttdato.
 * Dette løses nå med å opprette oppfølgingsoppgave istedenfor.
 */
class OpprettRevurderingSteg(
) : BehandlingSteg {
    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        return when (kontekst.vurderingType) {
            VurderingType.FØRSTEGANGSBEHANDLING, VurderingType.REVURDERING, VurderingType.MELDEKORT, VurderingType.EFFEKTUER_AKTIVITETSPLIKT, VurderingType.IKKE_RELEVANT -> {
                Fullført
            }
        }
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {

            return OpprettRevurderingSteg()
        }

        override fun type(): StegType {
            return StegType.OPPRETT_REVURDERING
        }
    }
}