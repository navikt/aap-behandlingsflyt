package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barnetillegg.flate

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barnetillegg.ManuelleBarnVurdeirng
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barnetillegg.ManueltBarnVurdeirng
import no.nav.aap.verdityper.Periode
import no.nav.aap.verdityper.sakogbehandling.Ident
import java.time.LocalDate

data class ManuelleBarnVurderingDto(
    val barn: List<ManueltBarnVurderingDto>
) {
    companion object {
        fun toDto(manuelleManuelleBarnVurdeirng: ManuelleBarnVurdeirng): ManuelleBarnVurderingDto {
            return ManuelleBarnVurderingDto(manuelleManuelleBarnVurdeirng.barn.map {
                ManueltBarnVurderingDto.toDto(it)
            })
        }
    }
}

data class ForsørgeransvarPeriode(
    val fraDato: LocalDate,
    val tilDato: LocalDate?
)

data class ManueltBarnVurderingDto(
    val ident: String,
    val begrunnelse: String,
    val skalBeregnesBarnetillegg: Boolean,
    val forsørgeransvarPerioder: List<ForsørgeransvarPeriode>
) {
    fun tilBarnVurderingPeriode() = ManueltBarnVurdeirng(
        ident = Ident(ident),
        perioder = forsørgeransvarPerioder.map { Periode(it.fraDato, it.tilDato ?: LocalDate.MAX) },
        begrunnelse = begrunnelse,
        skalBeregnesBarnetillegg = skalBeregnesBarnetillegg
    )

    companion object {
        fun toDto(barnVurdeirng: ManueltBarnVurdeirng) = ManueltBarnVurderingDto(
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