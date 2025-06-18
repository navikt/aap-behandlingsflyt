package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.HåndterSvarFraAndreinstansLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.svarfraandreinstans.SvarFraAndreinstansRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.lookup.repository.RepositoryProvider

class HåndterSvarFraAndreinstansLøser(
    private val svarFraAndreinstansRepository: SvarFraAndreinstansRepository,
) : AvklaringsbehovsLøser<HåndterSvarFraAndreinstansLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        svarFraAndreinstansRepository = repositoryProvider.provide(),
    )

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: HåndterSvarFraAndreinstansLøsning): LøsningsResultat {
        svarFraAndreinstansRepository.lagre(
            kontekst.kontekst.behandlingId,
            svarFraAndreinstansVurdering = løsning.svarFraAndreinstansVurdering.tilVurdering(kontekst.bruker)
        )
        return LøsningsResultat(
            begrunnelse = løsning.svarFraAndreinstansVurdering.begrunnelse
        )
    }

    override fun forBehov(): Definisjon {
        return Definisjon.HÅNDTER_SVAR_FRA_ANDREINSTANS
    }
}