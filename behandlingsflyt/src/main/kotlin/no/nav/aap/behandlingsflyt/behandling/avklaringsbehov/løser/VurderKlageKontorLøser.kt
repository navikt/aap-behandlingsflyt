package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.kontor.KlagebehandlingKontorRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.VurderKlageKontorLøsning
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.behandlingsflyt.utils.Validation
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException

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

        return when(val validatedLøsning = valider(løsning)) {
            is Validation.Invalid -> throw UgyldigForespørselException(validatedLøsning.errorMessage)
            is Validation.Valid -> LøsningsResultat(
                begrunnelse = løsning.klagevurderingKontor.begrunnelse
            )
        }
    }

    override fun forBehov(): Definisjon {
        return Definisjon.VURDER_KLAGE_KONTOR
    }

    private fun valider(løsning: VurderKlageKontorLøsning): Validation<VurderKlageKontorLøsning> {
        val ugyldigeHjemler = løsning.klagevurderingKontor.vilkårSomOmgjøres.filter {
            try {
                it.tilÅrsak()
                false
            } catch (e: IllegalStateException) {
                true
            }
        }

        if (ugyldigeHjemler.isNotEmpty()) {
            return Validation.Invalid(løsning, "Løsningen inneholder omgjøring av hjemler som ikke ikke har implementert omgjøring via revurdering: ${ugyldigeHjemler.joinToString { it.name }}")
        }

        return Validation.Valid(løsning)
    }
}
