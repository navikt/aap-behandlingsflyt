package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.nay.KlagebehandlingNayRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.VurderKlageNayLøsning
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.lookup.repository.RepositoryProvider

class VurderKlageNayLøser(
    private val klagebehandlingNayRepository: KlagebehandlingNayRepository,
) : AvklaringsbehovsLøser<VurderKlageNayLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        klagebehandlingNayRepository = repositoryProvider.provide(),
    )

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: VurderKlageNayLøsning): LøsningsResultat {
        klagebehandlingNayRepository.lagre(
            kontekst.kontekst.behandlingId,
            klagevurderingNay = løsning.klagevurderingNay.tilVurdering(kontekst.bruker)
        )
        return LøsningsResultat(
            begrunnelse = løsning.klagevurderingNay.begrunnelse
        )
    }

    override fun forBehov(): Definisjon {
        return Definisjon.VURDER_KLAGE_NAY
    }
}