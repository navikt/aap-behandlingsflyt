package no.nav.aap.behandlingsflyt.behandling.tilkjentytelse

import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent

class Tilkjent(
    val dagsats: Beløp,
    val gradering: Prosent,
    val grunnlag: Beløp,
    val grunnlagsfaktor: GUnit,
    val grunnbeløp: Beløp,
    val antallBarn: Int,
    val barnetilleggsats: Beløp,
    val barnetillegg: Beløp
    ) {


    fun redusertDagsats(): Beløp {
        return dagsats.multiplisert(gradering).pluss(barnetillegg.multiplisert(gradering))
    }

    override fun toString(): String {
        return "Tilkjent(dagsats=$dagsats, gradering=$gradering, redusertDagsats=${redusertDagsats()}), grunnlag=$grunnlag, grunnlagsfaktor=$grunnlagsfaktor, grunnbeløp=$grunnbeløp, antallBarn=$antallBarn, barnetilleggsats=$barnetilleggsats, barnetillegg=$barnetillegg)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Tilkjent

        if (dagsats != other.dagsats) return false
        if (gradering != other.gradering) return false
        if (grunnlag != other.grunnlag) return false
        if (grunnlagsfaktor != other.grunnlagsfaktor) return false
        if (grunnbeløp != other.grunnbeløp) return false
        if (antallBarn != other.antallBarn) return false
        if (barnetilleggsats != other.barnetilleggsats) return false
        if (barnetillegg != other.barnetillegg) return false

        return true
    }

    override fun hashCode(): Int {
        var result = dagsats.hashCode()
        result = 31 * result + gradering.hashCode()
        return result
    }
}

data class TilkjentGUnit(val dagsats: GUnit, val gradering: Prosent) {

    private fun redusertDagsats(): GUnit {
        return dagsats.multiplisert(gradering)
    }

    override fun toString(): String {
        return "Tilkjent(dagsats=$dagsats, gradering=$gradering, redusertDagsats=${redusertDagsats()})"
    }
}