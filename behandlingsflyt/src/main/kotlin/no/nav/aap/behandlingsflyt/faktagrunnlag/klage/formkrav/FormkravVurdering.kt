package no.nav.aap.behandlingsflyt.faktagrunnlag.klage.formkrav

data class FormkravVurdering(
    val begrunnelse: String,
    val erBrukerPart: Boolean,
    val erFristOverholdt: Boolean,
    val likevelBehandles: Boolean?,
    val erKonkret: Boolean,
    val erSignert: Boolean,
    val vurdertAv: String
) {
    fun erOppfylt() = erBrukerPart
            && erFristOverholdt()
            && erKonkret
            && erSignert

    fun erIkkeOppfylt() = !erOppfylt()
    
    fun erFristOverholdt() = erFristOverholdt || likevelBehandles == true

    fun erFristIkkeOverholdt() = !erFristOverholdt()
}