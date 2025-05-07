package no.nav.aap.behandlingsflyt.faktagrunnlag.klage.formkrav

data class FormkravVurdering(
    val begrunnelse: String,
    val erBrukerPart: Boolean,
    val erFristOverholdt: Boolean,
    val erKonkret: Boolean,
    val erSignert: Boolean,
    val vurdertAv: String
)