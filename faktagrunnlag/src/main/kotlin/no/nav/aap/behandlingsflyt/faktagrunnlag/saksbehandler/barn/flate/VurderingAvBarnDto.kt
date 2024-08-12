package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.flate

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.BarnVurderingPeriode
import no.nav.aap.verdityper.Periode
import no.nav.aap.verdityper.sakogbehandling.Ident
import java.time.LocalDate

data class VurderingAvBarnDto(
    val barn: List<ManueltBarnDto>
)

data class ForsørgeransvarPeriode(
    val fraDato: LocalDate,
    val tilDato: LocalDate?
)

data class ManueltBarnDto(
    val ident: String,
    val begrunnelse: String,
    val skalBeregnesBarnetillegg: Boolean,
    val forsørgeransvarPerioder: List<ForsørgeransvarPeriode>
) {
    fun tilBarnVurderingPeriode() = BarnVurderingPeriode(
            ident = Ident(ident),
            perioder = forsørgeransvarPerioder.map { Periode(it.fraDato, it.tilDato ?: LocalDate.MAX)},
            begrunnelse = begrunnelse,
            skalBeregnesBarnetillegg = skalBeregnesBarnetillegg
        )

}