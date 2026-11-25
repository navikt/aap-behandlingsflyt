package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.VurderSøknadUtenlandsoppholdLøsning
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.lookup.repository.RepositoryProvider

class VurderSøknadUtenlandsoppholdLøser() : AvklaringsbehovsLøser<VurderSøknadUtenlandsoppholdLøsning> {
    constructor(repositoryProvider: RepositoryProvider) : this()

    // TODO: Lagre vurderingen når grunnlaget er på plass
    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: VurderSøknadUtenlandsoppholdLøsning
    ): LøsningsResultat {
        return LøsningsResultat("Vurdert søknad om opphold i utlandet")
    }

    override fun forBehov(): Definisjon = Definisjon.VURDER_SØKNAD_UTENLANDSOPPHOLD
}
