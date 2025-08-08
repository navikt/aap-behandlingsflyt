package no.nav.aap.behandlingsflyt.behandling.tilkjentytelse

import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate
import java.time.LocalDateTime

data class TilkjentYtelsePeriode2Dto(
    val meldeperiode: Periode,
    val levertMeldekortDato: LocalDate?,
    val sisteLeverteMeldekort: MeldekortDto?,
    val meldekortStatus: MeldekortStaus?,
    val vurdertePerioder: List<VurdertPeriode>
)

data class MeldekortDto(
    val timerArbeidPerPeriode: ArbeidIPeriodeDto,
    val mottattTidspunkt: LocalDateTime
)

data class ArbeidIPeriodeDto(val timerArbeid: Double)


enum class MeldekortStaus {
    IKKE_LEVERT,
    LEVERT_ETTER_FRIST,
    OVERFØRT_TIL_ØKONOMI
}
