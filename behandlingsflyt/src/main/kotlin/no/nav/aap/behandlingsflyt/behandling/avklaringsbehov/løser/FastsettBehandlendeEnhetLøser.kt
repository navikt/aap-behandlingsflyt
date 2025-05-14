package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.FastsettBehandlendeEnhetLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.behandlendeenhet.BehandlendeEnhetRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.lookup.repository.RepositoryProvider

class FastsettBehandlendeEnhetLøser(
    private val behandlendeEnhetRepository: BehandlendeEnhetRepository,
) : AvklaringsbehovsLøser<FastsettBehandlendeEnhetLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        behandlendeEnhetRepository = repositoryProvider.provide(),
    )

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: FastsettBehandlendeEnhetLøsning): LøsningsResultat {
        behandlendeEnhetRepository.lagre(
            kontekst.kontekst.behandlingId,
            behandlendeEnhetVurdering = løsning.behandlendeEnhetVurdering.tilVurdering(kontekst.bruker)
        )
        return LøsningsResultat(
            begrunnelse = "Fastsatt behandlende enhet"
        )
    }

    override fun forBehov(): Definisjon {
        return Definisjon.FASTSETT_BEHANDLENDE_ENHET
    }
}
