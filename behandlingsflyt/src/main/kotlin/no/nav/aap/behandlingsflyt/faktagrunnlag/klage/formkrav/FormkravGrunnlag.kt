package no.nav.aap.behandlingsflyt.faktagrunnlag.klage.formkrav

data class FormkravGrunnlag(
    val vurdering: FormkravVurdering,
    val varsel: FormkravVarsel? = null
)