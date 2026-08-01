package no.nav.aap.tilkjentytelse

import no.nav.aap.barnetillegg.BarnetilleggGrunnlag
import no.nav.aap.samordning.arbeidsgiver.SamordningArbeidsgiverGrunnlag
import no.nav.aap.underveis.UnderveisGrunnlag
import no.nav.aap.samordning.barnepensjon.BarnepensjonGrunnlag
import no.nav.aap.komponenter.tidslinje.Tidslinje
import no.nav.aap.komponenter.verdityper.GUnit
import no.nav.aap.misc.Faktagrunnlag
import no.nav.aap.personopplysninger.Fødselsdato
import no.nav.aap.samordning.SamordningGrunnlag
import no.nav.aap.samordning.SamordningUføreGrunnlag

class TilkjentYtelseGrunnlag(
    val fødselsdato: Fødselsdato,
    val beregningsgrunnlag: GUnit?,
    val underveisgrunnlag: UnderveisGrunnlag,
    val barnetilleggGrunnlag: BarnetilleggGrunnlag,
    val samordningGrunnlag: SamordningGrunnlag,
    val samordningUføre: SamordningUføreGrunnlag?,
    val samordningArbeidsgiver: SamordningArbeidsgiverGrunnlag?,
    val barnepensjonGrunnlag: BarnepensjonGrunnlag?,
    val minsteÅrligeYtelse: Tidslinje<GUnit> = MINSTE_ÅRLIG_YTELSE_TIDSLINJE,
) : Faktagrunnlag