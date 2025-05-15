package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.kontor.KlagebehandlingKontorRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.VurderKlageKontorLøsning
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.lookup.repository.RepositoryProvider

class VurderKlageKontorLøser(
    private val klagebehandlingKontorRepository: KlagebehandlingKontorRepository,
) : AvklaringsbehovsLøser<VurderKlageKontorLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        klagebehandlingKontorRepository = repositoryProvider.provide(),
    )

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: VurderKlageKontorLøsning): LøsningsResultat {
        klagebehandlingKontorRepository.lagre(
            kontekst.kontekst.behandlingId,
            klagevurderingKontor = løsning.klagevurderingKontor.tilVurdering(kontekst.bruker)
        )
        return LøsningsResultat(
            begrunnelse = løsning.klagevurderingKontor.begrunnelse
        )
    }

    override fun forBehov(): Definisjon {
        return Definisjon.VURDER_KLAGE_KONTOR
    }
}