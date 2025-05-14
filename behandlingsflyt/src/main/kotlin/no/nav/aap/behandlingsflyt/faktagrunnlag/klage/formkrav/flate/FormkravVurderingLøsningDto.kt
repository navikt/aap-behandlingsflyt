package no.nav.aap.behandlingsflyt.faktagrunnlag.klage.formkrav.flate

import no.nav.aap.behandlingsflyt.faktagrunnlag.klage.formkrav.FormkravVurdering
import no.nav.aap.komponenter.httpklient.auth.Bruker

data class FormkravVurderingLøsningDto(
    val begrunnelse: String,
    val erBrukerPart: Boolean,
    val erFristOverholdt: Boolean,
    val erKonkret: Boolean,
    val erSignert: Boolean
) {
    fun tilVurdering(bruker: Bruker) = FormkravVurdering(
        begrunnelse = begrunnelse,
        erBrukerPart = erBrukerPart,
        erFristOverholdt = erFristOverholdt,
        erKonkret = erKonkret,
        erSignert = erSignert,
        vurdertAv = bruker.ident
    )
}