package no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning

import no.nav.aap.komponenter.verdityper.Beløp
import no.nav.aap.komponenter.verdityper.GUnit
import java.time.Year

/**
 * @param år Hvilket år denne inntekten gjelder for.
 * @param inntektIKroner Inntekt i kroner.
 * @param grunnbeløp Grunnbeløp i det gjeldende året.
 * @param inntektIG Inntekt i G _for det gjeldende året_.
 * @param inntekt6GBegrenset Inntekten, oppad begrenset til 6G.
 * @param er6GBegrenset Om inntekten ble 6G-begrenset.
 */
data class GrunnlagInntekt(
    val år: Year,
    val inntektIKroner: Beløp,
    val grunnbeløp: Beløp,
    val inntektIG: GUnit,
    val inntekt6GBegrenset: GUnit,
    val er6GBegrenset: Boolean
)
