package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.VurderFormkravLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.formkrav.FormkravRepository
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.utils.Validation
import no.nav.aap.lookup.repository.RepositoryProvider

class VurderFormkravLøser(
    private val formkravRepository: FormkravRepository,
) : AvklaringsbehovsLøser<VurderFormkravLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        formkravRepository = repositoryProvider.provide(),
    )

override fun løs(kontekst: AvklaringsbehovKontekst, løsning: VurderFormkravLøsning): LøsningsResultat {
    formkravRepository.lagre(
        kontekst.kontekst.behandlingId,
        formkravVurdering = løsning.formkravVurdering.tilVurdering(kontekst.bruker)
    )

    return when(val validatedLøsning = løsning.valider()) {
        is Validation.Invalid -> throw IllegalArgumentException(validatedLøsning.errorMessage)
        is Validation.Valid -> LøsningsResultat(begrunnelse = løsning.formkravVurdering.begrunnelse)
    }
}

    override fun forBehov(): Definisjon {
        return Definisjon.VURDER_FORMKRAV
    }

    private fun VurderFormkravLøsning.valider(): Validation<VurderFormkravLøsning> {
        if(formkravVurdering.erFristOverholdt == false && formkravVurdering.likevelBehandles == null) {
            return Validation.Invalid(this, "likevelBehandles må være satt dersom frist ikke er overholdt")
        }
        return Validation.Valid(this)
    }
}