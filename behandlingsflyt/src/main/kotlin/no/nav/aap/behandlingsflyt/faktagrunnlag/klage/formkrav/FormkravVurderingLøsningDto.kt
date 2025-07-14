package no.nav.aap.behandlingsflyt.faktagrunnlag.klage.formkrav

import no.nav.aap.komponenter.httpklient.auth.Bruker
import java.time.LocalDateTime
import java.time.ZoneId

data class FormkravVurderingLøsningDto(
    val begrunnelse: String,
    val erBrukerPart: Boolean,
    val erFristOverholdt: Boolean,
    val likevelBehandles: Boolean?,
    val erKonkret: Boolean,
    val erSignert: Boolean
) {
    fun tilVurdering(bruker: Bruker, dato: LocalDateTime) = FormkravVurdering(
        begrunnelse = begrunnelse,
        erBrukerPart = erBrukerPart,
        erFristOverholdt = erFristOverholdt,
        erKonkret = erKonkret,
        erSignert = erSignert,
        vurdertAv = bruker.ident,
        opprettet = dato.atZone(ZoneId.of("Europe/Oslo")).toInstant(),
        likevelBehandles = likevelBehandles,
    )
}