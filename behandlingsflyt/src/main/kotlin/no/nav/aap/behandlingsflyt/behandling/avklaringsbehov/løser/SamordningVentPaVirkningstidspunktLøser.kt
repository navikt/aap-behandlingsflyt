package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.SamordningVentPaVirkningstidspunktLøsning
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.lookup.repository.RepositoryProvider

class SamordningVentPaVirkningstidspunktLøser() : AvklaringsbehovsLøser<SamordningVentPaVirkningstidspunktLøsning>  {
    constructor(repositoryProvider: RepositoryProvider): this()

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: SamordningVentPaVirkningstidspunktLøsning
    ): LøsningsResultat {
        return LøsningsResultat("Tatt av vent")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.SAMORDNING_VENT_PA_VIRKNINGSTIDSPUNKT
    }
}