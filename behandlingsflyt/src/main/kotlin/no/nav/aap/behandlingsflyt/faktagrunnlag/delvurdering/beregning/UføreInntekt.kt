package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning

import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import java.time.Year

/**
 * @param år Hvilket år inntekten gjelder for.
 * @param inntektIKroner Inntekt i kroner for dette året.
 * @param uføregrad Uføregrad i prosent.
 * @param arbeidsgrad Komplement av uføregrad.
 * @param inntektJustertForUføregrad Inntekter oppjustert for uføregrad etter §11-28, fjerde ledd.
 */
data class UføreInntekt(
    val år: Year,
    val inntektIKroner: Beløp,
    val inntektIG: GUnit,
    @Deprecated("Bruk uføregrad i inntektsPerioder")
    val uføregrad: Prosent? = null,
    @Deprecated("Bruk arbeidsgrad i inntektsPerioder")
    val arbeidsgrad: Prosent? = null,
    val inntektJustertForUføregrad: Beløp,
    val inntektIGJustertForUføregrad: GUnit,
    val grunnbeløp: Beløp,
    val inntektsPerioder: List<UføreInntektPeriodisert> = emptyList()
)

data class UføreInntektPeriodisert(
    val periode: Periode,
    val inntektIKroner: Beløp,
    val inntektIG: GUnit,
    val uføregrad: Prosent,
    val arbeidsgrad: Prosent,
    val inntektJustertForUføregrad: Beløp,
    val inntektIGJustertForUføregrad: GUnit,
    val grunnbeløp: Beløp
)