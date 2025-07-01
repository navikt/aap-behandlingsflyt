package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.flate

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.arbeidsevne.ArbeidsevneVurdering
import no.nav.aap.komponenter.verdityper.Prosent
import java.time.LocalDate

data class FastsettArbeidsevneDto(
    val begrunnelse: String,
    val arbeidsevne: Int,
    val fraDato: LocalDate
) {
    fun toArbeidsevnevurdering(vurdertAv: String) =
        ArbeidsevneVurdering(
            begrunnelse = begrunnelse,
            arbeidsevne = Prosent(arbeidsevne),
            fraDato = fraDato,
            vurdertAv = vurdertAv
        )
}