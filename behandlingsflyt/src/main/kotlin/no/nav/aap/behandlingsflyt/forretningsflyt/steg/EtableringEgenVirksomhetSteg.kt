package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.Fullført
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.kontrakt.steg.StegType
import no.nav.aap.behandlingsflyt.sakogbehandling.flyt.FlytKontekstMedPerioder
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class EtableringEgenVirksomhetSteg private constructor() : BehandlingSteg {

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        // Verifiser state her ift revurderinger, om ikke lengre gyldig.

        // If finnes en vurdering
            // Alle perioder er gyldig (tidspunkter hvor både 11-5 og 11-6 b er oppfylt)
            // -- Utviklingsperiode inntil 6 mnd (131 dager?
            // -- Oppstartsperiode inntil 3 mnd (hvor mange dager?)
            // -- Oppstartsperioder kan aldri ligge før en utviklingsperiode.
            // -- Man må ha definert minst en periode i tidsplanen dersom vilkåret er oppfylt for en periode

        // Masse kult her

        return Fullført
    }

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return EtableringEgenVirksomhetSteg()
        }

        override fun type(): StegType {
            return StegType.ETABLERING_EGEN_VIRKSOMHET
        }
    }
}