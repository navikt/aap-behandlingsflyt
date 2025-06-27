package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.klagebehandling.nay.KlagebehandlingNayRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.VurderKlageNayLøsning
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.behandlingsflyt.utils.Validation
import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException

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

        return when(val validatedLøsning = valider(løsning)) {
            is Validation.Invalid -> throw UgyldigForespørselException(validatedLøsning.errorMessage)
            is Validation.Valid -> LøsningsResultat(
                begrunnelse = løsning.klagevurderingNay.begrunnelse
            )
        }
    }

    override fun forBehov(): Definisjon {
        return Definisjon.VURDER_KLAGE_NAY
    }

    private fun valider(løsning: VurderKlageNayLøsning): Validation<VurderKlageNayLøsning> {
        val ugyldigeHjemler = løsning.klagevurderingNay.vilkårSomOmgjøres.filter {
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
