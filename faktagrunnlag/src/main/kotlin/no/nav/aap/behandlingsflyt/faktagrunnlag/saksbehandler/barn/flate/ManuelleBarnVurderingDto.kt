package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.flate

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.BarnVurdering
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.BarnVurderingPeriode
import no.nav.aap.verdityper.Periode
import no.nav.aap.verdityper.sakogbehandling.Ident
import java.time.LocalDate

data class ManuelleBarnVurderingDto(
    val barn: List<ManueltBarnDto>
) {
    companion object {
        fun fromManuelleBarnVurdering(manuelleBarnVurdering: BarnVurdering?): ManuelleBarnVurderingDto {
            if (manuelleBarnVurdering == null) return ManuelleBarnVurderingDto(emptyList())
            return ManuelleBarnVurderingDto(manuelleBarnVurdering.barn.map {
                ManueltBarnDto.fromBarnVurderingPeriode(it)
            })
        }
    }
}

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
        perioder = forsørgeransvarPerioder.map { Periode(it.fraDato, it.tilDato ?: LocalDate.MAX) },
        begrunnelse = begrunnelse,
        skalBeregnesBarnetillegg = skalBeregnesBarnetillegg
    )

    companion object {
        fun fromBarnVurderingPeriode(barnVurdeirng: BarnVurderingPeriode) = ManueltBarnDto(
            ident = barnVurdeirng.ident.identifikator,
            begrunnelse = barnVurdeirng.begrunnelse,
            skalBeregnesBarnetillegg = barnVurdeirng.skalBeregnesBarnetillegg,
            forsørgeransvarPerioder = barnVurdeirng.perioder.map {
                ForsørgeransvarPeriode(
                    fraDato = it.fom,
                    tilDato = it.tom
                )
            }
        )
    }

}