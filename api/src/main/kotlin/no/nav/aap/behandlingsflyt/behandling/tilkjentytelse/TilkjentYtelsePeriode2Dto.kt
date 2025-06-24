package no.nav.aap.behandlingsflyt.behandling.tilkjentytelse

import no.nav.aap.komponenter.type.Periode
import java.time.LocalDate

data class TilkjentYtelsePeriode2Dto(
    val meldeperiode: Periode,
    val levertMeldekortDato: LocalDate?,
    val meldekortStatus: MeldekortStaus,
    val vurdertePerioder: List<VurdertPeriode>
)

enum class MeldekortStaus {
    IKKE_LEVERT,
    LEVERT_ETTER_FRIST,
    OVERFØRT_TIL_ØKONOMI
}
