package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FastsettFullmektigLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.fullmektig.FullmektigRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.lookup.repository.RepositoryProvider

class FastsettFullmektigLøser(
    private val fullmektigRepository: FullmektigRepository,
) : AvklaringsbehovsLøser<FastsettFullmektigLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        fullmektigRepository = repositoryProvider.provide(),
    )

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: FastsettFullmektigLøsning): LøsningsResultat {
        fullmektigRepository.lagre(
            kontekst.kontekst.behandlingId,
            vurdering = løsning.fullmektigVurdering.tilVurdering(kontekst.bruker)
        )
        return LøsningsResultat(
            begrunnelse = "Fastatt fullmektig"
        )
    }

    override fun forBehov(): Definisjon {
        return Definisjon.FASTSETT_FULLMEKTIG
    }
}