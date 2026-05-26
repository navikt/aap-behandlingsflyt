package no.nav.aap.behandlingsflyt.behandling.tilkjentytelse

import no.nav.aap.behandlingsflyt.utils.diff.DiffDto
import no.nav.aap.komponenter.tidslinje.somTidslinje

data class TilkjentYtelse2Dto(val perioder: List<TilkjentYtelsePeriode2Dto>)

data class TilkjentYtelse2MedDiffDto(val perioder: List<DiffDto<TilkjentYtelsePeriode2Dto>>)
fun TilkjentYtelse2Dto.tilTidslinje() = this.perioder.somTidslinje { it.meldeperiode }