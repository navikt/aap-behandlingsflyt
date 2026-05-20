package no.nav.aap.behandlingsflyt.behandling.tilkjentytelse

import no.nav.aap.behandlingsflyt.utils.diff.DiffDto

data class TilkjentYtelseDiffDto(val perioder: List<DiffDto<TilkjentYtelsePeriode2Dto>>)
