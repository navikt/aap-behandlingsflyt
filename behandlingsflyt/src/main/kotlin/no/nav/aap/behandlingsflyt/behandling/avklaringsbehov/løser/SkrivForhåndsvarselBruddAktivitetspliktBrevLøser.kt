package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.SkrivBrevAvklaringsbehovLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.SkrivForhåndsvarselBruddAktivitetspliktBrevLøsning
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class SkrivForhåndsvarselBruddAktivitetspliktBrevLøser(private val skrivBrevAvklaringsbehovLøser: SkrivBrevAvklaringsbehovLøser) :
    AvklaringsbehovsLøser<SkrivForhåndsvarselBruddAktivitetspliktBrevLøsning> {

    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        skrivBrevAvklaringsbehovLøser = SkrivBrevAvklaringsbehovLøser(repositoryProvider, gatewayProvider)
    )

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: SkrivForhåndsvarselBruddAktivitetspliktBrevLøsning
    ): LøsningsResultat {
        return skrivBrevAvklaringsbehovLøser.løs(
            kontekst,
            SkrivBrevAvklaringsbehovLøsning(løsning.brevbestillingReferanse, løsning.handling, løsning.mottakere)
        )
    }

    override fun forBehov(): Definisjon {
        return Definisjon.SKRIV_FORHÅNDSVARSEL_BRUDD_AKTIVITETSPLIKT_BREV
    }
}
