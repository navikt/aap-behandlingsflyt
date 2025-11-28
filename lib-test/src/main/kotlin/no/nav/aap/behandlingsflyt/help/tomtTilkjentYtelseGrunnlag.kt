package no.nav.aap.behandlingsflyt.help

import no.nav.aap.behandlingsflyt.behandling.tilkjentytelse.TilkjentYtelseGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.samordning.SamordningGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisGrunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import java.time.LocalDate

val tomtTilkjentYtelseGrunnlag = TilkjentYtelseGrunnlag(
    fødselsdato = Fødselsdato(LocalDate.of(2020, 1, 1)),
    beregningsgrunnlag = null,
    underveisgrunnlag = UnderveisGrunnlag(0, listOf()),
    barnetilleggGrunnlag = BarnetilleggGrunnlag(listOf()),
    samordningGrunnlag = SamordningGrunnlag(setOf()),
    samordningUføre = null,
    samordningArbeidsgiver = null,
)
