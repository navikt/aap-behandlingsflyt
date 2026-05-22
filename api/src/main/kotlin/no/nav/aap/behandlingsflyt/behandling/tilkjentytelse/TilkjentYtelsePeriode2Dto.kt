package no.nav.aap.behandlingsflyt.behandling.tilkjentytelse

import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate
import java.time.LocalDateTime

data class TilkjentYtelsePeriode2Dto(
    val meldeperiode: Periode,
    val levertMeldekortDato: LocalDate?,
    val sisteLeverteMeldekort: MeldekortDto?,
    val vurdertePerioder: List<VurdertPeriode>,
    val meldekortStatus: MeldekortStatus?,
)

enum class MeldekortStatus {
    IKKE_AVKLART,
    LEVERT_OK,
    LEVERT_FOR_SENT,
}


data class MeldekortDto(
    val timerArbeidPerPeriode: ArbeidIPeriodeDto,
    val mottattTidspunkt: LocalDateTime
)

data class ArbeidIPeriodeDto(val timerArbeid: Double)