package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning

import no.nav.aap.komponenter.type.Periode
import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.komponenter.verdityper.Prosent
import java.time.Year

/**
 * @param år Hvilket år inntekten gjelder for.
 * @param inntektIKroner Inntekt i kroner for dette året.
 * @param uføregrad Uføregrad i prosent. PS: denne brukes ikke i beregningen, her brukes uføregrad i [inntektsPerioder].
 * @param inntektJustertForUføregrad Inntekter oppjustert for uføregrad etter §11-28, fjerde ledd.
 */
data class UføreInntekt(
    val år: Year,
    val inntektIKroner: Beløp,
    val inntektIG: GUnit,
    @Deprecated("Bruk uføregrad i inntektsPerioder")
    val uføregrad: Prosent? = null,
    val inntektJustertForUføregrad: Beløp,
    val inntektIGJustertForUføregrad: GUnit,
    val grunnbeløp: Beløp,
    val inntektsPerioder: List<UføreInntektPeriodisert> = emptyList()
)

/**
 * @param uføregrad Uføregrad i en gitt periode.
 */
data class UføreInntektPeriodisert(
    val periode: Periode,
    val inntektIKroner: Beløp,
    val uføregrad: Prosent,
    val inntektJustertForUføregrad: Beløp
)