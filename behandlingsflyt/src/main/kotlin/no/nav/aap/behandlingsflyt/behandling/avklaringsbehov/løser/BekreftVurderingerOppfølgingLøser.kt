package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.BekreftVurderingerOppfølgingLøsning
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.lookup.repository.RepositoryProvider

class BekreftVurderingerOppfølgingLøser() : AvklaringsbehovsLøser<BekreftVurderingerOppfølgingLøsning> {
    constructor(repositoryProvider: RepositoryProvider): this()

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: BekreftVurderingerOppfølgingLøsning): LøsningsResultat {
        // DO NOTHING 4 Now
        return LøsningsResultat("")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.BEKREFT_VURDERINGER_OPPFØLGING
    }
}
