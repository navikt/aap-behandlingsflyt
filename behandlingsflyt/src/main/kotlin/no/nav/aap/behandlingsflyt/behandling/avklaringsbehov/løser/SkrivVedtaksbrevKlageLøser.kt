package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.SkrivBrevAvklaringsbehovLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.SkrivVedtaksbrevKlageLøsning
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider

class SkrivVedtaksbrevKlageLøser(
    private val skrivBrevAvklaringsbehovLøser: SkrivBrevAvklaringsbehovLøser,
) : AvklaringsbehovsLøser<SkrivVedtaksbrevKlageLøsning> {

    constructor(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) : this(
        skrivBrevAvklaringsbehovLøser = SkrivBrevAvklaringsbehovLøser(repositoryProvider, gatewayProvider)
    )

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: SkrivVedtaksbrevKlageLøsning
    ): LøsningsResultat {
        return skrivBrevAvklaringsbehovLøser.løs(
            kontekst,
            SkrivBrevAvklaringsbehovLøsning(løsning.brevbestillingReferanse, løsning.handling, løsning.mottakere, løsning.begrunnelse)
        )
    }

    override fun forBehov(): Definisjon {
        return Definisjon.SKRIV_VEDTAKSBREV_KLAGE
    }
}