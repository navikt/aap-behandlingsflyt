package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSamordningBarnepensjonLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.årmåned.validerPerioder
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.samordning.barnepensjon.BarnepensjonRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.lookup.repository.RepositoryProvider

class AvklarSamordningBarnepensjonLøser(
    val barnepensjonRepository: BarnepensjonRepository
) : AvklaringsbehovsLøser<AvklarSamordningBarnepensjonLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        barnepensjonRepository = repositoryProvider.provide()
    )

    override fun løs(
        kontekst: AvklaringsbehovKontekst,
        løsning: AvklarSamordningBarnepensjonLøsning
    ): LøsningsResultat {
        validerPerioder(løsning.barnepensjonVurdering.perioder)

        barnepensjonRepository.lagre(
            kontekst.behandlingId(),
            løsning.barnepensjonVurdering.tilVurdering(kontekst.bruker, kontekst.behandlingId())
        )

        return LøsningsResultat(løsning.barnepensjonVurdering.begrunnelse)

    }

    override fun forBehov(): Definisjon {
        return Definisjon.SAMORDNING_BARNEPENSJON
    }
}