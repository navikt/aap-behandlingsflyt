package no.nav.aap.behandlingsflyt.faktagrunnlag.klage.formkrav.flate

import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.formkrav.FormkravGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.formkrav.FormkravVurdering

data class FormkravGrunnlagDto(
    val vurdering: FormkravVurderingDto? = null
)

data class FormkravVurderingDto(
    val begrunnelse: String,
    val erBrukerPart: Boolean,
    val erFristOverholdt: Boolean,
    val likevelBehandles: Boolean?,
    val erKonkret: Boolean,
    val erSignert: Boolean,
    val vurdertAv: String
)

internal fun FormkravVurdering.tilDto() =
    FormkravVurderingDto(
        begrunnelse = begrunnelse,
        erBrukerPart = erBrukerPart,
        erFristOverholdt = erFristOverholdt,
        erKonkret = erKonkret,
        erSignert = erSignert,
        vurdertAv = vurdertAv,
        likevelBehandles = likevelBehandles
    )

internal fun FormkravGrunnlag.tilDto() =
    FormkravGrunnlagDto(
        vurdering = vurdering.tilDto()
    )
