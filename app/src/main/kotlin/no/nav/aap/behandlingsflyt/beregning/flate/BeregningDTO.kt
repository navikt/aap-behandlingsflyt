package no.nav.aap.behandlingsflyt.beregning.flate

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.beregning.Beregningsgrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Faktagrunnlag
import no.nav.aap.verdityper.GUnit

class BeregningDTO (
    val grunnlag: GUnit,
    val faktagrunnlag: Faktagrunnlag,
    val beregningsgrunnlag: Beregningsgrunnlag
)

/*
class BeregningDTO(
    val beregningsGrunnlag: GUnit,
    val faktagrunnlag: Faktagrunnlag,
    val nedsattArbeidsevneÅr,
    val inntekterFraForegåendeÅr,
    val inntektIKroner,
    val inntektIG,
    val er6GBegrenset,
    val erDetBruktGjennomsnitt,

    val antattÅrligInntektYrkesskadetidspunkt,
    val yrkesskadetidspunkt,
    val er6GBegrenset,//????
    val TerskelverdiForYrkesskadefordel,
    val AndelYrkesskade,
    val BenyttetAndelYrkesskade,
    val InntektPåYrkesskadetidspunkt,
    val YrkesskadeinntektIG,
    val grunnlagForBeregningAvYrkesskadeandel,
    val andelSomSkyldesYrkesskade,
    val andelSomIkkeSkyldesYrkesskade,
    val grunnlagEtterYrkesskadefordel,

    val uføreYtterligereNedsattArbeidsevneÅr,
    val uføreInntekterFraForegåendeÅr,
    val uføregrad,//Liste? Avhengig av om vi skal se på grad over tid
    val uføreOppjusterteInntekter,
    val uføreInntektIKroner,
    val uføreInntektIG,
    val uføreEr6GBegrenset,
    val uføreErDetBruktGjennomsnitt,
    )
 */