package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSamordningBarnepensjonLøsning
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.lookup.repository.RepositoryProvider

class AvklarSamordningBarnepensjonLøser(
) : AvklaringsbehovsLøser<AvklarSamordningBarnepensjonLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        
    )

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: AvklarSamordningBarnepensjonLøsning
    ): LøsningsResultat {
        return LøsningsResultat("Vurdert samordning barnepensjon")

    }

    override fun forBehov(): Definisjon {
        return Definisjon.SAMORDNING_BARNEPENSJON
    }
}