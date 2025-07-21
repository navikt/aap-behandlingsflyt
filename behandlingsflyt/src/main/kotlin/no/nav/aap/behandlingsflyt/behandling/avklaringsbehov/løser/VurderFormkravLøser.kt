package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.VurderFormkravLøsning
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.formkrav.FormkravRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.formkrav.FormkravVurdering
import no.nav.aap.behandlingsflyt.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.utils.Validation
import no.nav.aap.komponenter.httpklient.auth.Bruker
import no.nav.aap.lookup.repository.RepositoryProvider
import java.time.LocalDateTime

class VurderFormkravLøser(
    private val formkravRepository: FormkravRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository
) : AvklaringsbehovsLøser<VurderFormkravLøsning> {

    constructor(repositoryProvider: RepositoryProvider) : this(
        formkravRepository = repositoryProvider.provide(),
        avklaringsbehovRepository = repositoryProvider.provide()
    )

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: VurderFormkravLøsning): LøsningsResultat {
        val vurdering = løsning.formkravVurdering.tilVurdering(kontekst.bruker, LocalDateTime.now())
        formkravRepository.lagre(
            kontekst.kontekst.behandlingId,
            formkravVurdering = vurdering
        )

        return when (val validatedLøsning = løsning.valider()) {
            is Validation.Invalid -> throw IllegalArgumentException(validatedLøsning.errorMessage)
            is Validation.Valid -> {
                settKlagePåVentHvisInnenforFristenOgBrevAlleredeSendt(kontekst.behandlingId(), vurdering, kontekst.bruker)
                LøsningsResultat(begrunnelse = løsning.formkravVurdering.begrunnelse)
            }
        }
    }

    private fun settKlagePåVentHvisInnenforFristenOgBrevAlleredeSendt(behandlingId: BehandlingId, vurdering: FormkravVurdering, bruker: Bruker) {
        val avklaringsbehov = avklaringsbehovRepository.hentAvklaringsbehovene(behandlingId)
        val ventebehov = avklaringsbehov.hentBehovForDefinisjon(Definisjon.VENTE_PÅ_FRIST_FORHÅNDSVARSEL_KLAGE_FORMKRAV)

        if (ventebehov != null && !ventebehov.fristUtløpt() && vurdering.erIkkeOppfylt()) {
            avklaringsbehov.reåpne(Definisjon.VENTE_PÅ_FRIST_FORHÅNDSVARSEL_KLAGE_FORMKRAV)
        }
    }

    override fun forBehov(): Definisjon {
        return Definisjon.VURDER_FORMKRAV
    }

    private fun VurderFormkravLøsning.valider(): Validation<VurderFormkravLøsning> {
        if (!formkravVurdering.erFristOverholdt && formkravVurdering.likevelBehandles == null) {
            return Validation.Invalid(this, "likevelBehandles må være satt dersom frist ikke er overholdt")
        }
        return Validation.Valid(this)
    }
}