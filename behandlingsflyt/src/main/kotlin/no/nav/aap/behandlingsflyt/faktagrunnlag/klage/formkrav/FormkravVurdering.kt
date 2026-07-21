package no.nav.aap.behandlingsflyt.faktagrunnlag.klage.formkrav

import no.nav.aap.komponenter.verdityper.Bruker
import java.time.Instant

data class FormkravVurdering(
    val begrunnelse: String,
    val erBrukerPart: Boolean,
    val erFristOverholdt: Boolean,
    val likevelBehandles: Boolean?,
    val erKonkret: Boolean,
    val erSignert: Boolean,
    val vurdertAv: Bruker,
    val opprettet: Instant
) {
    fun erOppfylt() = erBrukerPart
            && erFristOverholdt()
            && erKonkret
            && erSignert

    fun erIkkeOppfylt() = !erOppfylt()
    
    fun erFristOverholdt() = erFristOverholdt || likevelBehandles == true

    fun erFristIkkeOverholdt() = !erFristOverholdt()
}