package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsopptrapping

import java.time.LocalDate
import kotlin.String

data class ArbeidsopptrappingDto(
    val begrunnelse: String,
    val fraDato: LocalDate,
    val reellMulighetTilOpptrapping: Boolean,
    val rettPaaAAPIOpptrapping: Boolean,
) {
    fun toArbeidsopptrappingVurdering(vurdertAv: String) =
        ArbeidsopptrappingVurdering(
            begrunnelse = begrunnelse,
            fraDato = fraDato,
            reellMulighetTilOpptrapping = reellMulighetTilOpptrapping,
            rettPaaAAPIOpptrapping = rettPaaAAPIOpptrapping,
            vurdertAv = vurdertAv,
            opprettetTid = null
        )
}